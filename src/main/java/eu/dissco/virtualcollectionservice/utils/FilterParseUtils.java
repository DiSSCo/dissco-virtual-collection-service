package eu.dissco.virtualcollectionservice.utils;

import java.util.List;

public class FilterParseUtils {

  private FilterParseUtils() {
    // This is a utility class
  }

  public static List<Object> harmonizeValues(Object odsPredicateValue,
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
}
