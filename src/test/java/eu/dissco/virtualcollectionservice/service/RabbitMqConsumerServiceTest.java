package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenVirtualCollectionEvent;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RabbitMqConsumerServiceTest {

  @Mock
  private ProcessingService processingService;
  private RabbitMqConsumerService consumerService;

  @BeforeEach
  void setup() {
    consumerService = new RabbitMqConsumerService(MAPPER, processingService);
  }

  @Test
  void testGetMessages() throws IOException {
    // Given
    var message = MAPPER.writeValueAsString(givenVirtualCollectionEvent());

    // When
    consumerService.getMessages(message);

    // Then
    then(processingService).should().handleMessage(givenVirtualCollectionEvent());
  }
}
