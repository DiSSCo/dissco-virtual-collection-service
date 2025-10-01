package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimenEvent;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenVirtualCollection;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.virtualcollectionservice.component.SpecimenEvaluationComponent;
import eu.dissco.virtualcollectionservice.component.VirtualCollectionCacheComponent;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DigitalSpecimenProcessingTest {

  @Mock
  private VirtualCollectionCacheComponent cache;
  @Mock
  private RabbitMqPublisherService publisherService;
  @Mock
  private SpecimenEvaluationComponent evaluationComponent;

  private DigitalSpecimenProcessingService processingService;

  @BeforeEach
  void setUp() {
    processingService = new DigitalSpecimenProcessingService(MAPPER, new ApplicationProperties(),
        cache, publisherService, evaluationComponent);
  }

  @Test
  void handleIngestionEvents() throws JsonProcessingException {
    // Given
    var virtualCollection = givenVirtualCollection();
    var digitalSpecimen = givenDigitalSpecimenEvent();
    given(cache.getCache()).willReturn(Set.of(virtualCollection));
    given(
        evaluationComponent.evaluateSpecimen(digitalSpecimen.digitalSpecimenWrapper().attributes(),
            virtualCollection.getOdsHasTargetDigitalObjectFilter())).willReturn(true);

    // When
    processingService.handleIngestionEvents(List.of(digitalSpecimen));

    // Then
    then(publisherService).should().publishDigitalSpecimen(any());
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
    then(publisherService).shouldHaveNoInteractions();
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
