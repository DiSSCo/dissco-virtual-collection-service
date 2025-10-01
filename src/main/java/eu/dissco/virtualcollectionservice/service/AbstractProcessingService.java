package eu.dissco.virtualcollectionservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.property.ApplicationProperties;
import eu.dissco.virtualcollectionservice.schema.Agent;
import eu.dissco.virtualcollectionservice.schema.Agent.Type;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import eu.dissco.virtualcollectionservice.schema.EntityRelationship;
import eu.dissco.virtualcollectionservice.schema.Identifier;
import eu.dissco.virtualcollectionservice.schema.Identifier.DctermsType;
import eu.dissco.virtualcollectionservice.schema.Identifier.OdsGupriLevel;
import eu.dissco.virtualcollectionservice.schema.Identifier.OdsIdentifierStatus;
import eu.dissco.virtualcollectionservice.schema.OdsHasRole;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AbstractProcessingService {

  private static final String HAS_VIRTUAL_COLLECTION = "hasVirtualCollection";

  protected final ObjectMapper objectMapper;
  private final ApplicationProperties applicationProperties;

  protected void addVirtualCollection(DigitalSpecimen digitalSpecimen, String virtualCollectionId,
      URI virtualCollectionURI) {
    var relationships = digitalSpecimen.getOdsHasEntityRelationships();
    if (relationships.stream().anyMatch(
        relationship -> relationship.getDwcRelationshipOfResource().equals(HAS_VIRTUAL_COLLECTION)
            && relationship.getDwcRelatedResourceID().equals(virtualCollectionId))) {
      log.warn(
          "Digital specimen with id: {} already has a relationship to virtual collection with id: {}. Skipping addition.",
          digitalSpecimen.getId(), virtualCollectionId);
    } else {
      relationships.add(new EntityRelationship()
          .withType("ods:EntityRelationship")
          .withDwcRelationshipOfResource(HAS_VIRTUAL_COLLECTION)
          .withOdsRelatedResourceURI(virtualCollectionURI)
          .withDwcRelatedResourceID(virtualCollectionId)
          .withDwcRelationshipEstablishedDate(Date.from(Instant.now()))
          .withOdsHasAgents(List.of(new Agent()
              .withId(applicationProperties.getPid())
              .withType(Type.SCHEMA_SOFTWARE_APPLICATION)
              .withSchemaName(applicationProperties.getName())
              .withOdsHasRoles(
                  List.of(new OdsHasRole()
                      .withType("schema:Role")
                      .withSchemaRoleName("virtual-collection-manager")
                  )
              )
              .withOdsHasIdentifiers(List.of(new Identifier()
                  .withId(applicationProperties.getPid())
                  .withType("ods:Identifier")
                  .withDctermsTitle("DOI")
                  .withDctermsType(DctermsType.DOI)
                  .withDctermsIdentifier(applicationProperties.getPid())
                  .withOdsIsPartOfLabel(Boolean.FALSE)
                  .withOdsGupriLevel(
                      OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT)
                  .withOdsIdentifierStatus(OdsIdentifierStatus.PREFERRED)))))
      );
    }
  }

}
