package eu.dissco.virtualcollectionservice.repository;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenElasticQuery;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenSpecimenNode;
import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import eu.dissco.virtualcollectionservice.property.ElasticSearchProperties;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;

@Testcontainers
class ElasticSearchRepositoryIT {

  private static final DockerImageName ELASTIC_IMAGE = DockerImageName.parse(
      "docker.elastic.co/elasticsearch/elasticsearch").withTag("9.2.0");
  private static final String ELASTICSEARCH_USERNAME = "elastic";
  private static final String ELASTICSEARCH_PASSWORD = "s3cret";
  private static final ElasticsearchContainer container = new ElasticsearchContainer(
      ELASTIC_IMAGE).withPassword(ELASTICSEARCH_PASSWORD);
  private static final String DIGITAL_SPECIMEN_INDEX = "digital-specimen";
  private static ElasticsearchClient client;
  private static Rest5Client restClient;
  private final ElasticSearchProperties properties = new ElasticSearchProperties();
  private ElasticSearchRepository elasticRepository;

  @BeforeAll
  static void initContainer() {
    // Create the elasticsearch container.
    container.start();

    var creds = Base64.getEncoder()
        .encodeToString((ELASTICSEARCH_USERNAME + ":" + ELASTICSEARCH_PASSWORD).getBytes());

    restClient = Rest5Client.builder(
            new HttpHost("https", "localhost", container.getMappedPort(9200)))
        .setDefaultHeaders(new Header[]{new BasicHeader("Authorization", "Basic " + creds)})
        .setSSLContext(container.createSslContextFromCa()).build();

    ElasticsearchTransport transport = new Rest5ClientTransport(restClient,
        new Jackson3JsonpMapper(MAPPER));

    client = new ElasticsearchClient(transport);
  }

  @AfterAll
  static void closeResources() throws Exception {
    restClient.close();
  }

  @BeforeEach
  void initRepository() {
    elasticRepository = new ElasticSearchRepository(client, properties);
  }

  @AfterEach
  void clearIndex() throws IOException {
    if (client.indices().exists(re -> re.index(DIGITAL_SPECIMEN_INDEX)).value()) {
      client.indices().delete(b -> b.index(DIGITAL_SPECIMEN_INDEX));
    }
  }

  @Test
  void testRetrieveObjects() throws IOException {
    // Given
    postDigitalSpecimens(List.of(givenSpecimenNode()));

    // When
    var specimen = elasticRepository.retrieveObjects(null, DIGITAL_SPECIMEN_INDEX,
        givenElasticQuery());

    // Then
    assertThat(specimen.getFirst()).isEqualTo(givenSpecimenNode());
  }

  private void postDigitalSpecimens(List<JsonNode> jsonObjects)
      throws IOException {
    var bulkRequest = new BulkRequest.Builder();
    for (var jsonObject : jsonObjects) {
      bulkRequest.operations(op -> op.index(
          idx -> idx.index(DIGITAL_SPECIMEN_INDEX).id(jsonObject.get("@id").asString())
              .document(jsonObject)));
    }
    client.bulk(bulkRequest.build());
    client.indices().refresh(b -> b.index(DIGITAL_SPECIMEN_INDEX));
  }
}
