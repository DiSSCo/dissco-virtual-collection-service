package eu.dissco.virtualcollectionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.component.SpecimenEvaluationComponent;
import eu.dissco.virtualcollectionservice.component.VirtualCollectionCacheComponent;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DigitalSpecimenProcessingService extends AbstractProcessingService {

  private final VirtualCollectionCacheComponent cache;
  private final RabbitMqPublisherService publisherService;
  private final SpecimenEvaluationComponent specimenEvaluationComponent;

  public DigitalSpecimenProcessingService(ObjectMapper objectMapper,
      ApplicationProperties applicationProperties, VirtualCollectionCacheComponent cache,
      RabbitMqPublisherService publisherService,
      SpecimenEvaluationComponent specimenEvaluationComponent) {
    super(objectMapper, applicationProperties);
    this.cache = cache;
    this.publisherService = publisherService;
    this.specimenEvaluationComponent = specimenEvaluationComponent;
  }


  public void handleIngestionEvents(List<DigitalSpecimenEvent> events)
      throws JsonProcessingException {
    for (var event : events) {
      for (var virtualCollection : cache.getCache()) {
        var specimen = event.digitalSpecimenWrapper().attributes();
        if (specimenEvaluationComponent.evaluateSpecimen(specimen,
            virtualCollection.getOdsHasTargetDigitalObjectFilter())) {
          addVirtualCollection(specimen, virtualCollection.getId(),
              URI.create(virtualCollection.getId()));
          try {
            publisherService.publishDigitalSpecimen(event);
          } catch (JsonProcessingException e) {
            log.error(
                "Manual action needed. Error publishing digital specimen with id: {}, error: {}",
                event.digitalSpecimenWrapper().attributes().getId(), e.getMessage());
          }
        }
      }
    }
  }
}
