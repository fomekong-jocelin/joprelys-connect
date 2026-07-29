package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiContinuousCaptureClarificationRegressionTest {

    @Test
    void safeFactsMustApplyWhileAmbiguousMedicationRemainsNonBlockingReviewMetadata() {
        AiProvider provider = mock(AiProvider.class);
        VisitService visitService = mock(VisitService.class);
        ClinicalContextAssembler contextAssembler = mock(ClinicalContextAssembler.class);
        VisitEntity visit = mock(VisitEntity.class);
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(contextAssembler.assemble(visitId)).thenReturn(Map.of(
                "patient", Map.of("allergies", ""),
                "activeMedications", List.of()));
        when(provider.chat(anyList(), anyString())).thenReturn(new AiChatResponse("""
                {
                  "changes": [{
                    "field":"symptoms",
                    "operation":"SET",
                    "value":"Céphalée aiguë depuis trois jours.",
                    "reason":"Symptôme explicitement dicté.",
                    "uncertainty":"LOW",
                    "evidence":["céphalée aiguë depuis trois jours"]
                  }],
                  "assistantMessage":"La fréquence de Vitafer reste à préciser.",
                  "needsClarification":true,
                  "clarification":{
                    "field":"prescription",
                    "question":"Pouvez-vous préciser la fréquence de Vitafer ?",
                    "options":[]
                  }
                }
                """, 80, "test-model"));

        AiProperties properties = new AiProperties(
                true, "openai", "openai", 30, 20, 0.35, "fr", null, null, null);
        ObjectMapper objectMapper = new ObjectMapper();
        AiConsultationService service = new AiConsultationService(
                provider,
                properties,
                visitService,
                objectMapper,
                new AiClinicalResponseParser(objectMapper),
                contextAssembler,
                new AiRevisionManager(),
                new AiClarificationManager(properties));

        service.startSession(visitId, userId, organizationId, Map.of(), "fr");
        var response = service.processCaptureTranscript(
                visitId,
                userId,
                organizationId,
                "Le patient présente une céphalée aiguë depuis trois jours. Je prescris Vitafer une cuillère une fois pas jour.",
                null,
                "DICTATION");

        assertThat(response.draft().get("symptoms")).contains("Céphalée aiguë depuis trois jours");
        assertThat(response.needsClarification()).isFalse();
        assertThat(response.clarifications()).hasSize(1);
        assertThat(response.clarifications().getFirst().field()).isEqualTo("prescription");
        assertThat(response.clarifications().getFirst().status()).isEqualTo("PENDING");
    }
}
