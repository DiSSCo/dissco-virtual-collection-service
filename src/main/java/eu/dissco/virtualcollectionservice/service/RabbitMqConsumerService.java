package eu.dissco.virtualcollectionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class RabbitMqConsumerService {

  private final ObjectMapper objectMapper;
  private final ProcessingService processingService;

  @RabbitListener(queues = {
      "${rabbitmq.queue-name:virtual-collection-queue}"})
  public void getMessages(String message) throws IOException {
    var virtualCollectionEvent = objectMapper.readValue(message, VirtualCollectionEvent.class);
    processingService.handleMessage(virtualCollectionEvent);
  }

}
