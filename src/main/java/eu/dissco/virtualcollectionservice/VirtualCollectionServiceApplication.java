package eu.dissco.virtualcollectionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VirtualCollectionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(VirtualCollectionServiceApplication.class, args);
  }

}
