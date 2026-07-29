package com.joprelys.backend.ai.realtime.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeEntity;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RealtimeClinicalIntakeCorrectionReviewTest {

    @Test
    void clinicianCorrectionMustReplaceLowAsrConfidenceForReviewBadge() {
        RealtimeClinicalIntakeRepository repository = mock(RealtimeClinicalIntakeRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AiProperties properties = new AiProperties(
                true, "openai", "openai", 30, 20, 0.35, "fr", null, null, null);
        RealtimeClinicalIntakeService service = new RealtimeClinicalIntakeService(
                repository, visitRepository, properties);

        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getId()).thenReturn(visitId);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));

        RealtimeClinicalIntakeEntity entity = new RealtimeClinicalIntakeEntity(
                organizationId,
                visitId,
                RealtimeIntakeSource.CONSULTATION,
                1,
                "event-low-confidence",
                "item-low-confidence",
                "Paracétamol mille mg.",
                0.20,
                userId);
        when(repository.findByIdAndVisitIdAndSource(
                entity.getId(), visitId, RealtimeIntakeSource.CONSULTATION))
                .thenReturn(Optional.of(entity));
        when(repository.save(any(RealtimeClinicalIntakeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var corrected = service.correct(
                visitId,
                entity.getId(),
                userId,
                organizationId,
                "Paracétamol 1000 mg.");

        assertThat(corrected.confidence()).isEqualTo(0.20);
        assertThat(corrected.correctionCount()).isEqualTo(1);
        assertThat(corrected.originalTranscript()).isEqualTo("Paracétamol mille mg.");
        assertThat(corrected.transcript()).isEqualTo("Paracétamol 1000 mg.");
        assertThat(corrected.reviewRequired()).isFalse();
    }
}
