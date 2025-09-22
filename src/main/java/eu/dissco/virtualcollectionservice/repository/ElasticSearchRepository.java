package eu.dissco.virtualcollectionservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.virtualcollectionservice.property.ElasticSearchProperties;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ElasticSearchRepository {

  private static final String SORT_BY = "dcterms:identifier.keyword";
  private final ElasticsearchClient client;
  private final ElasticSearchProperties properties;

  public List<JsonNode> retrieveObjects(String lastId, String index, Query query)
      throws IOException {
    var searchRequestBuilder = new SearchRequest.Builder()
        .index(index)
        .query(query)
        .trackTotalHits(t -> t.enabled(Boolean.TRUE))
        .size(properties.getPageSize())
        .sort(s -> s.field(f -> f.field(SORT_BY).order(SortOrder.Desc)));
    if (lastId != null) {
      searchRequestBuilder
          .searchAfter(sa -> sa.stringValue(lastId));
    }
    var searchResult = client.search(searchRequestBuilder.build(), ObjectNode.class);
    return searchResult.hits().hits().stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .map(JsonNode.class::cast)
        .toList();
  }
}
