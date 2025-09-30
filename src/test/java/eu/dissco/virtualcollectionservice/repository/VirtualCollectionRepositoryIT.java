package eu.dissco.virtualcollectionservice.repository;

import static eu.dissco.virtualcollectionservice.database.jooq.Tables.VIRTUAL_COLLECTION;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenVirtualCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.PostgreSQLContainer.IMAGE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zaxxer.hikari.HikariDataSource;
import eu.dissco.virtualcollectionservice.database.jooq.enums.CollectionType;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection.LtcBasisOfScheme;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class VirtualCollectionRepositoryIT {

  private static final DockerImageName POSTGIS =
      DockerImageName.parse("postgres:17.5").asCompatibleSubstituteFor(IMAGE);

  @Container
  private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>(POSTGIS);
  protected DSLContext context;
  private HikariDataSource dataSource;
  private VirtualCollectionRepository repository;

  private static CollectionType getLtcBasisOfScheme(VirtualCollection virtualCollection) {
    return switch (virtualCollection.getLtcBasisOfScheme()) {
      case LtcBasisOfScheme.REFERENCE_COLLECTION -> CollectionType.REFERENCE_COLLECTION;
      case LtcBasisOfScheme.COMMUNITY_COLLECTION -> CollectionType.COMMUNITY_COLLECTION;
    };
  }

  @BeforeEach
  void prepareDatabase() {
    dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(CONTAINER.getJdbcUrl());
    dataSource.setUsername(CONTAINER.getUsername());
    dataSource.setPassword(CONTAINER.getPassword());
    dataSource.setMaximumPoolSize(2);
    dataSource.setConnectionInitSql(CONTAINER.getTestQueryString());
    Flyway.configure().mixed(true).dataSource(dataSource).load().migrate();
    context = new DefaultDSLContext(dataSource, SQLDialect.POSTGRES);
    repository = new VirtualCollectionRepository(context, MAPPER);
  }

  @AfterEach
  void disposeDataSource() {
    dataSource.close();
  }

  @Test
  void testGetAllVirtualCollections() throws JsonProcessingException {
    // Given
    var virtualCollections = List.of(givenVirtualCollection(),
        givenVirtualCollection("https://hdl.handle.net/TEST/YYY-YYY-YYY",
            "Second Virtual Collection"));
    insertVirtualCollection(virtualCollections);

    // When
    var result = repository.getAllVirtualCollections();
    // Then
    assertThat(result).hasSameElementsAs(virtualCollections);
  }

  private void insertVirtualCollection(List<VirtualCollection> virtualCollections) throws JsonProcessingException {
    for (var virtualCollection : virtualCollections) {
      context.insertInto(VIRTUAL_COLLECTION)
          .set(VIRTUAL_COLLECTION.ID,
              virtualCollection.getId().replace("https://hdl.handle.net/", ""))
          .set(VIRTUAL_COLLECTION.VERSION, virtualCollection.getSchemaVersion())
          .set(VIRTUAL_COLLECTION.NAME, virtualCollection.getLtcCollectionName())
          .set(VIRTUAL_COLLECTION.COLLECTION_TYPE, getLtcBasisOfScheme(virtualCollection))
          .set(VIRTUAL_COLLECTION.CREATED, virtualCollection.getSchemaDateCreated().toInstant())
          .set(VIRTUAL_COLLECTION.MODIFIED, virtualCollection.getSchemaDateModified().toInstant())
          .set(VIRTUAL_COLLECTION.CREATOR, virtualCollection.getSchemaCreator().getId())
          .set(VIRTUAL_COLLECTION.DATA, mapToJSONB(virtualCollection))
          .execute();
    }
  }

  private JSONB mapToJSONB(VirtualCollection virtualCollection) throws JsonProcessingException {
    return JSONB.valueOf(MAPPER.writeValueAsString(virtualCollection));
  }


}
