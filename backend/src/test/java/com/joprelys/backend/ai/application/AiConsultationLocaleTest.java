package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiConsultationLocaleTest {

    @Test
    void shouldUseRequestedEnglishLocaleForGreetingAndTranscription() {
        AiProvider provider = mock(AiProvider.class);
        VisitService visitService = mock(VisitService.class);
        ClinicalContextAssembler contextAssembler = mock(ClinicalContextAssembler.class);
        VisitEntity visit = mock(VisitEntity.class);
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(contextAssembler.assemble(visitId)).thenReturn(Map.of());
        when(provider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("en")))
                .thenReturn(new AiTranscription("Pain on the right side", "en", null));

        AiProperties properties = new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                "fr",
                null,
                null,
                null);
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

        var session = service.startSession(
                visitId,
                userId,
                organizationId,
                Map.of(),
                "en");

        assertEquals("Hello doctor. I’m listening.", session.assistantMessage());
        assertEquals("en", service.sessionLocale(visitId, userId, organizationId));

        var transcription = service.transcribeAudio(
                visitId,
                userId,
                organizationId,
                new byte[] {1, 2, 3},
                "audio/webm");

        assertEquals("Pain on the right side", transcription.transcript());
        verify(provider).transcribeAudio(any(byte[].class), eq("audio/webm"), eq("en"));
    }
}
