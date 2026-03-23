package eu.dissco.virtualcollectionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@AllArgsConstructor
public class RabbitMqConsumerService {

  private final JsonMapper jsonMapper;
  private final VirtualCollectionProcessingService virtualCollectionProcessingService;
  private final DigitalSpecimenProcessingService digitalSpecimenProcessingService;

  @RabbitListener(queues = {
      "${rabbitmq.ingestion-queue-name:virtual-collection-ingestion-queue}"}, containerFactory = "consumerBatchContainerFactory")
  public void getMessages(@Payload List<String> messages) throws JsonProcessingException {
    var events = messages.stream()
        .map(message -> jsonMapper.readValue(message, DigitalSpecimenEvent.class))
        .filter(Objects::nonNull).toList();
    digitalSpecimenProcessingService.handleIngestionEvents(events);
  }

  @RabbitListener(queues = {
      "${rabbitmq.queue-name:virtual-collection-queue}"})
  public void getMessages(String message) throws IOException {
    var virtualCollectionEvent = jsonMapper.readValue(message, VirtualCollectionEvent.class);
    virtualCollectionProcessingService.handleMessage(virtualCollectionEvent);
  }

}
