package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenSpecimenNode;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenVirtualCollectionEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import eu.dissco.virtualcollectionservice.repository.ElasticSearchRepository;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {

  @Mock
  private ElasticSearchRepository repository;

  private ProcessingService service;

  @BeforeEach
  void setup() {
    service = new ProcessingService(repository);
  }

  @Test
  void testHandleMessage() throws IOException {
    // Given
    var event = givenVirtualCollectionEvent();
    given(repository.retrieveObjects(any(), eq("digital-specimen"), any(Query.class))).willReturn(
        List.of(givenSpecimenNode())).willReturn(List.of());

    // When
    service.handleMessage(event);

    // Then
    then(repository).should(times(2))
        .retrieveObjects(any(), eq("digital-specimen"), any(Query.class));
  }
}
