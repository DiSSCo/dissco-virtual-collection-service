package eu.dissco.virtualcollectionservice.component;

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
    log.debug("Assuming this is an individual predicate");
    var predicateType = filter.getOdsPredicateType();
    var harmonizedValues = harmonizeValues(filter.getOdsPredicateValue(),
        filter.getOdsPredicateValues());
    return getQueryFromPredicate(predicateType.value(),
        filter.getOdsPredicateKey(), harmonizedValues);
  }

  private static Query handleMultiplePredicates(TargetDigitalObjectFilter filter) {
    log.debug("Assuming this is a list of predicates");
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

  private static List<Object> harmonizeValues(Object odsPredicateValue,
      List<Object> odsPredicateValues) {
    if (odsPredicateValue != null) {
      return List.of(odsPredicateValue);
    } else if (odsPredicateValues != null && !odsPredicateValues.isEmpty()) {
      return odsPredicateValues;
    } else {
      throw new IllegalArgumentException(
          "Both predicate value and predicate values are null or empty");
    }
  }

  private static Query getQueryFromPredicate(String predicateType, String predicateKey,
      List<Object> predicateValue) {
    var sanitizedKey = sanitizeKey(predicateKey, predicateValue);
    if (predicateType.equals(OdsPredicateType.NOT.value()) && predicateValue.size() == 1) {
      var termQuery = getTermQuery(predicateValue, sanitizedKey);
      return new Query.Builder().bool(b -> b.mustNot(termQuery)).build();
    } else if (predicateType.equals(OdsPredicateType.EQUALS.value())
        && predicateValue.size() == 1) {
      var termQuery = getTermQuery(predicateValue, sanitizedKey);
      return new Query.Builder().bool(b -> b.must(termQuery)).build();
    } else if (
        (predicateType.equals(OdsPredicateType.IN.value()) || predicateType.equals(
            OdsPredicateType.EQUALS.value())) && predicateValue.size() > 1) {
      return new Query.Builder().terms(t -> t.field(sanitizedKey)
              .terms(ts -> ts.value(
                  predicateValue.stream().map(value -> FieldValue.of((String) value)).toList())))
          .build();
    } else {
      throw new IllegalArgumentException("Invalid predicateType for this level: " + predicateType);
    }
  }

  // It makes an assumption that all values in a list are of the same type
  private static String sanitizeKey(String predicateKey, List<Object> predicateValue) {
    var sanitizedKey = predicateKey.replace("'", "")
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
