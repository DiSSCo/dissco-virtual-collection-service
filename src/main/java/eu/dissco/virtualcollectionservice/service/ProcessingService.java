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
import eu.dissco.virtualcollectionservice.schema.Agent;
import eu.dissco.virtualcollectionservice.schema.Agent.Type;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import eu.dissco.virtualcollectionservice.schema.EntityRelationship;
import eu.dissco.virtualcollectionservice.schema.Identifier;
import eu.dissco.virtualcollectionservice.schema.Identifier.DctermsType;
import eu.dissco.virtualcollectionservice.schema.Identifier.OdsGupriLevel;
import eu.dissco.virtualcollectionservice.schema.Identifier.OdsIdentifierStatus;
import eu.dissco.virtualcollectionservice.schema.OdsHasRole;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessingService {

  private static final String ID_FIELD = "dcterms:identifier";
  private static final String HAS_VIRTUAL_COLLECTION = "hasVirtualCollection";

  private final ObjectMapper objectMapper;
  private final ElasticSearchRepository elasticSearchRepository;
  private final RabbitMqPublisherService rabbitMqPublisherService;
  private final ApplicationProperties applicationProperties;

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

  private void addVirtualCollection(DigitalSpecimen digitalSpecimen, String virtualCollectionId,
      URI virtualCollectionURI) {
    var relationships = digitalSpecimen.getOdsHasEntityRelationships();
    if (relationships.stream().anyMatch(
        relationship -> relationship.getDwcRelationshipOfResource().equals(HAS_VIRTUAL_COLLECTION)
            && relationship.getDwcRelatedResourceID().equals(virtualCollectionId))) {
      log.warn(
          "Digital specimen with id: {} already has a relationship to virtual collection with id: {}. Skipping addition.",
          digitalSpecimen.getId(), virtualCollectionId);
    } else {
      relationships.add(new EntityRelationship()
          .withType("ods:EntityRelationship")
          .withDwcRelationshipOfResource(HAS_VIRTUAL_COLLECTION)
          .withOdsRelatedResourceURI(virtualCollectionURI)
          .withDwcRelatedResourceID(virtualCollectionId)
          .withDwcRelationshipEstablishedDate(Date.from(Instant.now()))
          .withOdsHasAgents(List.of(new Agent()
              .withId(applicationProperties.getPid())
              .withType(Type.SCHEMA_SOFTWARE_APPLICATION)
              .withSchemaName(applicationProperties.getName())
              .withOdsHasRoles(
                  List.of(new OdsHasRole()
                      .withType("schema:Role")
                      .withSchemaRoleName("virtual-collection-manager")
                  )
              )
              .withOdsHasIdentifiers(List.of(new Identifier()
                  .withId(applicationProperties.getPid())
                  .withType("ods:Identifier")
                  .withDctermsTitle("DOI")
                  .withDctermsType(DctermsType.DOI)
                  .withDctermsIdentifier(applicationProperties.getPid())
                  .withOdsIsPartOfLabel(Boolean.FALSE)
                  .withOdsGupriLevel(
                      OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT)
                  .withOdsIdentifierStatus(OdsIdentifierStatus.PREFERRED)))))
      );
    }
  }
}
