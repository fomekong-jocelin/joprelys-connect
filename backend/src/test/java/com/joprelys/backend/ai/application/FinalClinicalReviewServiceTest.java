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
    void shouldRejectMedicationSubstitutionNotPresentInAcceptedDraft() {
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

    private FinalClinicalReviewService serviceWithResponse(String content) {
        FinalClinicalReviewGateway gateway = (draft, locale) ->
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
