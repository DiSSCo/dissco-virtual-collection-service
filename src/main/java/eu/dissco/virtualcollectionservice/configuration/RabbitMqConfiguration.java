package eu.dissco.virtualcollectionservice.configuration;

import eu.dissco.virtualcollectionservice.component.MessageCompressionComponent;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class RabbitMqConfiguration {

  @Bean
  public RabbitTemplate compressedTemplate(ConnectionFactory connectionFactory,
      MessageCompressionComponent compressedMessageConverter) {
    var rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(compressedMessageConverter);
    return rabbitTemplate;
  }
}
