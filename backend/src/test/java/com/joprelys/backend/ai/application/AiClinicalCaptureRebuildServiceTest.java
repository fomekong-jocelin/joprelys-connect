package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class AiClinicalCaptureRebuildServiceTest {

    private final RealtimeClinicalIntakeService intakeService = mock(RealtimeClinicalIntakeService.class);
    private final AiConsultationService consultationService = mock(AiConsultationService.class);
    private final AiClinicalCaptureRebuildService service = new AiClinicalCaptureRebuildService(
            intakeService,
            consultationService);

    @Test
    void rebuildMustUseEveryRecoverableTranscriptSegmentInOrder() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        SessionView session = mock(SessionView.class);
        when(intakeService.listActive(visitId, organizationId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(List.of(
                        realtimeEntry(visitId, 1, "Le patient présente des céphalées sévères."),
                        realtimeEntry(visitId, 2, "Il n'a pas de fièvre."),
                        realtimeEntry(visitId, 3, "Je prescris du paracétamol 1000 mg matin et soir pendant 4 jours.")));
        when(consultationService.getSession(visitId, userId, organizationId))
                .thenReturn(Optional.of(session));

        service.rebuild(
                visitId,
                userId,
                organizationId,
                Map.of("clinicalExam", "Conscience normale"),
                "fr");

        verify(consultationService).startSession(
                visitId,
                userId,
                organizationId,
                Map.of("clinicalExam", "Conscience normale"),
                "fr");
        ArgumentCaptor<String> transcript = ArgumentCaptor.forClass(String.class);
        verify(consultationService).processCaptureTranscript(
                eq(visitId),
                eq(userId),
                eq(organizationId),
                transcript.capture(),
                eq(null),
                eq("REALTIME"));
        org.assertj.core.api.Assertions.assertThat(transcript.getValue())
                .containsSubsequence(
                        "Le patient présente des céphalées sévères.",
                        "Il n'a pas de fièvre.",
                        "Je prescris du paracétamol 1000 mg matin et soir pendant 4 jours.");
        verify(intakeService).markAnalyzed(
                visitId, organizationId, RealtimeIntakeSource.CONSULTATION);
    }

    @Test
    void dictationCaptureMustReachModelAsTrustedDictation() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        SessionView session = mock(SessionView.class);
        when(intakeService.listActive(visitId, organizationId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(List.of(dictationEntry(
                        visitId,
                        1,
                        "Paracétamol 1000mg matin midi soir 2 comprimés par prise par voie orale.")));
        when(consultationService.getSession(visitId, userId, organizationId))
                .thenReturn(Optional.of(session));

        service.rebuild(visitId, userId, organizationId, Map.of(), "fr");

        verify(consultationService).processCaptureTranscript(
                eq(visitId),
                eq(userId),
                eq(organizationId),
                eq("Paracétamol 1000mg matin midi soir 2 comprimés par prise par voie orale."),
                eq(null),
                eq("DICTATION"));
    }

    @Test
    void rebuildMustFailWithoutErasingAnythingWhenCaptureIsEmpty() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(intakeService.listActive(visitId, organizationId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.rebuild(
                visitId,
                UUID.randomUUID(),
                organizationId,
                Map.of(),
                "fr"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CAPTURE_EMPTY");
    }

    private IntakeView realtimeEntry(UUID visitId, long sequence, String transcript) {
        return entry(visitId, sequence, "event-" + sequence, transcript);
    }

    private IntakeView dictationEntry(UUID visitId, long sequence, String transcript) {
        return entry(visitId, sequence, "dictation:" + sequence, transcript);
    }

    private IntakeView entry(UUID visitId, long sequence, String eventId, String transcript) {
        return new IntakeView(
                UUID.randomUUID(),
                visitId,
                RealtimeIntakeSource.CONSULTATION,
                sequence,
                eventId,
                "item-" + sequence,
                transcript,
                null,
                0.95,
                false,
                0,
                null,
                "PENDING",
                Instant.parse("2026-07-29T01:00:00Z").plusSeconds(sequence));
    }
}
