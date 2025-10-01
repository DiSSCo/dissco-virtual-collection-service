package eu.dissco.virtualcollectionservice.component;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenAndFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimen;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenEqualsListFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenInFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenNotFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenOrFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenTargetDigitalObjectFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.virtualcollectionservice.schema.OdsHasPredicate;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SpecimenEvaluationComponentTest {

  private SpecimenEvaluationComponent component;

  public static Stream<Arguments> evaluateSpecimenProvider() {
    return Stream.of(
        Arguments.of(givenTargetDigitalObjectFilter(), true),
        Arguments.of(givenEqualsListFilter(), true),
        Arguments.of(givenNotFilter(), true),
        Arguments.of(givenInFilter(), true),
        Arguments.of(givenOrFilter(), true),
        Arguments.of(givenAndFilter(), true),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.AND)
            .withOdsHasPredicates(List.of(
                new OdsHasPredicate()
                    .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                    .withOdsPredicateKey("$['ods:isKnownToContainMedia']")
                    .withOdsPredicateValue(true)
            )), false),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.OR)
            .withOdsHasPredicates(List.of(
                new OdsHasPredicate()
                    .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                    .withOdsPredicateKey("$['ods:hasEvents'][0]['ods:hasLocation']['dwc:country']")
                    .withOdsPredicateValue("Netherlands")
            )), false));
  }

  @BeforeEach
  void setUp() {
    component = new SpecimenEvaluationComponent(MAPPER);
  }

  @ParameterizedTest
  @MethodSource("evaluateSpecimenProvider")
  void testEvaluateSpecimen(TargetDigitalObjectFilter filter, boolean expected)
      throws JsonProcessingException {
    // Given
    var specimen = givenDigitalSpecimen();

    // When
    var result = component.evaluateSpecimen(specimen, filter);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("eu.dissco.virtualcollectionservice.utils.TestUtils#illegalFilters")
  void testInvalidParseQuery(TargetDigitalObjectFilter objectFilter)
      throws JsonProcessingException {
    // Given
    var specimen = givenDigitalSpecimen();

    // When
    assertThrows(IllegalArgumentException.class,
        () -> component.evaluateSpecimen(specimen, objectFilter));
  }


}
