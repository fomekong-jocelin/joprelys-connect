package com.joprelys.backend.ai.medication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RxNormMedicationKnowledgeProviderTest {

    @Test
    void shouldResolveCanonicalDrugIngredientsAndAtcClasses() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test/REST");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationKnowledgeProvider provider = new RxNormMedicationKnowledgeProvider(
                builder.build(), Duration.ofMinutes(30));

        server.expect(requestTo("https://rxnav.test/REST/rxcui.json?name=Augmentin&search=2"))
                .andRespond(withSuccess("""
                        {"idGroup":{"rxnormId":["1001"]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://rxnav.test/REST/rxcui/1001/properties.json"))
                .andRespond(withSuccess("""
                        {"properties":{"rxcui":"1001","name":"amoxicillin / clavulanate product","tty":"SBD"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://rxnav.test/REST/rxcui/1001/related.json?tty=IN%20PIN%20MIN"))
                .andRespond(withSuccess("""
                        {
                          "relatedGroup": {
                            "conceptGroup": [
                              {
                                "tty":"IN",
                                "conceptProperties":[
                                  {"rxcui":"723","name":"amoxicillin","tty":"IN"},
                                  {"rxcui":"19711","name":"clavulanate","tty":"IN"}
                                ]
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://rxnav.test/REST/rxclass/class/byRxcui.json?rxcui=1001&relaSource=ATC"))
                .andRespond(withSuccess("""
                        {
                          "rxclassDrugInfoList": {
                            "rxclassDrugInfo": [
                              {
                                "relaSource":"ATC",
                                "rela":"",
                                "rxclassMinConceptItem": {
                                  "classId":"J01CR02",
                                  "className":"amoxicillin and beta-lactamase inhibitor",
                                  "classType":"ATC5"
                                }
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var resolved = provider.resolve("Augmentin").orElseThrow();

        assertEquals("RXNORM", resolved.source());
        assertEquals("1001", resolved.conceptId());
        assertEquals("amoxicillin / clavulanate product", resolved.canonicalName());
        assertEquals(2, resolved.ingredients().size());
        assertEquals("723", resolved.ingredients().getFirst().conceptId());
        assertEquals("J01CR02", resolved.classes().getFirst().classId());
        server.verify();
    }

    @Test
    void shouldCacheResolvedMedication() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test/REST");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationKnowledgeProvider provider = new RxNormMedicationKnowledgeProvider(
                builder.build(), Duration.ofMinutes(30));

        server.expect(requestTo("https://rxnav.test/REST/rxcui.json?name=Amoxicilline&search=2"))
                .andRespond(withSuccess("""
                        {"idGroup":{"rxnormId":["723"]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://rxnav.test/REST/rxcui/723/properties.json"))
                .andRespond(withSuccess("""
                        {"properties":{"rxcui":"723","name":"amoxicillin","tty":"IN"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://rxnav.test/REST/rxcui/723/related.json?tty=IN%20PIN%20MIN"))
                .andRespond(withSuccess("""
                        {"relatedGroup":{"conceptGroup":[]}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://rxnav.test/REST/rxclass/class/byRxcui.json?rxcui=723&relaSource=ATC"))
                .andRespond(withSuccess("""
                        {"rxclassDrugInfoList":{"rxclassDrugInfo":[]}}
                        """, MediaType.APPLICATION_JSON));

        assertTrue(provider.resolve("Amoxicilline").isPresent());
        assertTrue(provider.resolve("amoxicilline").isPresent());
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenStrictOrNormalizedSearchFindsNothing() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test/REST");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationKnowledgeProvider provider = new RxNormMedicationKnowledgeProvider(
                builder.build(), Duration.ofMinutes(30));

        server.expect(requestTo("https://rxnav.test/REST/rxcui.json?name=MarqueLocale&search=2"))
                .andRespond(withSuccess("""
                        {"idGroup":{}}
                        """, MediaType.APPLICATION_JSON));

        assertTrue(provider.resolve("MarqueLocale").isEmpty());
        server.verify();
    }

    @Test
    void shouldExposeReferentialFailureAsDedicatedException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://rxnav.test/REST");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RxNormMedicationKnowledgeProvider provider = new RxNormMedicationKnowledgeProvider(
                builder.build(), Duration.ofMinutes(30));

        server.expect(requestTo("https://rxnav.test/REST/rxcui.json?name=Amoxicilline&search=2"))
                .andRespond(withServerError());

        assertThrows(
                MedicationKnowledgeUnavailableException.class,
                () -> provider.resolve("Amoxicilline"));
        server.verify();
    }
}
