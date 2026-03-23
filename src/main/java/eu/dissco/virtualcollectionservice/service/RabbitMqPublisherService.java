package eu.dissco.virtualcollectionservice.service;

import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.property.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqPublisherService {

  private final RabbitTemplate rabbitTemplate;
  private final RabbitMqProperties rabbitProperties;
  private final JsonMapper jsonMapper;

  public void publishDigitalSpecimen(DigitalSpecimenEvent digitalSpecimenEvent) {
    rabbitTemplate.convertAndSend(rabbitProperties.getExchangeName(),
        rabbitProperties.getRoutingKeyName(),
        jsonMapper.writeValueAsString(digitalSpecimenEvent));
  }

}
