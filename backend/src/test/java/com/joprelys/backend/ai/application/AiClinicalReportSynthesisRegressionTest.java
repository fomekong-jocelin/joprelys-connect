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

class AiClinicalReportSynthesisRegressionTest {

    @Test
    void mobileTranscriptMustProduceLosslessPartialReportWithoutInventedDiagnosis() {
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
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Patient âgé de 12 ans présentant une céphalée aiguë depuis trois jours. Il n'arrive plus à se lever.",
                      "reason": "Symptômes et histoire explicitement dictés.",
                      "uncertainty": "LOW",
                      "evidence": [
                        "patient âgé de 12 ans qui se plaint d'une céphalée aiguë depuis trois jours",
                        "Il n'arrive plus à se lever"
                      ]
                    },
                    {
                      "field": "advice",
                      "operation": "SET",
                      "value": "Boire beaucoup et se reposer.",
                      "reason": "Conseils explicitement donnés.",
                      "uncertainty": "LOW",
                      "evidence": ["je lui demande également de beaucoup boire et de se reposer"]
                    },
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{
                        "drugName": "Paracétamol",
                        "dosage": "1000 mg",
                        "posology": "2 comprimé par prise",
                        "frequency": "matin midi soir",
                        "route": "voie orale"
                      }],
                      "reason": "Ligne médicamenteuse suffisamment explicite.",
                      "uncertainty": "LOW",
                      "evidence": [
                        "Paracétamol 1000mg",
                        "matin midi soir",
                        "2 comprimé par prise",
                        "voie orale"
                      ]
                    },
                    {
                      "field": "labOrders",
                      "operation": "SET",
                      "value": ["Goutte épaisse"],
                      "reason": "Examen explicitement demandé.",
                      "uncertainty": "LOW",
                      "evidence": ["Examen goutte d'epaisse"]
                    }
                  ],
                  "assistantMessage": "Un point de prescription reste à préciser.",
                  "needsClarification": true,
                  "clarification": {
                    "field": "prescription",
                    "question": "Pouvez-vous préciser la fréquence de Vitafer ?",
                    "options": []
                  }
                }
                """, 100, "test-model"));

        AiProperties properties = properties();
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
        service.processCaptureTranscript(
                visitId,
                userId,
                organizationId,
                """
                Je reçois aujourd'hui un patient âgé de 12 ans qui se plaint d'une céphalée aiguë depuis trois jours. Il n'arrive plus à se lever.
                Quatre jours, je lui demande également de beaucoup boire et de se reposer. Je lui prescris du Paracétamol 1000mg a prendre matin midi soir 2 comprimé par prise par voie orale Vitafer 1cuillerr a café 1 fois pas jour voie orale Examen goutte d'epaisse
                """,
                null,
                "DICTATION");

        var session = service.getSession(visitId, userId, organizationId).orElseThrow();

        assertThat(session.draft().get("symptoms"))
                .contains("12 ans")
                .contains("céphalée aiguë")
                .contains("trois jours")
                .contains("Il n'arrive plus à se lever");
        assertThat(session.draft().get("advice"))
                .contains("Boire beaucoup")
                .contains("se reposer");
        assertThat(session.draft().get("prescription"))
                .contains("Paracétamol")
                .contains("1000 mg")
                .contains("matin midi soir")
                .contains("2 comprimé par prise")
                .doesNotContain("Vitafer");
        assertThat(session.draft().get("labOrders")).contains("Goutte épaisse");
        assertThat(session.draft()).doesNotContainKeys("diagnosis", "finalDiagnosis");
    }

    private AiProperties properties() {
        return new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                0.35,
                "fr",
                null,
                null,
                null);
    }
}
