package com.joprelys.backend.medication.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RxNormMedicationReferencePortTest {

    @Test
    void shouldResolveNormalizedConceptAndProperties() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationReferencePort port = new RxNormMedicationReferencePort(builder.build());

        server.expect(requestTo(
                        "https://rxnav.test/REST/Prescribe/rxcui.json?name=Amoxicilline&search=2"))
                .andRespond(withSuccess("""
                        {"idGroup":{"name":"Amoxicilline","rxnormId":["723"]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://rxnav.test/REST/rxcui/723/properties.json"))
                .andRespond(withSuccess("""
                        {"properties":{
                          "rxcui":"723",
                          "name":"amoxicillin",
                          "synonym":"Amoxicillin",
                          "tty":"IN",
                          "language":"ENG",
                          "suppress":"N"
                        }}
                        """, MediaType.APPLICATION_JSON));

        List<MedicationConcept> concepts = port.findCandidates("Amoxicilline", 5);

        assertEquals(1, concepts.size());
        MedicationConcept concept = concepts.getFirst();
        assertEquals("RXNORM", concept.source());
        assertEquals("723", concept.conceptId());
        assertEquals("amoxicillin", concept.canonicalName());
        assertEquals("IN", concept.termType());
        assertTrue(concept.active());
        assertEquals(Set.of("723"), port.findIngredientConceptIds(concept));
        server.verify();
    }

    @Test
    void shouldResolveIngredientIdsForBrandedOrClinicalDrugConcept() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationReferencePort port = new RxNormMedicationReferencePort(builder.build());
        MedicationConcept branded = new MedicationConcept(
                "RXNORM",
                "209459",
                "acetaminophen 500 MG Oral Tablet [Tylenol]",
                "SBD",
                List.of("Tylenol 500 MG Oral Tablet"),
                true);

        server.expect(requestTo(
                        "https://rxnav.test/REST/rxcui/209459/related.json?tty=IN%20PIN%20MIN"))
                .andRespond(withSuccess("""
                        {
                          "relatedGroup": {
                            "rxcui": null,
                            "conceptGroup": [
                              {
                                "tty": "IN",
                                "conceptProperties": [
                                  {"rxcui":"161","name":"acetaminophen","tty":"IN","suppress":"N"}
                                ]
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        Set<String> ingredientIds = port.findIngredientConceptIds(branded);

        assertEquals(Set.of("161"), ingredientIds);
        server.verify();
    }

    @Test
    void shouldReturnEmptyListWhenRxNormDoesNotResolveTheName() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationReferencePort port = new RxNormMedicationReferencePort(builder.build());

        server.expect(requestTo(
                        "https://rxnav.test/REST/Prescribe/rxcui.json?name=ProduitInconnu&search=2"))
                .andRespond(withSuccess("{" + "\"idGroup\":{\"name\":\"ProduitInconnu\"}}",
                        MediaType.APPLICATION_JSON));

        List<MedicationConcept> concepts = port.findCandidates("ProduitInconnu", 5);

        assertTrue(concepts.isEmpty());
        server.verify();
    }
}
