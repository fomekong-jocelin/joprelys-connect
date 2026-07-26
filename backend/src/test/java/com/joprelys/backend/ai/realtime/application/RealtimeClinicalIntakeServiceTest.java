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
    void shouldPersistVerifiedTurnWithNextVisitSequence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        when(repository.findByVisitIdAndEventId(visitId, "event-1")).thenReturn(Optional.empty());
        when(repository.findByVisitIdAndItemId(visitId, "item-1")).thenReturn(Optional.empty());
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

        assertThat(result.sequence()).isEqualTo(8);
        assertThat(result.transcript()).isEqualTo("Patient sans fièvre");
        assertThat(result.eventId()).isEqualTo("event-1");
        assertThat(result.itemId()).isEqualTo("item-1");
    }

    @Test
    void shouldReturnExistingRowForIdenticalEventRetryWithoutCreatingAnotherSequence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId,
                visitId,
                "event-2",
                "item-2",
                "Tension 120 sur 80",
                0.93,
                userId);
        when(repository.findByVisitIdAndEventId(visitId, "event-2"))
                .thenReturn(Optional.of(existing));

        var result = service.ingest(
                visitId,
                userId,
                organizationId,
                "event-2",
                "item-2",
                "Tension 120 sur 80",
                0.93);

        assertThat(result.eventId()).isEqualTo("event-2");
        verify(repository, never()).findMaximumSequence(visitId);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldDeduplicateByOpenAiItemIdEvenWhenEventIdChanges() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId,
                visitId,
                "event-original",
                "item-stable",
                "Douleur depuis trois jours",
                0.9,
                userId);
        when(repository.findByVisitIdAndEventId(visitId, "event-replayed"))
                .thenReturn(Optional.empty());
        when(repository.findByVisitIdAndItemId(visitId, "item-stable"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.ingest(
                visitId,
                userId,
                organizationId,
                "event-replayed",
                "item-stable",
                "Douleur depuis trois jours",
                0.9))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_REALTIME_INTAKE_ID_REUSED");
    }

    @Test
    void shouldRejectSameEventIdWithDifferentTranscript() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity existing = entity(
                organizationId,
                visitId,
                "event-3",
                "item-3",
                "Pouls 72",
                0.92,
                userId);
        when(repository.findByVisitIdAndEventId(visitId, "event-3"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.ingest(
                visitId,
                userId,
                organizationId,
                "event-3",
                "item-3",
                "Pouls 120",
                0.92))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_REALTIME_INTAKE_ID_REUSED");
    }

    @Test
    void shouldRejectLowConfidenceBeforePersistence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        assertThatThrownBy(() -> service.ingest(
                visitId,
                userId,
                organizationId,
                "event-low",
                "item-low",
                "texte incertain",
                0.2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_TRANSCRIPTION_LOW_CONFIDENCE");

        verify(visitRepository, never()).findByIdForUpdate(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectInactiveVisit() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visit.getStatus()).thenReturn("TERMINE");
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.ingest(
                visitId,
                UUID.randomUUID(),
                organizationId,
                "event-closed",
                "item-closed",
                "Patient stable",
                0.9))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("VISIT_NOT_ACTIVE");
    }

    private void prepareVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
    }

    private RealtimeClinicalIntakeEntity entity(
            UUID organizationId,
            UUID visitId,
            String eventId,
            String itemId,
            String text,
            double confidence,
            UUID userId) {
        return new RealtimeClinicalIntakeEntity(
                organizationId,
                visitId,
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
                        "gpt-4.1",
                        "gpt-4o-mini-transcribe",
                        "medical",
                        0.8,
                        "https://api.openai.com/v1"),
                null,
                null);
    }
}
