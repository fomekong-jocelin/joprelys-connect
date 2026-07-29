package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    void rebuildMustUseEveryRecoverableTranscriptSegmentInOneBoundedCallWhenSmall() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        SessionView session = mock(SessionView.class);
        when(session.draft()).thenReturn(Map.of());
        when(intakeService.listActive(visitId, organizationId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(List.of(
                        entry(visitId, 1, "Le patient présente des céphalées sévères."),
                        entry(visitId, 2, "Il n'a pas de fièvre."),
                        entry(visitId, 3, "Je prescris du paracétamol 1000 mg matin et soir pendant 4 jours.")));
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
                transcript.capture());
        org.assertj.core.api.Assertions.assertThat(transcript.getValue())
                .containsSubsequence(
                        "Le patient présente des céphalées sévères.",
                        "Il n'a pas de fièvre.",
                        "Je prescris du paracétamol 1000 mg matin et soir pendant 4 jours.");
        verify(intakeService).markAnalyzed(
                visitId, organizationId, RealtimeIntakeSource.CONSULTATION);
    }

    @Test
    void longTranscriptMustStayBoundedInsteadOfRetryingEverySentence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        SessionView session = mock(SessionView.class);
        when(session.draft()).thenReturn(Map.of());
        String longTranscript = "céphalée persistante. ".repeat(2_400);
        when(intakeService.listActive(visitId, organizationId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(List.of(entry(visitId, 1, longTranscript)));
        when(consultationService.getSession(visitId, userId, organizationId))
                .thenReturn(Optional.of(session));

        service.rebuild(visitId, userId, organizationId, Map.of(), "fr");

        ArgumentCaptor<String> chunks = ArgumentCaptor.forClass(String.class);
        verify(consultationService, times(2)).processCaptureTranscript(
                eq(visitId),
                eq(userId),
                eq(organizationId),
                chunks.capture());
        org.assertj.core.api.Assertions.assertThat(chunks.getAllValues())
                .allSatisfy(chunk -> org.assertj.core.api.Assertions.assertThat(chunk.length()).isLessThanOrEqualTo(30_000));
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

    private IntakeView entry(UUID visitId, long sequence, String transcript) {
        return new IntakeView(
                UUID.randomUUID(),
                visitId,
                RealtimeIntakeSource.CONSULTATION,
                sequence,
                "event-" + sequence,
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
