package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.component.ElasticSearchQueryParser.parseTargetFilterToQuery;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenWrapper;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import eu.dissco.virtualcollectionservice.repository.ElasticSearchRepository;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VirtualCollectionProcessingService extends AbstractProcessingService {

  private static final String ID_FIELD = "dcterms:identifier";

  private final ElasticSearchRepository elasticSearchRepository;
  private final RabbitMqPublisherService rabbitMqPublisherService;


  public VirtualCollectionProcessingService(ObjectMapper objectMapper,
      ElasticSearchRepository elasticSearchRepository,
      RabbitMqPublisherService rabbitMqPublisherService,
      ApplicationProperties applicationProperties) {
    super(objectMapper, applicationProperties);
    this.elasticSearchRepository = elasticSearchRepository;
    this.rabbitMqPublisherService = rabbitMqPublisherService;
  }

  public void handleMessage(VirtualCollectionEvent virtualCollectionEvent) throws IOException {
    log.info("Received a {} request for virtual collection with id: {}",
        virtualCollectionEvent.action(), virtualCollectionEvent.virtualCollection().getId());
    var filter = virtualCollectionEvent.virtualCollection().getOdsHasTargetDigitalObjectFilter();
    var elasticQuery = parseTargetFilterToQuery(filter);
    var totalResult = processRequest(elasticQuery, virtualCollectionEvent.virtualCollection());
    log.info("Successfully finished processing all results: {} ", totalResult);
  }

  private long processRequest(Query elasticQuery, VirtualCollection virtualCollection)
      throws IOException {
    boolean keepSearching = true;
    long resultsProcessed = 0;
    String lastId = null;
    while (keepSearching) {
      log.info("Paginating over elastic, resultsProcessed: {}", resultsProcessed);
      var searchResult = elasticSearchRepository.retrieveObjects(lastId, "digital-specimen",
          elasticQuery);
      if (searchResult.isEmpty()) {
        keepSearching = false;
      } else {
        processSearchResult(searchResult, virtualCollection);
        lastId = searchResult.getLast().get(ID_FIELD).asText();
        resultsProcessed += searchResult.size();
      }
    }
    return resultsProcessed;
  }

  private void processSearchResult(List<JsonNode> searchResult,
      VirtualCollection virtualCollection) {
    log.info("Processing {} results", searchResult.size());
    var virtualCollectionId = virtualCollection.getId();
    var virtualCollectionURI = URI.create(virtualCollectionId);
    searchResult.stream().map(json -> objectMapper.convertValue(json, DigitalSpecimen.class))
        .forEach(digitalSpecimen -> {
          log.info("Processing digital specimen with id: {}", digitalSpecimen.getId());
          try {
            addVirtualCollection(digitalSpecimen, virtualCollectionId, virtualCollectionURI);
            var digitalSpecimenEvent = wrapIntoEvent(digitalSpecimen);
            rabbitMqPublisherService.publishDigitalSpecimen(digitalSpecimenEvent);
          } catch (JsonProcessingException e) {
            log.error(
                "Manual action needed. Error publishing digital specimen with id: {}, error: {}",
                digitalSpecimen.getId(), e.getMessage());
          }
        });
  }

  private DigitalSpecimenEvent wrapIntoEvent(DigitalSpecimen digitalSpecimen) {
    return new DigitalSpecimenEvent(
        Collections.emptySet(),
        new DigitalSpecimenWrapper(
            digitalSpecimen.getOdsNormalisedPhysicalSpecimenID(),
            digitalSpecimen.getOdsFdoType(),
            digitalSpecimen,
            objectMapper.createObjectNode()
        ),
        Collections.emptyList(),
        false,
        false
    );
  }
}
