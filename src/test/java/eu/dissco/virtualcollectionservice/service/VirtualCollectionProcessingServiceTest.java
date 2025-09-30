package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.CREATED;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimenEventWithVC;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimenWithVC;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenSpecimenNode;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenVirtualCollectionEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import eu.dissco.virtualcollectionservice.repository.ElasticSearchRepository;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtualCollectionProcessingServiceTest {

  private static MockedStatic<Instant> mockedInstant;
  private static MockedStatic<Clock> mockedClock;

  @Mock
  private ElasticSearchRepository repository;
  @Mock
  private RabbitMqPublisherService publisherService;

  private VirtualCollectionProcessingService service;

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
  void setup() {
    service = new VirtualCollectionProcessingService(MAPPER, repository, publisherService,
        new ApplicationProperties());
  }

  @Test
  void testHandleMessage() throws IOException {
    // Given
    setUpInstantNow();
    var event = givenVirtualCollectionEvent();
    given(repository.retrieveObjects(any(), eq("digital-specimen"), any(Query.class))).willReturn(
        List.of(givenSpecimenNode())).willReturn(List.of());

    // When
    service.handleMessage(event);

    // Then
    then(publisherService).should().publishDigitalSpecimen(givenDigitalSpecimenEventWithVC());
    tearDownClock();
  }

  @Test
  void testHandleMessageVCAlreadyPresent() throws IOException {
    // Given
    var event = givenVirtualCollectionEvent();
    given(repository.retrieveObjects(any(), eq("digital-specimen"), any(Query.class))).willReturn(
        List.of(MAPPER.valueToTree(givenDigitalSpecimenWithVC()))).willReturn(List.of());

    // When
    service.handleMessage(event);

    // Then
    then(publisherService).should().publishDigitalSpecimen(givenDigitalSpecimenEventWithVC());
  }
}
