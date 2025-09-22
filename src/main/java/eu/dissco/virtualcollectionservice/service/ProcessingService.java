package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.component.ElasticSearchQueryParser.parseTargetFilterToQuery;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import eu.dissco.virtualcollectionservice.repository.ElasticSearchRepository;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessingService {

  private static final String ID_FIELD = "dcterms:identifier";

  private final ElasticSearchRepository elasticSearchRepository;

  public void handleMessage(VirtualCollectionEvent virtualCollectionEvent) throws IOException {
    log.info("Received a {} request for virtual collection with id: {}",
        virtualCollectionEvent.action(), virtualCollectionEvent.virtualCollection().getId());
    var filter = virtualCollectionEvent.virtualCollection().getOdsHasTargetDigitalObjectFilter();
    var elasticQuery = parseTargetFilterToQuery(filter);
    var totalResult = processRequest(elasticQuery);
    log.info("Successfully finished processing all results: {} ", totalResult);
  }


  private long processRequest(Query elasticQuery) throws IOException {
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
        processSearchResult(searchResult);
        lastId = searchResult.getLast().get(ID_FIELD).asText();
        resultsProcessed += searchResult.size();
      }
    }
    return resultsProcessed;
  }

  private void processSearchResult(List<JsonNode> searchResult) {
    log.info("Processing {} results", searchResult.size());
  }


}
