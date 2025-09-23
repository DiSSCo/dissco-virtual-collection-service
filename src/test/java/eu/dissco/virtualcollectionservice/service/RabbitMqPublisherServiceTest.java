package eu.dissco.virtualcollectionservice.service;

import static eu.dissco.virtualcollectionservice.utils.TestUtils.MAPPER;
import static eu.dissco.virtualcollectionservice.utils.TestUtils.givenDigitalSpecimenEventWithVC;
import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.virtualcollectionservice.property.RabbitMqProperties;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RabbitMqPublisherServiceTest {

  private static RabbitMQContainer container;
  private static RabbitTemplate rabbitTemplate;
  private RabbitMqPublisherService rabbitMqPublisherService;


  @BeforeAll
  static void setupContainer() throws IOException, InterruptedException {
    container = new RabbitMQContainer("rabbitmq:4.0.8-management-alpine");
    container.start();
    // Declare digital specimen exchange, queue and binding
    declareRabbitResources("digital-specimen-exchange", "digital-specimen-queue",
        "digital-specimen");
    CachingConnectionFactory factory = new CachingConnectionFactory(container.getHost());
    factory.setPort(container.getAmqpPort());
    factory.setUsername(container.getAdminUsername());
    factory.setPassword(container.getAdminPassword());
    rabbitTemplate = new RabbitTemplate(factory);
    rabbitTemplate.setReceiveTimeout(100L);
  }

  private static void declareRabbitResources(String exchangeName, String queueName,
      String routingKey)
      throws IOException, InterruptedException {
    container.execInContainer("rabbitmqadmin", "declare", "exchange", "name=" + exchangeName,
        "type=direct", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "queue", "name=" + queueName,
        "queue_type=quorum", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "binding", "source=" + exchangeName,
        "destination_type=queue", "destination=" + queueName, "routing_key=" + routingKey);
  }

  @AfterAll
  static void shutdownContainer() {
    container.stop();
  }

  @BeforeEach
  void setup() {
    rabbitMqPublisherService = new RabbitMqPublisherService(rabbitTemplate,
        new RabbitMqProperties(), MAPPER);
  }

  @Test
  void testPublishDigitalSpecimen() throws IOException {
    // Given
    var event = givenDigitalSpecimenEventWithVC();

    // When
    rabbitMqPublisherService.publishDigitalSpecimen(event);

    // Then
    var receivedMessage = rabbitTemplate.receive("digital-specimen-queue");
    assertThat(new String(receivedMessage.getBody())).isNotNull();
  }
}
