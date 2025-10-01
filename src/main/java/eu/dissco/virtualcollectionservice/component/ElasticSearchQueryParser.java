package eu.dissco.virtualcollectionservice.component;

import static eu.dissco.virtualcollectionservice.utils.FilterParseUtils.harmonizeValues;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElasticSearchQueryParser {

  private ElasticSearchQueryParser() {
    // This is a utility class
  }

  private static Query getTermQuery(List<Object> predicateValue, String sanitizedKey) {
    return new Query.Builder().term(
        t -> t.field(sanitizedKey).value(FieldValue.of(predicateValue.getFirst()))
            .caseInsensitive(true)).build();
  }

  public static Query parseTargetFilterToQuery(TargetDigitalObjectFilter filter) {
    if (filter.getOdsHasPredicates() == null || filter.getOdsHasPredicates().isEmpty()) {
      return handleSinglePredicate(filter);
    } else if (filter.getOdsHasPredicates() != null && !filter.getOdsHasPredicates().isEmpty()) {
      return handleMultiplePredicates(filter);
    } else {
      throw new IllegalArgumentException(
          "The filter must contain either a single predicate or a list of predicates");
    }
  }

  private static Query handleSinglePredicate(TargetDigitalObjectFilter filter) {
    log.debug("Assuming this is an individual predicate: {}", filter);
    var predicateType = filter.getOdsPredicateType();
    var harmonizedValues = harmonizeValues(filter.getOdsPredicateValue(),
        filter.getOdsPredicateValues());
    return getQueryFromPredicate(predicateType.value(),
        filter.getOdsPredicateKey(), harmonizedValues);
  }

  private static Query handleMultiplePredicates(TargetDigitalObjectFilter filter) {
    log.debug("Assuming this is a list of predicates: {}", filter);
    var parsedQueries = new ArrayList<Query>();
    filter.getOdsHasPredicates().stream().map(
            predicate -> getQueryFromPredicate(
                predicate.getOdsPredicateType().value(), predicate.getOdsPredicateKey(),
                harmonizeValues(predicate.getOdsPredicateValue(), predicate.getOdsPredicateValues())))
        .forEach(parsedQueries::add);
    if (filter.getOdsPredicateType().equals(OdsPredicateType.AND)) {
      return new Query.Builder().bool(b -> b.must(parsedQueries)).build();
    } else if (filter.getOdsPredicateType().equals(OdsPredicateType.OR)) {
      return new Query.Builder().bool(b -> b.should(parsedQueries).minimumShouldMatch("1")).build();
    } else {
      throw new IllegalArgumentException(
          "When using a list of predicates, the predicate type must be either AND or OR");
    }
  }

  private static Query getQueryFromPredicate(String predicateType, String predicateKey,
      List<Object> predicateValues) {
    var sanitizedKey = sanitizeKey(predicateKey, predicateValues);
    if (predicateType.equals(OdsPredicateType.NOT.value()) && predicateValues.size() == 1) {
      var termQuery = getTermQuery(predicateValues, sanitizedKey);
      return new Query.Builder().bool(b -> b.mustNot(termQuery)).build();
    } else if (predicateType.equals(OdsPredicateType.EQUALS.value())
        && predicateValues.size() == 1) {
      var termQuery = getTermQuery(predicateValues, sanitizedKey);
      return new Query.Builder().bool(b -> b.must(termQuery)).build();
    } else if (
        (predicateType.equals(OdsPredicateType.IN.value()) || predicateType.equals(
            OdsPredicateType.EQUALS.value())) && predicateValues.size() > 1) {
      return new Query.Builder().terms(t -> t.field(sanitizedKey)
              .terms(ts -> ts.value(
                  predicateValues.stream().map(value -> FieldValue.of((String) value)).toList())))
          .build();
    } else {
      throw new IllegalArgumentException("Invalid predicateType for this level: " + predicateType);
    }
  }

  /**
   * Assumption: All values in the predicateValue list are of the same type.
   * The method checks the type of the first element to determine whether to append
   *  .keyword to the sanitized key (for String values). If the list contains
   * mixed types, this may result in incorrect query construction or runtime errors.
   * Callers must ensure that predicateValue contains only
   * elements of a single type, and that the first element is representative of the entire list.
   */
  private static String sanitizeKey(String predicateKey, List<Object> predicateValue) {
    var sanitizedKey = predicateKey
        .replace("'", "")
        .replace("[*]", "")
        .replace("$", "")
        .replace("[", "")
        .replace("]", "")
        .replace("\"", "");
    if (predicateValue.getFirst() instanceof String) {
      return sanitizedKey + ".keyword";
    } else {
      return sanitizedKey;
    }
  }
}
