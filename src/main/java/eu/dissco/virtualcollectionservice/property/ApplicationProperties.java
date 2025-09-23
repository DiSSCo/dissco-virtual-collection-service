package eu.dissco.virtualcollectionservice.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("application")
public class ApplicationProperties {

  @NotBlank
  private String name = "DiSSCo Virtual Collection Service";

  @NotBlank
  private String pid = "https://doi.org/10.5281/zenodo.17182153";

}
