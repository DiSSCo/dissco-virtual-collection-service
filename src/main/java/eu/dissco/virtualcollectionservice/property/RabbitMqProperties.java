package eu.dissco.virtualcollectionservice.property;

import jakarta.validation.constraints.NotBlank;
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

}
