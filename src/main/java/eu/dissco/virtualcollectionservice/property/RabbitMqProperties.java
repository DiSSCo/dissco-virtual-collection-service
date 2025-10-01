package eu.dissco.virtualcollectionservice.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

  @NotBlank
  private String exchangeName = "digital-specimen-exchange";

  @NotBlank
  private String routingKeyName = "digital-specimen";

  @NotBlank
  private String ingestionDlqExchangeName = "virtual-collection-ingestion-exchange-dlq";

  @NotBlank
  private String ingestionDlqKeyName = "virtual-collection-ingestion-dlq";

  @Positive
  private int batchSize = 500;
}
