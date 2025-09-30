package eu.dissco.virtualcollectionservice.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenEvent;
import eu.dissco.virtualcollectionservice.domain.DigitalSpecimenWrapper;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionAction;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import eu.dissco.virtualcollectionservice.schema.Agent;
import eu.dissco.virtualcollectionservice.schema.Agent.Type;
import eu.dissco.virtualcollectionservice.schema.DigitalSpecimen;
import eu.dissco.virtualcollectionservice.schema.EntityRelationship;
import eu.dissco.virtualcollectionservice.schema.Identifier;
import eu.dissco.virtualcollectionservice.schema.Identifier.DctermsType;
import eu.dissco.virtualcollectionservice.schema.Identifier.OdsGupriLevel;
import eu.dissco.virtualcollectionservice.schema.Identifier.OdsIdentifierStatus;
import eu.dissco.virtualcollectionservice.schema.OdsHasPredicate;
import eu.dissco.virtualcollectionservice.schema.OdsHasRole;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection.LtcBasisOfScheme;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection.OdsStatus;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class TestUtils {

  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  public static final String SPECIMEN_ID = "https://doi.org/TEST/ZZZ-X4T-YYV";
  public static final String APP_ID = "https://doi.org/10.5281/zenodo.17182153";
  public static final String APP_NAME = "DiSSCo Virtual Collection Service";
  public static final Instant CREATED = Instant.parse("2025-09-23T12:05:24.000Z");
  private static final String VC_ID = "https://hdl.handle.net/TEST/XXX-XXX-XXX";

  public static VirtualCollectionEvent givenVirtualCollectionEvent() {
    return new VirtualCollectionEvent(
        VirtualCollectionAction.CREATE,
        givenVirtualCollection()
    );
  }

  public static VirtualCollection givenVirtualCollection() {
    return givenVirtualCollection(VC_ID, "A test collection");
  }

  public static VirtualCollection givenVirtualCollection(String id, String name) {
    return new VirtualCollection()
        .withId(id)
        .withDctermsIdentifier(id)
        .withType("VirtualCollection")
        .withLtcCollectionName(name)
        .withLtcBasisOfScheme(LtcBasisOfScheme.REFERENCE_COLLECTION)
        .withOdsStatus(OdsStatus.ACTIVE)
        .withSchemaVersion(1)
        .withSchemaDateCreated(Date.from(Instant.now()))
        .withSchemaDateModified(Date.from(Instant.now()))
        .withSchemaCreator(new Agent().withId("https://orcid.org/0000-0002-5669-2769"))
        .withOdsHasTargetDigitalObjectFilter(givenTargetDigitalObjectFilter());
  }

  public static TargetDigitalObjectFilter givenTargetDigitalObjectFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.EQUALS)
        .withOdsPredicateKey("$['dcterms:identifier']")
        .withOdsPredicateValue("https://doi.org/TEST/ZZZ-X4T-YYV");
  }

  public static TargetDigitalObjectFilter givenNotFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.NOT)
        .withOdsPredicateKey("$['ods:physicalSpecimenIDType']")
        .withOdsPredicateValue("Local");
  }

  public static TargetDigitalObjectFilter givenInFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.IN)
        .withOdsPredicateKey("$['ods:topicDiscipline']")
        .withOdsPredicateValues(List.of("Botany", "Zoology"));
  }

  public static TargetDigitalObjectFilter givenOrFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.OR)
        .withOdsHasPredicates(List.of(
            new OdsHasPredicate()
                .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                .withOdsPredicateKey("$['ods:topicDiscipline']")
                .withOdsPredicateValue("Botany"),
            new OdsHasPredicate()
                .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                .withOdsPredicateKey("$['ods:version']")
                .withOdsPredicateValue(2)
        ));
  }

  public static TargetDigitalObjectFilter givenAndFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.AND)
        .withOdsHasPredicates(List.of(
            new OdsHasPredicate()
                .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                .withOdsPredicateKey("$['ods:isKnownToContainMedia']")
                .withOdsPredicateValue(false),
            new OdsHasPredicate()
                .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                .withOdsPredicateKey("$['dwc:preparations']")
                .withOdsPredicateValue("herbarium sheet")
        ));
  }

  public static TargetDigitalObjectFilter givenEqualsListFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.EQUALS)
        .withOdsPredicateKey("$['ods:topicDiscipline']")
        .withOdsPredicateValues(List.of("Botany", "Zoology"));
  }

  public static Stream<Arguments> illegalFilters() {
    return Stream.of(
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.AND)
            .withOdsPredicateKey("@id")
            .withOdsPredicateValue("http://example.com/specimen/12345")),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.NOT)
            .withOdsPredicateKey("ods:topicDiscipline")
            .withOdsPredicateValues(List.of("botany", "zoology"))),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.IN)
            .withOdsPredicateKey("ods:topicDiscipline")
            .withOdsPredicateValues(List.of("botany"))),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.IN)
            .withOdsPredicateKey("ods:topicDiscipline")
            .withOdsPredicateValues(null)),
        Arguments.of(new TargetDigitalObjectFilter()
            .withOdsPredicateType(OdsPredicateType.EQUALS)
            .withOdsHasPredicates(List.of(
                new OdsHasPredicate()
                    .withOdsPredicateType(OdsHasPredicate.OdsPredicateType.EQUALS)
                    .withOdsPredicateKey("ods:topicDiscipline")
                    .withOdsPredicateValues(List.of("botany", "zoology"))
            ))
        )
    );
  }

  public static Query givenElasticQuery() {
    return new Query.Builder().bool(b -> b.must(new Query.Builder().term(
        t -> t.field("@id.keyword")
            .value(FieldValue.of(SPECIMEN_ID))
            .caseInsensitive(true)).build())).build();
  }

  public static DigitalSpecimenEvent givenDigitalSpecimenEventWithVC()
      throws JsonProcessingException {
    return new DigitalSpecimenEvent(
        Collections.emptySet(),
        new DigitalSpecimenWrapper(
            "https://herbarium.bgbm.org/object/B100039428",
            "https://doi.org/21.T11148/894b1e6cad57e921764e",
            givenDigitalSpecimenWithVC(),
            MAPPER.createObjectNode()
        ),
        Collections.emptyList(),
        false,
        false
    );
  }

  public static DigitalSpecimenEvent givenDigitalSpecimenEvent()
      throws JsonProcessingException {
    return new DigitalSpecimenEvent(
        Collections.emptySet(),
        new DigitalSpecimenWrapper(
            "https://herbarium.bgbm.org/object/B100039428",
            "https://doi.org/21.T11148/894b1e6cad57e921764e",
            givenDigitalSpecimen(),
            MAPPER.createObjectNode()
        ),
        Collections.emptyList(),
        false,
        true
    );
  }

  public static DigitalSpecimen givenDigitalSpecimenWithVC() throws JsonProcessingException {
    var digitalSpecimen = MAPPER.convertValue(givenDigitalSpecimen(), DigitalSpecimen.class);
    digitalSpecimen.getOdsHasEntityRelationships().add(
        new EntityRelationship()
            .withType("ods:EntityRelationship")
            .withDwcRelationshipOfResource("hasVirtualCollection")
            .withOdsRelatedResourceURI(URI.create(VC_ID))
            .withDwcRelatedResourceID(VC_ID)
            .withDwcRelationshipEstablishedDate(Date.from(CREATED))
            .withOdsHasAgents(List.of(new Agent()
                .withId(APP_ID)
                .withType(Type.SCHEMA_SOFTWARE_APPLICATION)
                .withSchemaName(APP_NAME)
                .withOdsHasRoles(
                    List.of(new OdsHasRole()
                        .withType("schema:Role")
                        .withSchemaRoleName("virtual-collection-manager")
                    )
                )
                .withOdsHasIdentifiers(List.of(new Identifier()
                    .withId(APP_ID)
                    .withType("ods:Identifier")
                    .withDctermsTitle("DOI")
                    .withDctermsType(DctermsType.DOI)
                    .withDctermsIdentifier(APP_ID)
                    .withOdsIsPartOfLabel(Boolean.FALSE)
                    .withOdsGupriLevel(
                        OdsGupriLevel.GLOBALLY_UNIQUE_STABLE_PERSISTENT_RESOLVABLE_FDO_COMPLIANT)
                    .withOdsIdentifierStatus(OdsIdentifierStatus.PREFERRED)))))
    );
    return digitalSpecimen;
  }

  public static DigitalSpecimen givenDigitalSpecimen() throws JsonProcessingException {
    return MAPPER.convertValue(givenSpecimenNode(), DigitalSpecimen.class);
  }

  public static JsonNode givenSpecimenNode() throws JsonProcessingException {
    return MAPPER.readTree(
        """
            {
              "@id" : "https://doi.org/TEST/ZZZ-X4T-YYV",
              "@type" : "ods:DigitalSpecimen",
              "dcterms:identifier" : "https://doi.org/TEST/ZZZ-X4T-YYV",
              "ods:version" : 1,
              "ods:status" : "Active",
              "dcterms:modified" : "1455095789000",
              "dcterms:created" : "2025-06-06T05:59:58.181Z",
              "ods:fdoType" : "https://doi.org/21.T11148/894b1e6cad57e921764e",
              "ods:midsLevel" : 1,
              "ods:normalisedPhysicalSpecimenID" : "https://herbarium.bgbm.org/object/B100039428",
              "ods:physicalSpecimenID" : "https://herbarium.bgbm.org/object/B100039428",
              "ods:physicalSpecimenIDType" : "Resolvable",
              "ods:isKnownToContainMedia" : false,
              "ods:sourceSystemID" : "https://hdl.handle.net/TEST/4BW-0WF-NLY",
              "ods:sourceSystemName" : "Herbarium Berolinense, Berlin (B)",
              "ods:livingOrPreserved" : "Preserved",
              "dcterms:license" : "http://creativecommons.org/publicdomain/zero/1.0/",
              "dwc:basisOfRecord" : "PreservedSpecimen",
              "ods:organisationID" : "https://ror.org/00bv4cx53",
              "ods:organisationName" : "Botanic Garden and Botanical Museum Berlin",
              "dwc:collectionID" : "Herbarium Berolinense",
              "ods:topicOrigin" : "Natural",
              "ods:topicDomain" : "Life",
              "ods:topicDiscipline" : "Botany",
              "ods:specimenName" : "Acalypha L.",
              "dwc:preparations" : "herbarium sheet",
              "dwc:datasetName" : "Herbarium Berolinense, Berlin (B)",
              "ods:hasEntityRelationships" : [ {
                "@type" : "ods:EntityRelationship",
                "dwc:relationshipOfResource" : "hasOrganisationID",
                "dwc:relatedResourceID" : "https://ror.org/00bv4cx53",
                "ods:relatedResourceURI" : "https://ror.org/00bv4cx53",
                "dwc:relationshipEstablishedDate" : "2025-06-06T01:47:28.463Z",
                "ods:hasAgents" : [ {
                  "@id" : "https://doi.org/10.5281/zenodo.14379776",
                  "@type" : "schema:SoftwareApplication",
                  "schema:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                  "schema:name" : "DiSSCo Translator Service",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "data-translator"
                  } ],
                  "ods:hasIdentifiers" : [ {
                    "@id" : "https://doi.org/10.5281/zenodo.14379776",
                    "@type" : "ods:Identifier",
                    "dcterms:title" : "DOI",
                    "dcterms:type" : "DOI",
                    "dcterms:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                    "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvableFDOCompliant",
                    "ods:identifierStatus" : "Preferred"
                  } ]
                } ]
              }, {
                "@type" : "ods:EntityRelationship",
                "dwc:relationshipOfResource" : "hasSourceSystemID",
                "dwc:relatedResourceID" : "https://hdl.handle.net/TEST/4BW-0WF-NLY",
                "ods:relatedResourceURI" : "https://hdl.handle.net/TEST/4BW-0WF-NLY",
                "dwc:relationshipEstablishedDate" : "2025-06-06T01:47:28.463Z",
                "ods:hasAgents" : [ {
                  "@id" : "https://doi.org/10.5281/zenodo.14379776",
                  "@type" : "schema:SoftwareApplication",
                  "schema:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                  "schema:name" : "DiSSCo Translator Service",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "data-translator"
                  } ],
                  "ods:hasIdentifiers" : [ {
                    "@id" : "https://doi.org/10.5281/zenodo.14379776",
                    "@type" : "ods:Identifier",
                    "dcterms:title" : "DOI",
                    "dcterms:type" : "DOI",
                    "dcterms:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                    "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvableFDOCompliant",
                    "ods:identifierStatus" : "Preferred"
                  } ]
                } ]
              }, {
                "@type" : "ods:EntityRelationship",
                "dwc:relationshipOfResource" : "hasFDOType",
                "dwc:relatedResourceID" : "https://doi.org/21.T11148/894b1e6cad57e921764e",
                "ods:relatedResourceURI" : "https://doi.org/21.T11148/894b1e6cad57e921764e",
                "dwc:relationshipEstablishedDate" : "2025-06-06T01:47:28.463Z",
                "ods:hasAgents" : [ {
                  "@id" : "https://doi.org/10.5281/zenodo.14379776",
                  "@type" : "schema:SoftwareApplication",
                  "schema:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                  "schema:name" : "DiSSCo Translator Service",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "data-translator"
                  } ],
                  "ods:hasIdentifiers" : [ {
                    "@id" : "https://doi.org/10.5281/zenodo.14379776",
                    "@type" : "ods:Identifier",
                    "dcterms:title" : "DOI",
                    "dcterms:type" : "DOI",
                    "dcterms:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                    "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvableFDOCompliant",
                    "ods:identifierStatus" : "Preferred"
                  } ]
                } ]
              }, {
                "@type" : "ods:EntityRelationship",
                "dwc:relationshipOfResource" : "hasPhysicalIdentifier",
                "dwc:relatedResourceID" : "https://herbarium.bgbm.org/object/B100039428",
                "ods:relatedResourceURI" : "https://herbarium.bgbm.org/object/B100039428",
                "dwc:relationshipEstablishedDate" : "2025-06-06T01:47:28.463Z",
                "ods:hasAgents" : [ {
                  "@id" : "https://doi.org/10.5281/zenodo.14379776",
                  "@type" : "schema:SoftwareApplication",
                  "schema:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                  "schema:name" : "DiSSCo Translator Service",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "data-translator"
                  } ],
                  "ods:hasIdentifiers" : [ {
                    "@id" : "https://doi.org/10.5281/zenodo.14379776",
                    "@type" : "ods:Identifier",
                    "dcterms:title" : "DOI",
                    "dcterms:type" : "DOI",
                    "dcterms:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                    "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvableFDOCompliant",
                    "ods:identifierStatus" : "Preferred"
                  } ]
                } ]
              }, {
                "@type" : "ods:EntityRelationship",
                "dwc:relationshipOfResource" : "hasLicense",
                "dwc:relatedResourceID" : "http://creativecommons.org/publicdomain/zero/1.0/",
                "ods:relatedResourceURI" : "http://creativecommons.org/publicdomain/zero/1.0/",
                "dwc:relationshipEstablishedDate" : "2025-06-06T01:47:28.463Z",
                "ods:hasAgents" : [ {
                  "@id" : "https://doi.org/10.5281/zenodo.14379776",
                  "@type" : "schema:SoftwareApplication",
                  "schema:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                  "schema:name" : "DiSSCo Translator Service",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "data-translator"
                  } ],
                  "ods:hasIdentifiers" : [ {
                    "@id" : "https://doi.org/10.5281/zenodo.14379776",
                    "@type" : "ods:Identifier",
                    "dcterms:title" : "DOI",
                    "dcterms:type" : "DOI",
                    "dcterms:identifier" : "https://doi.org/10.5281/zenodo.14379776",
                    "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvableFDOCompliant",
                    "ods:identifierStatus" : "Preferred"
                  } ]
                } ]
              }, {
                "@type" : "ods:EntityRelationship",
                "dwc:relationshipOfResource" : "hasCOLID",
                "dwc:relatedResourceID" : "8VT5Y",
                "ods:relatedResourceURI" : "https://www.catalogueoflife.org/data/taxon/8VT5Y",
                "dwc:relationshipEstablishedDate" : "2025-06-06T05:44:18.432Z",
                "ods:hasAgents" : [ {
                  "@id" : "https://doi.org/10.5281/zenodo.14380476",
                  "@type" : "schema:SoftwareApplication",
                  "schema:identifier" : "https://doi.org/10.5281/zenodo.14380476",
                  "schema:name" : "DiSSCo Name Usage Search Service",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "taxon-resolver"
                  } ],
                  "ods:hasIdentifiers" : [ {
                    "@id" : "https://doi.org/10.5281/zenodo.14380476",
                    "@type" : "ods:Identifier",
                    "dcterms:title" : "DOI",
                    "dcterms:type" : "DOI",
                    "dcterms:identifier" : "https://doi.org/10.5281/zenodo.14380476",
                    "ods:isPartOfLabel" : false,
                    "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvableFDOCompliant",
                    "ods:identifierStatus" : "Preferred"
                  } ]
                } ]
              } ],
              "ods:hasIdentifications" : [ {
                "@type" : "ods:Identification",
                "ods:identificationType" : "TaxonIdentification",
                "dwc:verbatimIdentification" : "Acalypha L.",
                "ods:isVerifiedIdentification" : true,
                "ods:hasAgents" : [ {
                  "@type" : "schema:Person",
                  "schema:name" : "A.Soto (INB) 2006-08 [en LAGU]",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "identifier"
                  } ]
                } ],
                "ods:hasTaxonIdentifications" : [ {
                  "@id" : "https://www.catalogueoflife.org/data/taxon/8VT5Y",
                  "@type" : "ods:TaxonIdentification",
                  "dwc:taxonID" : "https://www.catalogueoflife.org/data/taxon/8VT5Y",
                  "dwc:scientificName" : "Acalypha L.",
                  "ods:scientificNameHTMLLabel" : "<i>Acalypha</i> L.",
                  "dwc:scientificNameAuthorship" : "L.",
                  "dwc:taxonRank" : "GENUS",
                  "dwc:verbatimTaxonRank" : "genus",
                  "dwc:kingdom" : "Plantae",
                  "dwc:phylum" : "Tracheophyta",
                  "dwc:class" : "Magnoliopsida",
                  "dwc:order" : "Malpighiales",
                  "dwc:family" : "Euphorbiaceae",
                  "dwc:genus" : "Acalypha",
                  "dwc:taxonomicStatus" : "ACCEPTED"
                } ]
              } ],
              "ods:hasIdentifiers" : [ {
                "@id" : "B 10 0039428",
                "@type" : "ods:Identifier",
                "dcterms:title" : "abcd:unitID",
                "dcterms:type" : "Locally unique identifier",
                "dcterms:identifier" : "B 10 0039428",
                "ods:gupriLevel" : "LocallyUniqueStable"
              }, {
                "@id" : "https://herbarium.bgbm.org/object/B100039428",
                "@type" : "ods:Identifier",
                "dcterms:title" : "abcd:unitGUID",
                "dcterms:type" : "URL",
                "dcterms:identifier" : "https://herbarium.bgbm.org/object/B100039428",
                "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvable"
              }, {
                "@id" : "https://herbarium.bgbm.org/object/B100039428",
                "@type" : "ods:Identifier",
                "dcterms:title" : "abcd:recordURI",
                "dcterms:type" : "URL",
                "dcterms:identifier" : "https://herbarium.bgbm.org/object/B100039428",
                "ods:gupriLevel" : "GloballyUniqueStablePersistentResolvable"
              } ],
              "ods:hasEvents" : [ {
                "@type" : "ods:Event",
                "dwc:eventDate" : "2006-08-22",
                "dwc:verbatimEventDate" : "2006-08-22",
                "dwc:habitat" : "Vegetación secundaria en regeneración.",
                "dwc:eventRemarks" : "Herbaria: B LAGU MHES MO",
                "ods:hasAgents" : [ {
                  "@type" : "schema:Person",
                  "schema:name" : "Monterrosa,J., Soto,A.",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "collector"
                  } ]
                }, {
                  "@type" : "schema:Person",
                  "schema:name" : "Rivera,A.M.",
                  "ods:hasRoles" : [ {
                    "@type" : "schema:Role",
                    "schema:roleName" : "collector"
                  } ]
                } ],
                "ods:hasLocation" : {
                  "@type" : "ods:Location",
                  "dwc:continent" : "Middle and South America",
                  "dwc:country" : "El Salvador",
                  "dwc:countryCode" : "SV",
                  "dwc:locality" : "Depto. Cabañas, Cinquera, Ruta El Obrajón-El Tule",
                  "ods:hasGeoreference" : {
                    "@type" : "ods:Georeference",
                    "dwc:decimalLatitude" : 13.88333,
                    "dwc:decimalLongitude" : -88.93333
                  }
                }
              } ]
            }
            """
    );
  }

}
