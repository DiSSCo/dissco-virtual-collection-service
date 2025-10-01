package eu.dissco.virtualcollectionservice.component;

import static eu.dissco.virtualcollectionservice.utils.FilterParseUtils.harmonizeValues;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpecimenEvaluationComponent {

  private final ObjectMapper objectMapper;

  public boolean evaluateSpecimen(DigitalSpecimen specimen,
      TargetDigitalObjectFilter filter) throws JsonProcessingException {
    if (filter.getOdsHasPredicates() == null || filter.getOdsHasPredicates().isEmpty()) {
      return handleSinglePredicate(specimen, filter);
    } else if (filter.getOdsHasPredicates() != null && !filter.getOdsHasPredicates().isEmpty()) {
      return handleMultiplePredicates(specimen, filter);
    } else {
      throw new IllegalArgumentException(
          "The filter must contain either a single predicate or a list of predicates");
    }
  }

  private boolean handleMultiplePredicates(DigitalSpecimen specimen,
      TargetDigitalObjectFilter filter)
      throws JsonProcessingException {
    log.debug("Assuming this is a list of predicates: {}", filter);
    var document = JsonPath.parse(objectMapper.writeValueAsString(specimen));
    for (var predicate : filter.getOdsHasPredicates()) {
      var result = checkPredicate(document, predicate.getOdsPredicateType().value(),
          predicate.getOdsPredicateKey(),
          harmonizeValues(predicate.getOdsPredicateValue(), predicate.getOdsPredicateValues()));
      if (result && filter.getOdsPredicateType().equals(OdsPredicateType.OR)) {
        return true;
      }
      if (!result && filter.getOdsPredicateType().equals(OdsPredicateType.AND)) {
        return false;
      }
    }
    if (filter.getOdsPredicateType().equals(OdsPredicateType.AND)) {
      return true;
    } else if (filter.getOdsPredicateType().equals(OdsPredicateType.OR)) {
      return false;
    } else {
      throw new IllegalArgumentException(
          "When using a list of predicates, the predicate type must be either AND or OR");
    }
  }

  private boolean handleSinglePredicate(DigitalSpecimen specimen, TargetDigitalObjectFilter filter)
      throws JsonProcessingException {
    log.debug("Assuming this is an individual predicate: {}", filter);
    var harmonizedValues = harmonizeValues(filter.getOdsPredicateValue(),
        filter.getOdsPredicateValues());
    var document = JsonPath.parse(objectMapper.writeValueAsString(specimen));
    return checkPredicate(document, filter.getOdsPredicateType().value(),
        filter.getOdsPredicateKey(), harmonizedValues);
  }

  private boolean checkPredicate(DocumentContext document, String predicateType,
      String predicateKey, List<Object> predicateValues) {
    if (predicateType.equals(OdsPredicateType.EQUALS.value()) && predicateValues.size() == 1) {
      var result = document.read(predicateKey);
      return predicateValues.getFirst().equals(result);
    } else if (predicateType.equals(OdsPredicateType.NOT.value()) && predicateValues.size() == 1) {
      var result = document.read(predicateKey);
      return !predicateValues.getFirst().equals(result);
    } else if (
        (predicateType.equals(OdsPredicateType.IN.value()) || predicateType
            .equals(OdsPredicateType.EQUALS.value())) && predicateValues.size() > 1) {
      var result = document.read(predicateKey);
      return predicateValues.contains(result);
    } else {
      throw new IllegalArgumentException(
          "The predicate type is not supported for local evaluation: " + predicateType);
    }
  }
}
