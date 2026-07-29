package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiChatResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class FinalClinicalReviewServiceTest {

    private final UUID visitId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    @Test
    void shouldReturnProposalOnlyAndApplyPatchAfterExplicitAcceptance() {
        Map<String, String> draft = Map.of(
                "symptoms", "Fièvre 39 C sans vomissements");
        FinalClinicalReviewService service = serviceWithResponse("""
                {
                  "changes": [{
                    "field": "symptoms",
                    "operation": "SET",
                    "value": "Fièvre sans vomissements 39 C",
                    "reason": "Réorganisation sans changement clinique",
                    "uncertainty": "LOW",
                    "evidence": ["Fièvre 39 C sans vomissements"]
                  }],
                  "assistantMessage": "Une amélioration de forme est proposée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var review = service.createReview(
                visitId, userId, organizationId, draft, "fr");

        assertEquals("PENDING", review.status());
        assertEquals(1, review.revision().proposals().size());
        assertTrue(review.acceptedPatch().isEmpty());

        var proposal = review.revision().proposals().getFirst();
        var decided = service.decideProposal(
                visitId,
                userId,
                organizationId,
                review.reviewId(),
                proposal.id(),
                "ACCEPT",
                draft);

        assertEquals("DECIDED", decided.status());
        assertEquals("Fièvre sans vomissements 39 C", decided.acceptedPatch().get("symptoms"));
    }

    @Test
    void shouldRecoverTranscriptGroundedPrescriptionAndLabOrderMissedByFastDraft() {
        Map<String, String> draft = Map.of(
                "symptoms", "Céphalée aiguë depuis trois jours");
        String transcript = """
                Céphalée aiguë depuis trois jours.
                Je prescris du Paracétamol 1000mg par voie orale.
                Examen goutte d'epaisse.
                """;
        FinalClinicalReviewService service = serviceWithResponse("""
                {
                  "changes": [
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\",\"route\":\"voie orale\"}]",
                      "reason": "Prescription explicitement présente dans le transcript mais absente du brouillon.",
                      "uncertainty": "LOW",
                      "evidence": ["Paracétamol 1000mg", "voie orale"]
                    },
                    {
                      "field": "labOrders",
                      "operation": "SET",
                      "value": "[\"goutte épaisse\"]",
                      "reason": "Examen explicitement demandé dans le transcript.",
                      "uncertainty": "LOW",
                      "evidence": ["Examen goutte d'epaisse"]
                    }
                  ],
                  "assistantMessage": "Deux omissions explicites peuvent être récupérées.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var review = service.createReview(
                visitId,
                userId,
                organizationId,
                draft,
                transcript,
                "fr");

        assertEquals("PENDING", review.status());
        assertEquals(2, review.revision().proposals().size());
        assertTrue(review.revision().proposals().stream()
                .anyMatch(proposal -> "prescription".equals(proposal.field())
                        && proposal.proposedValue().contains("Paracétamol")));
        assertTrue(review.revision().proposals().stream()
                .anyMatch(proposal -> "labOrders".equals(proposal.field())
                        && proposal.proposedValue().contains("goutte épaisse")));

        var decided = service.decideReview(
                visitId,
                userId,
                organizationId,
                review.reviewId(),
                "ACCEPT",
                draft);

        assertEquals("DECIDED", decided.status());
        assertTrue(decided.acceptedPatch().get("prescription").contains("Paracétamol"));
        assertTrue(decided.acceptedPatch().get("labOrders").contains("goutte épaisse"));
    }

    @Test
    void shouldRejectStaleReviewWhenDraftChangedBeforeDecision() {
        Map<String, String> draft = Map.of(
                "symptoms", "Fièvre 39 C sans vomissements");
        FinalClinicalReviewService service = serviceWithResponse("""
                {
                  "changes": [{
                    "field": "symptoms",
                    "operation": "SET",
                    "value": "Fièvre sans vomissements 39 C",
                    "reason": "Réorganisation sans changement clinique",
                    "uncertainty": "LOW",
                    "evidence": ["Fièvre 39 C sans vomissements"]
                  }],
                  "assistantMessage": "Une amélioration de forme est proposée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);
        var review = service.createReview(
                visitId, userId, organizationId, draft, "fr");
        var proposal = review.revision().proposals().getFirst();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.decideProposal(
                        visitId,
                        userId,
                        organizationId,
                        review.reviewId(),
                        proposal.id(),
                        "ACCEPT",
                        Map.of("symptoms", "Fièvre 40 C sans vomissements")));

        assertEquals("AI_FINAL_REVIEW_STALE", error.getReason());
    }

    @Test
    void shouldFailClosedWhenTerraInventsUnsupportedDiagnosis() {
        Map<String, String> draft = Map.of(
                "symptoms", "Fièvre 39 C sans vomissements");
        FinalClinicalReviewService service = serviceWithResponse("""
                {
                  "changes": [{
                    "field": "diagnosis",
                    "operation": "SET",
                    "value": "Paludisme confirmé",
                    "reason": "Diagnostic proposé",
                    "uncertainty": "LOW",
                    "evidence": ["Fièvre 39 C sans vomissements"]
                  }],
                  "assistantMessage": "Diagnostic proposé.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var review = service.createReview(
                visitId, userId, organizationId, draft, "fr");

        assertEquals("NO_CHANGES", review.status());
        assertTrue(review.revision().proposals().isEmpty());
        assertTrue(review.acceptedPatch().isEmpty());
    }

    @Test
    void shouldFailClosedWhenFinalReviewInventsMedicationNotPresentInEvidence() {
        Map<String, String> draft = Map.of(
                "prescription", "[{\"drugName\":\"Paracétamol\",\"dosage\":\"500 mg\"}]");
        FinalClinicalReviewService service = serviceWithResponse("""
                {
                  "changes": [{
                    "field": "prescription",
                    "operation": "SET",
                    "value": "[{\"drugName\":\"Amoxicilline\",\"dosage\":\"500 mg\"}]",
                    "reason": "Substitution proposée",
                    "uncertainty": "LOW",
                    "evidence": ["Paracétamol"]
                  }],
                  "assistantMessage": "Substitution proposée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var review = service.createReview(
                visitId, userId, organizationId, draft, "fr");

        assertEquals("NO_CHANGES", review.status());
        assertTrue(review.revision().proposals().isEmpty());
    }

    @Test
    void shouldNeverProposeVitalsAndShouldDropUnsupportedLabAddition() {
        Map<String, String> draft = Map.of(
                "vitals", "{\"temperature\":39.0}",
                "labOrders", "[\"NFS\"]");
        FinalClinicalReviewService service = serviceWithResponse("""
                {
                  "changes": [
                    {
                      "field": "vitals",
                      "operation": "SET",
                      "value": "{\"temperature\":37.0}",
                      "reason": "Normalisation",
                      "uncertainty": "LOW",
                      "evidence": ["39.0"]
                    },
                    {
                      "field": "labOrders",
                      "operation": "SET",
                      "value": "[\"NFS\",\"CRP\"]",
                      "reason": "Ajout proposé",
                      "uncertainty": "LOW",
                      "evidence": ["NFS"]
                    }
                  ],
                  "assistantMessage": "Modifications proposées.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var review = service.createReview(
                visitId, userId, organizationId, draft, "fr");

        assertEquals("NO_CHANGES", review.status());
        assertTrue(review.revision().proposals().isEmpty());
    }

    private FinalClinicalReviewService serviceWithResponse(String content) {
        FinalClinicalReviewGateway gateway = (draft, transcript, locale) ->
                new AiChatResponse(content, 321, "gpt-5.6-terra");
        ObjectMapper objectMapper = new ObjectMapper();
        ClinicalContextAssembler contextAssembler = mock(ClinicalContextAssembler.class);
        when(contextAssembler.assemble(visitId)).thenReturn(Map.of());
        return new FinalClinicalReviewService(
                gateway,
                new AiClinicalResponseParser(objectMapper),
                contextAssembler,
                objectMapper);
    }
}
