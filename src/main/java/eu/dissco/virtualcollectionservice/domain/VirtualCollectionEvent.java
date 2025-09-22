package eu.dissco.virtualcollectionservice.domain;

import eu.dissco.virtualcollectionservice.schema.VirtualCollection;

public record VirtualCollectionEvent(
    VirtualCollectionAction action,
    VirtualCollection virtualCollection
) {

}
