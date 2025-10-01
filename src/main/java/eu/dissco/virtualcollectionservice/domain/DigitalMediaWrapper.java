package eu.dissco.virtualcollectionservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.virtualcollectionservice.schema.DigitalMedia;


public record DigitalMediaWrapper(
    @JsonProperty("ods:type")
    String type,
    @JsonProperty("ods:attributes")
    DigitalMedia attributes,
    @JsonProperty("ods:originalAttributes")
    JsonNode originalAttributes) {

}
