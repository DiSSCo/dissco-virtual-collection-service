package eu.dissco.virtualcollectionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.virtualcollectionservice.component.SpecimenEvaluationComponent;
import eu.dissco.virtualcollectionservice.component.VirtualCollectionCacheComponent;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class DigitalSpecimenProcessingService extends AbstractProcessingService {

  private final VirtualCollectionCacheComponent cache;
  private final RabbitMqPublisherService publisherService;
  private final SpecimenEvaluationComponent specimenEvaluationComponent;

  public DigitalSpecimenProcessingService(JsonMapper jsonMapper,
      ApplicationProperties applicationProperties, VirtualCollectionCacheComponent cache,
      RabbitMqPublisherService publisherService,
      SpecimenEvaluationComponent specimenEvaluationComponent) {
    super(jsonMapper, applicationProperties);
    this.cache = cache;
    this.publisherService = publisherService;
    this.specimenEvaluationComponent = specimenEvaluationComponent;
  }


  public void handleIngestionEvents(List<DigitalSpecimenEvent> events)
      throws JsonProcessingException {
    log.info("Handling {} ingestion events", events.size());
    for (var event : events) {
      for (var virtualCollection : cache.getCache()) {
        var specimen = event.digitalSpecimenWrapper().attributes();
        if (specimenEvaluationComponent.evaluateSpecimen(specimen,
            virtualCollection.getOdsHasTargetDigitalObjectFilter())) {
          log.info("Adding specimen with id: {} to virtual collection with id: {}",
              specimen.getOdsNormalisedPhysicalSpecimenID(), virtualCollection.getId());
          addVirtualCollection(specimen, virtualCollection.getId(),
              URI.create(virtualCollection.getId()));
        }
      }
      publisherService.publishDigitalSpecimen(event);
    }
  }
}
