package eu.dissco.virtualcollectionservice.component;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.SPECIMEN_ID;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenAndFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenElasticQuery;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenEqualsListFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenInFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenNotFilter;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenOrFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchQueryParserTest {

  static Stream<Arguments> parseQueryProvider() {
    return Stream.of(
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.EQUALS)
            .withOdsPredicateKey("@id")
            .withOdsPredicateValue(SPECIMEN_ID), givenElasticQuery()),
        Arguments.of(givenNotFilter(),
            new Query.Builder().bool(b -> b.mustNot(new Query.Builder().term(
                t -> t.field("ods:physicalSpecimenIDType.keyword").value(FieldValue.of("Local"))
                    .caseInsensitive(true)).build())).build()),
        Arguments.of(givenInFilter(),
            new Query.Builder().terms(
                    t -> t.field("ods:topicDiscipline.keyword")
                        .terms(
                            ts -> ts.value(List.of(FieldValue.of("Botany"), FieldValue.of("Zoology")))))
                .build()),
        Arguments.of(givenEqualsListFilter(),
            new Query.Builder().terms(
                    t -> t.field("ods:topicDiscipline.keyword")
                        .terms(
                            ts -> ts.value(List.of(FieldValue.of("Botany"), FieldValue.of("Zoology")))))
                .build()),
        Arguments.of(givenAndFilter(),
            new Query.Builder().bool(b -> b.must(List.of(new Query.Builder().bool(
                    b2 -> b2.must(m -> m.term(t -> t.field("ods:isKnownToContainMedia")
                        .value(FieldValue.of(false))
                        .caseInsensitive(true)))).build(),
                new Query.Builder().bool(b2 -> b2.must(m -> m.term(
                    t -> t.field("dwc:preparations.keyword")
                        .value(FieldValue.of("herbarium sheet"))
                        .caseInsensitive(true)))).build()))).build()),
        Arguments.of(givenOrFilter(),
            new Query.Builder().bool(b -> b.should(List.of(new Query.Builder().bool(
                    b2 -> b2.must(m -> m.term(t -> t.field("ods:topicDiscipline.keyword")
                        .value(FieldValue.of("Botany"))
                        .caseInsensitive(true)))).build(),
                new Query.Builder().bool(b2 -> b2.must(m -> m.term(
                    t -> t.field("ods:version")
                        .value(FieldValue.of(2))
                        .caseInsensitive(true)))).build())).minimumShouldMatch("1")).build()));
  }

  @ParameterizedTest
  @MethodSource("parseQueryProvider")
  void testParseQuery(TargetDigitalObjectFilter objectFilter, Query expected) {
    // Given

    // When
    var result = ElasticSearchQueryParser.parseTargetFilterToQuery(objectFilter);

    // Then
    assertThat(result.toString()).hasToString(expected.toString());
  }

  @ParameterizedTest
  @MethodSource("eu.dissco.virtualcollectionservice.utils.TestUtils#illegalFilters")
  void testInvalidParseQuery(TargetDigitalObjectFilter objectFilter) {
    // Given

    // When
    assertThrows(IllegalArgumentException.class,
        () -> ElasticSearchQueryParser.parseTargetFilterToQuery(objectFilter));
  }
}
