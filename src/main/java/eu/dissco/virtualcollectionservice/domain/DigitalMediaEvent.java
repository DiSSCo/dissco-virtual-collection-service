package eu.dissco.virtualcollectionservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record DigitalMediaEvent(
    Set<String> masList,
    @JsonProperty("digitalMedia")
    DigitalMediaWrapper digitalMediaWrapper,
    Boolean forceMasSchedule) {

}
