package com.joprelys.backend.ai.realtime.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeEntity;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RealtimeClinicalIntakeServiceTest {

    private final RealtimeClinicalIntakeRepository repository = mock(RealtimeClinicalIntakeRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final RealtimeClinicalIntakeService service = new RealtimeClinicalIntakeService(
            repository,
            visitRepository,
            properties());

    @Test
    void shouldPersistVerifiedConsultationTurnWithNextVisitSequence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        expectMissing(visitId, RealtimeIntakeSource.CONSULTATION, "event-1", "item-1");
        when(repository.findMaximumSequence(visitId)).thenReturn(7L);
        when(repository.saveAndFlush(any(RealtimeClinicalIntakeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.ingest(
                visitId,
                userId,
                organizationId,
                "event-1",
                "item-1",
                " Patient sans fièvre ",
                0.91);

        assertThat(result.source()).isEqualTo(RealtimeIntakeSource.CONSULTATION);
        assertThat(result.sequence()).isEqualTo(8);
        assertThat(result.transcript()).isEqualTo("Patient sans fièvre");
        assertThat(result.reviewRequired()).isFalse();
        assertThat(result.captureStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldPersistVitalsTurnInAnIsolatedSourceNamespace() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        expectMissing(visitId, RealtimeIntakeSource.VITALS, "event-shared", "item-shared");
        when(repository.findMaximumSequence(visitId)).thenReturn(3L);
        when(repository.saveAndFlush(any(RealtimeClinicalIntakeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.ingest(
                visitId,
                userId,
                organizationId,
                RealtimeIntakeSource.VITALS,
                "event-shared",
                "item-shared",
                "Saturation 96",
                0.88);

        assertThat(result.source()).isEqualTo(RealtimeIntakeSource.VITALS);
        assertThat(result.sequence()).isEqualTo(4);
        verify(repository).findByVisitIdAndSourceAndEventId(
                visitId, RealtimeIntakeSource.VITALS, "event-shared");
        verify(repository, never()).findByVisitIdAndSourceAndEventId(
                visitId, RealtimeIntakeSource.CONSULTATION, "event-shared");
    }

    @Test
    void shouldReturnExistingRowForIdenticalEventRetryWithoutCreatingAnotherSequence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-2", "item-2", "Tension 120 sur 80", 0.93, userId);
        when(repository.findByVisitIdAndSourceAndEventId(
                visitId, RealtimeIntakeSource.CONSULTATION, "event-2"))
                .thenReturn(Optional.of(existing));

        var result = service.ingest(
                visitId, userId, organizationId,
                "event-2", "item-2", "Tension 120 sur 80", 0.93);

        assertThat(result.eventId()).isEqualTo("event-2");
        verify(repository, never()).findMaximumSequence(visitId);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldDeduplicateByOpenAiItemIdWithinSameSourceEvenWhenEventIdChanges() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-original", "item-stable", "Douleur depuis trois jours", 0.9, userId);
        when(repository.findByVisitIdAndSourceAndEventId(
                visitId, RealtimeIntakeSource.CONSULTATION, "event-replayed"))
                .thenReturn(Optional.empty());
        when(repository.findByVisitIdAndSourceAndItemId(
                visitId, RealtimeIntakeSource.CONSULTATION, "item-stable"))
                .thenReturn(Optional.of(existing));

        var result = service.ingest(
                visitId, userId, organizationId,
                "event-replayed", "item-stable", "Douleur depuis trois jours", 0.9);

        assertThat(result.eventId()).isEqualTo("event-original");
        assertThat(result.itemId()).isEqualTo("item-stable");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectStableItemIdWhenItsOriginalTranscriptChanges() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-original", "item-stable", "Douleur depuis trois jours", 0.9, userId);
        when(repository.findByVisitIdAndSourceAndEventId(
                visitId, RealtimeIntakeSource.CONSULTATION, "event-replayed"))
                .thenReturn(Optional.empty());
        when(repository.findByVisitIdAndSourceAndItemId(
                visitId, RealtimeIntakeSource.CONSULTATION, "item-stable"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.ingest(
                visitId, userId, organizationId,
                "event-replayed", "item-stable", "Douleur depuis cinq jours", 0.9))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_REALTIME_INTAKE_ID_REUSED");
    }

    @Test
    void shouldPersistLowConfidenceInsteadOfDestroyingNonEmptyTranscript() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        expectMissing(visitId, RealtimeIntakeSource.CONSULTATION, "event-low", "item-low");
        when(repository.findMaximumSequence(visitId)).thenReturn(0L);
        when(repository.saveAndFlush(any(RealtimeClinicalIntakeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.ingest(
                visitId, userId, organizationId,
                "event-low", "item-low", "texte incertain", 0.2);

        assertThat(result.transcript()).isEqualTo("texte incertain");
        assertThat(result.confidence()).isEqualTo(0.2);
        assertThat(result.reviewRequired()).isTrue();
        verify(repository).saveAndFlush(any());
    }

    @Test
    void shouldPersistMissingConfidenceAsReviewableTranscript() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        expectMissing(visitId, RealtimeIntakeSource.CONSULTATION, "event-null", "item-null");
        when(repository.findMaximumSequence(visitId)).thenReturn(0L);
        when(repository.saveAndFlush(any(RealtimeClinicalIntakeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.ingest(
                visitId, userId, organizationId,
                "event-null", "item-null", "négation de fièvre", null);

        assertThat(result.transcript()).isEqualTo("négation de fièvre");
        assertThat(result.confidence()).isZero();
        assertThat(result.reviewRequired()).isTrue();
    }

    @Test
    void shouldCorrectExactDurableSegmentAndPreserveOriginalText() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-correction", "item-correction", "Il a mal au bra.", 0.5, userId);
        when(repository.findByIdAndVisitIdAndSource(
                existing.getId(), visitId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(RealtimeClinicalIntakeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.correct(
                visitId, existing.getId(), userId, organizationId, "Il a mal au bras.");

        assertThat(result.transcript()).isEqualTo("Il a mal au bras.");
        assertThat(result.originalTranscript()).isEqualTo("Il a mal au bra.");
        assertThat(result.correctionCount()).isEqualTo(1);
        assertThat(result.captureStatus()).isEqualTo("PENDING");
        verify(repository).save(existing);
    }

    @Test
    void shouldReturnOnlyNonConsumedCaptureForRecovery() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareReadableVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity first = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-1", "item-1", "Première phrase", 0.9, userId);
        RealtimeClinicalIntakeEntity second = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-2", "item-2", "Deuxième phrase", 0.9, userId);
        when(repository.findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
                visitId, RealtimeIntakeSource.CONSULTATION, "CONSUMED"))
                .thenReturn(List.of(first, second));

        var result = service.listActive(
                visitId, organizationId, RealtimeIntakeSource.CONSULTATION);

        assertThat(result).extracting(RealtimeClinicalIntakeService.IntakeView::transcript)
                .containsExactly("Première phrase", "Deuxième phrase");
    }

    @Test
    void shouldMarkAllRecoverableCaptureConsumedOnlyWhenExplicitlyFinalized() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareReadableVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity first = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-1", "item-1", "Première phrase", 0.9, userId);
        RealtimeClinicalIntakeEntity second = entity(
                organizationId, visitId, RealtimeIntakeSource.CONSULTATION,
                "event-2", "item-2", "Deuxième phrase", 0.9, userId);
        when(repository.findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
                visitId, RealtimeIntakeSource.CONSULTATION, "CONSUMED"))
                .thenReturn(List.of(first, second));

        service.consume(visitId, organizationId, RealtimeIntakeSource.CONSULTATION);

        assertThat(first.getCaptureStatus()).isEqualTo("CONSUMED");
        assertThat(second.getCaptureStatus()).isEqualTo("CONSUMED");
        assertThat(first.getConsumedAt()).isNotNull();
        assertThat(second.getConsumedAt()).isNotNull();
        verify(repository).saveAll(List.of(first, second));
    }

    @Test
    void shouldRejectInactiveVisitForNewCapture() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visit.getStatus()).thenReturn("TERMINEE");
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.ingest(
                visitId, UUID.randomUUID(), organizationId,
                "event-closed", "item-closed", "Patient stable", 0.9))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("VISIT_NOT_ACTIVE");
    }

    private void expectMissing(
            UUID visitId,
            RealtimeIntakeSource source,
            String eventId,
            String itemId) {
        when(repository.findByVisitIdAndSourceAndEventId(visitId, source, eventId))
                .thenReturn(Optional.empty());
        when(repository.findByVisitIdAndSourceAndItemId(visitId, source, itemId))
                .thenReturn(Optional.empty());
    }

    private void prepareLockedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
    }

    private void prepareReadableVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
    }

    private RealtimeClinicalIntakeEntity entity(
            UUID organizationId,
            UUID visitId,
            RealtimeIntakeSource source,
            String eventId,
            String itemId,
            String text,
            double confidence,
            UUID userId) {
        return new RealtimeClinicalIntakeEntity(
                organizationId,
                visitId,
                source,
                1,
                eventId,
                itemId,
                text,
                confidence,
                userId);
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
                new AiProperties.OpenAiProperties(
                        "test-key",
                        "gpt-4o-mini",
                        "gpt-4o-mini-transcribe",
                        "medical",
                        0.8,
                        "https://api.openai.com/v1"),
                null,
                null);
    }
}
