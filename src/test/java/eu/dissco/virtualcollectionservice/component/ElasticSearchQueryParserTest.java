package eu.dissco.virtualcollectionservice.component;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.SPECIMEN_ID;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenElasticQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import eu.dissco.virtualcollectionservice.schema.OdsHasPredicate;
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
        Arguments.of(new TargetDigitalObjectFilter()
                .withOdsPredicateType(OdsPredicateType.NOT)
                .withOdsPredicateKey("schema:version")
                .withOdsPredicateValue(1),
            new Query.Builder().bool(b -> b.mustNot(new Query.Builder().term(
                t -> t.field("schema:version").value(FieldValue.of(1))
                    .caseInsensitive(true)).build())).build()),
        Arguments.of(new TargetDigitalObjectFilter()
                .withOdsPredicateType(OdsPredicateType.IN)
                .withOdsPredicateKey("ods:topicDiscipline")
                .withOdsPredicateValues(List.of("botany", "zoology")),
            new Query.Builder().terms(
                    t -> t.field("ods:topicDiscipline.keyword")
                        .terms(
                            ts -> ts.value(List.of(FieldValue.of("botany"), FieldValue.of("zoology")))))
                .build()),
        Arguments.of(new TargetDigitalObjectFilter()
                .withOdsPredicateType(OdsPredicateType.EQUALS)
                .withOdsPredicateKey("ods:topicDiscipline")
                .withOdsPredicateValues(List.of("botany", "zoology")),
            new Query.Builder().terms(
                    t -> t.field("ods:topicDiscipline.keyword")
                        .terms(
                            ts -> ts.value(List.of(FieldValue.of("botany"), FieldValue.of("zoology")))))
                .build()),
        Arguments.of(new TargetDigitalObjectFilter()
                .withOdsPredicateType(OdsPredicateType.AND)
                .withOdsHasPredicates(List.of(
                    new OdsHasPredicate()
                        .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                        .withOdsPredicateKey("ods:topicDiscipline")
                        .withOdsPredicateValue("botany"),
                    new OdsHasPredicate()
                        .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                        .withOdsPredicateKey("schema:version")
                        .withOdsPredicateValue(2)
                )),
            new Query.Builder().bool(b -> b.must(List.of(new Query.Builder().bool(
                    b2 -> b2.must(m -> m.term(t -> t.field("ods:topicDiscipline.keyword")
                        .value(FieldValue.of("botany"))
                        .caseInsensitive(true)))).build(),
                new Query.Builder().bool(b2 -> b2.must(m -> m.term(
                    t -> t.field("schema:version")
                        .value(FieldValue.of(2))
                        .caseInsensitive(true)))).build()))).build()),
        Arguments.of(new TargetDigitalObjectFilter()
                .withOdsPredicateType(OdsPredicateType.OR)
                .withOdsHasPredicates(List.of(
                    new OdsHasPredicate()
                        .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                        .withOdsPredicateKey("ods:topicDiscipline")
                        .withOdsPredicateValue("botany"),
                    new OdsHasPredicate()
                        .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                        .withOdsPredicateKey("schema:version")
                        .withOdsPredicateValue(2)
                )),
            new Query.Builder().bool(b -> b.should(List.of(new Query.Builder().bool(
                    b2 -> b2.must(m -> m.term(t -> t.field("ods:topicDiscipline.keyword")
                        .value(FieldValue.of("botany"))
                        .caseInsensitive(true)))).build(),
                new Query.Builder().bool(b2 -> b2.must(m -> m.term(
                    t -> t.field("schema:version")
                        .value(FieldValue.of(2))
                        .caseInsensitive(true)))).build())).minimumShouldMatch("1")).build()));
  }

  static Stream<Arguments> parseIllegalQueryProvider() {
    return Stream.of(
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.AND)
            .withOdsPredicateKey("@id")
            .withOdsPredicateValue("http://example.com/specimen/12345")),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.NOT)
            .withOdsPredicateKey("ods:topicDiscipline")
            .withOdsPredicateValues(List.of("botany", "zoology"))),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.IN)
            .withOdsPredicateKey("ods:topicDiscipline")
            .withOdsPredicateValues(List.of("botany"))),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.IN)
            .withOdsPredicateKey("ods:topicDiscipline")
            .withOdsPredicateValues(null)),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.EQUALS)
            .withOdsHasPredicates(List.of(
                new OdsHasPredicate()
                    .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                    .withOdsPredicateKey("ods:topicDiscipline")
                    .withOdsPredicateValues(List.of("botany", "zoology"))
            ))
        )
    );
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
  @MethodSource("parseIllegalQueryProvider")
  void testInvalidParseQuery(TargetDigitalObjectFilter objectFilter) {
    // Given

    // When
    assertThrows(IllegalArgumentException.class,
        () -> ElasticSearchQueryParser.parseTargetFilterToQuery(objectFilter));
  }
}
