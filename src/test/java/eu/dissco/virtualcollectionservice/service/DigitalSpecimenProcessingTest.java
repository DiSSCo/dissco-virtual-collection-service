package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.CREATED;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimenEvent;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimenEventWithVC;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenVirtualCollection;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.virtualcollectionservice.component.SpecimenEvaluationComponent;
import eu.dissco.virtualcollectionservice.component.VirtualCollectionCacheComponent;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigitalSpecimenProcessingTest {

  private static MockedStatic<Instant> mockedInstant;
  private static MockedStatic<Clock> mockedClock;

  @Mock
  private VirtualCollectionCacheComponent cache;
  @Mock
  private RabbitMqPublisherService publisherService;
  @Mock
  private SpecimenEvaluationComponent evaluationComponent;

  private DigitalSpecimenProcessingService processingService;

  private static void tearDownClock() {
    mockedInstant.close();
    mockedClock.close();
  }

  private static void setUpInstantNow() {
    Clock clock = Clock.fixed(CREATED, ZoneOffset.UTC);
    Instant instant = Instant.now(clock);
    mockedInstant = mockStatic(Instant.class);
    mockedInstant.when(Instant::now).thenReturn(instant);
    mockedInstant.when(() -> Instant.from(any())).thenReturn(instant);
    mockedInstant.when(() -> Instant.parse(any())).thenReturn(instant);
    mockedClock = mockStatic(Clock.class);
    mockedClock.when(Clock::systemUTC).thenReturn(clock);
  }

  @BeforeEach
  void setUp() {
    processingService = new DigitalSpecimenProcessingService(MAPPER, new ApplicationProperties(),
        cache, publisherService, evaluationComponent);
  }

  @Test
  void handleIngestionEvents() throws JsonProcessingException {
    // Given
    var virtualCollection = givenVirtualCollection();
    var secondFilter = new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.EQUALS)
        .withOdsPredicateKey("$['ods:topicDiscipline']")
        .withOdsPredicateValue("Geology");
    var digitalSpecimen = givenDigitalSpecimenEvent();
    given(cache.getCache()).willReturn(Set.of(virtualCollection,
        givenVirtualCollection("https://hdl.handle.net/TEST/YYY-YYY-YYY", "Another VC", secondFilter
        )));
    // Mock needs to be behind the cache
    setUpInstantNow();
    given(
        evaluationComponent.evaluateSpecimen(digitalSpecimen.digitalSpecimenWrapper().attributes(),
            virtualCollection.getOdsHasTargetDigitalObjectFilter())).willReturn(true);
    given(
        evaluationComponent.evaluateSpecimen(digitalSpecimen.digitalSpecimenWrapper().attributes(),
            secondFilter)).willReturn(false);

    // When
    processingService.handleIngestionEvents(List.of(digitalSpecimen));

    // Then
    then(publisherService).should().publishDigitalSpecimen(givenDigitalSpecimenEventWithVC(true));
    tearDownClock();
  }

  @Test
  void handleIngestionEventsNoMatch() throws JsonProcessingException {
    // Given
    var virtualCollection = givenVirtualCollection();
    var digitalSpecimen = givenDigitalSpecimenEvent();
    given(cache.getCache()).willReturn(Set.of(virtualCollection));
    given(
        evaluationComponent.evaluateSpecimen(digitalSpecimen.digitalSpecimenWrapper().attributes(),
            virtualCollection.getOdsHasTargetDigitalObjectFilter())).willReturn(false);

    // When
    processingService.handleIngestionEvents(List.of(digitalSpecimen));

    // Then
    then(publisherService).should().publishDigitalSpecimen(digitalSpecimen);
  }

  @Test
  void handleIngestionEventsInvalidVC() throws JsonProcessingException {
    // Given
    var virtualCollection = givenVirtualCollection();
    var digitalSpecimen = givenDigitalSpecimenEvent();
    given(cache.getCache()).willReturn(Set.of(virtualCollection));
    given(
        evaluationComponent.evaluateSpecimen(digitalSpecimen.digitalSpecimenWrapper().attributes(),
            virtualCollection.getOdsHasTargetDigitalObjectFilter())).willThrow(
        new JsonProcessingException("Invalid Json") {
        });

    // When
    assertThrows(JsonProcessingException.class,
        () -> processingService.handleIngestionEvents(List.of(digitalSpecimen)));

    // Then
    then(publisherService).shouldHaveNoInteractions();
  }

}
