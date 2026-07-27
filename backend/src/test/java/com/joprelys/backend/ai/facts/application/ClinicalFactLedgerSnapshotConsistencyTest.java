package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEvidenceEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalFactLedgerSnapshotConsistencyTest {

    @Test
    void shouldResolveFactsAgainstProvidedTranscriptSnapshotWithoutReadingTranscriptAgain() {
        ClinicalFactRepository factRepository = mock(ClinicalFactRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        ClinicalFactEvidenceValidator validator = mock(ClinicalFactEvidenceValidator.class);
        AmbientTranscriptLedgerService transcriptLedger = mock(AmbientTranscriptLedgerService.class);
        ClinicalFactLedgerService service = new ClinicalFactLedgerService(
                factRepository,
                visitRepository,
                validator,
                transcriptLedger);

        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID transcriptItemId = UUID.randomUUID();

        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        ClinicalFactEntity fact = new ClinicalFactEntity(
                organizationId,
                visitId,
                7,
                "snapshot-fact-1",
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "CHEST_PAIN",
                "douleur thoracique",
                Polarity.POSITIVE,
                null,
                null,
                null,
                "depuis ce matin",
                Laterality.UNSPECIFIED,
                null,
                null,
                FactStatus.ASSERTED,
                null,
                userId);
        fact.addEvidence(new ClinicalFactEvidenceEntity(
                transcriptItemId,
                0,
                20,
                "douleur thoracique",
                true));
        when(factRepository.findByVisitIdOrderBySequenceNoAsc(visitId))
                .thenReturn(List.of(fact));

        var result = service.listEffectiveForTranscriptSnapshot(
                visitId,
                organizationId,
                Set.of(transcriptItemId));

        assertThat(result.facts()).hasSize(1);
        assertThat(result.facts().getFirst().sourceEventId()).isEqualTo("snapshot-fact-1");
        assertThat(result.facts().getFirst().evidence().getFirst().transcriptItemId())
                .isEqualTo(transcriptItemId);
        verifyNoInteractions(transcriptLedger);
    }
}
