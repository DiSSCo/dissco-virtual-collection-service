package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.component.ElasticSearchQueryParser.parseTargetFilterToQuery;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenWrapper;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionAction;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import eu.dissco.virtualcollectionservice.repository.ElasticSearchRepository;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class VirtualCollectionProcessingService extends AbstractProcessingService {

	private static final String ID_FIELD = "dcterms:identifier";

	private final ElasticSearchRepository elasticSearchRepository;

	private final RabbitMqPublisherService rabbitMqPublisherService;

	public VirtualCollectionProcessingService(JsonMapper jsonMapper, ElasticSearchRepository elasticSearchRepository,
			RabbitMqPublisherService rabbitMqPublisherService, ApplicationProperties applicationProperties) {
		super(jsonMapper, applicationProperties);
		this.elasticSearchRepository = elasticSearchRepository;
		this.rabbitMqPublisherService = rabbitMqPublisherService;
	}

	public void handleMessage(VirtualCollectionEvent virtualCollectionEvent) throws IOException {
		log.info("Received a {} request for virtual collection with id: {}", virtualCollectionEvent.action(),
				virtualCollectionEvent.virtualCollection().getId());
		var filter = virtualCollectionEvent.virtualCollection().getOdsHasTargetDigitalObjectFilter();
		var elasticQuery = parseTargetFilterToQuery(filter);
		var totalResult = processRequest(elasticQuery, virtualCollectionEvent);
		log.info("Successfully finished processing all results: {} ", totalResult);
	}

	private long processRequest(Query elasticQuery, VirtualCollectionEvent virtualCollectionEvent) throws IOException {
		boolean keepSearching = true;
		long resultsProcessed = 0;
		String lastId = null;
		while (keepSearching) {
			log.info("Paginating over elastic, resultsProcessed: {}", resultsProcessed);
			var searchResult = elasticSearchRepository.retrieveObjects(lastId, "digital-specimen", elasticQuery);
			if (searchResult.isEmpty()) {
				keepSearching = false;
			}
			else {
				processSearchResult(searchResult, virtualCollectionEvent);
				lastId = searchResult.getLast().get(ID_FIELD).asString();
				resultsProcessed += searchResult.size();
			}
		}
		return resultsProcessed;
	}

	private void processSearchResult(List<JsonNode> searchResult, VirtualCollectionEvent virtualCollectionEvent) {
		log.info("Processing {} results", searchResult.size());
		var virtualCollectionId = virtualCollectionEvent.virtualCollection().getId();
		var virtualCollectionUri = URI.create(virtualCollectionId);
		searchResult.stream()
			.map(json -> jsonMapper.convertValue(json, DigitalSpecimen.class))
			.forEach(digitalSpecimen -> {
				log.info("Processing digital specimen with id: {}", digitalSpecimen.getId());
				if (VirtualCollectionAction.CREATE.equals(virtualCollectionEvent.action())) {
					addVirtualCollection(digitalSpecimen, virtualCollectionId, virtualCollectionUri);
				}
				else {
					removeVirtualCollection(digitalSpecimen, virtualCollectionId, virtualCollectionUri);
				}
				var digitalSpecimenEvent = wrapIntoEvent(digitalSpecimen);
				rabbitMqPublisherService.publishDigitalSpecimen(digitalSpecimenEvent);
			});
	}

	private void removeVirtualCollection(DigitalSpecimen digitalSpecimen, String virtualCollectionId,
			URI virtualCollectionURI) {
		log.info("Removing virtual collection with id: {} from digital specimen with id: {}", virtualCollectionId,
				digitalSpecimen.getId());
		digitalSpecimen.setOdsHasEntityRelationships(digitalSpecimen.getOdsHasEntityRelationships()
			.stream()
			.filter(er -> !er.getOdsRelatedResourceURI().equals(virtualCollectionURI))
			.toList());
	}

	private DigitalSpecimenEvent wrapIntoEvent(DigitalSpecimen digitalSpecimen) {
		return new DigitalSpecimenEvent(Collections.emptySet(),
				new DigitalSpecimenWrapper(digitalSpecimen.getOdsNormalisedPhysicalSpecimenID(),
						digitalSpecimen.getOdsFdoType(), digitalSpecimen, null),
				Collections.emptyList(), false, false);
	}

}
