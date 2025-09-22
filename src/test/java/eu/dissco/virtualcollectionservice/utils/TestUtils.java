package eu.dissco.virtualcollectionservice.utils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionAction;
import eu.dissco.virtualcollectionservice.domain.VirtualCollectionEvent;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter;
import eu.dissco.virtualcollectionservice.schema.TargetDigitalObjectFilter.OdsPredicateType;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection.LtcBasisOfScheme;
import eu.dissco.virtualcollectionservice.schema.VirtualCollection.OdsStatus;

public class TestUtils {

  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final String VC_ID = "https://hdl.handle.net/TEST/XXX-XXX-XXX";
  public static final String SPECIMEN_ID = "https://doi.org/TEST/ZZZ-X4T-YYV";

  public static VirtualCollectionEvent givenVirtualCollectionEvent() {
    return new VirtualCollectionEvent(
        VirtualCollectionAction.CREATE,
        givenVirtualCollection()
    );
  }

  public static VirtualCollection givenVirtualCollection() {
    return new VirtualCollection()
        .withId(VC_ID)
        .withDctermsIdentifier(VC_ID)
        .withType("VirtualCollection")
        .withLtcCollectionName("A test collection")
        .withLtcBasisOfScheme(LtcBasisOfScheme.REFERENCE_COLLECTION)
        .withOdsStatus(OdsStatus.ACTIVE)
        .withSchemaVersion(1)
        .withOdsHasTargetDigitalObjectFilter(givenTargetDigitalObjectFilter());
  }

  private static TargetDigitalObjectFilter givenTargetDigitalObjectFilter() {
    return new TargetDigitalObjectFilter()
        .withOdsPredicateType(OdsPredicateType.EQUALS)
        .withOdsPredicateKey("dcterms:identifier")
        .withOdsPredicateValue("urn:catalog:12345");
  }

  public static Query givenElasticQuery() {
    return new Query.Builder().bool(b -> b.must(new Query.Builder().term(
        t -> t.field("@id.keyword")
            .value(FieldValue.of(SPECIMEN_ID))
            .caseInsensitive(true)).build())).build();
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
