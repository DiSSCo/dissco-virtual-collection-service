package eu.dissco.virtualcollectionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.property.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqPublisherService {

  private final RabbitTemplate rabbitTemplate;
  private final RabbitMqProperties rabbitProperties;
  private final ObjectMapper objectMapper;

  public void publishDigitalSpecimen(DigitalSpecimenEvent digitalSpecimenEvent)
      throws JsonProcessingException {
    log.info("Publishing digital specimen with id: {}",
        digitalSpecimenEvent.digitalSpecimenWrapper().attributes().getId());
    rabbitTemplate.convertAndSend(rabbitProperties.getExchangeName(),
        rabbitProperties.getRoutingKeyName(),
        objectMapper.writeValueAsString(digitalSpecimenEvent));
  }

  public void sendMessageDLQ(Object message) {
    rabbitTemplate.convertAndSend(rabbitProperties.getIngestionDlqExchangeName(),
        rabbitProperties.getIngestionDlqKeyName(), message);
  }

}
