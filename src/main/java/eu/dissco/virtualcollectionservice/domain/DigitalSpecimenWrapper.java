package eu.dissco.virtualcollectionservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import tools.jackson.databind.JsonNode;

public record DigitalSpecimenWrapper(@JsonProperty("ods:normalisedPhysicalSpecimenID") String physicalSpecimenID,
		@JsonProperty("ods:type") String type, @JsonProperty("ods:attributes") DigitalSpecimen attributes,
		@JsonProperty("ods:originalAttributes") JsonNode originalAttributes) {

}
