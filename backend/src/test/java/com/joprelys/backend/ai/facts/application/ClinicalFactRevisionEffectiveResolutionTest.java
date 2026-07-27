package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionReason;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEvidenceEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionEvidenceEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionEvidenceRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalFactRevisionEffectiveResolutionTest {

    @Test
    void shouldHideTargetOnlyWhileRetractionEvidenceBelongsToEffectiveTranscriptSnapshot() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID factEvidenceItemId = UUID.randomUUID();
        UUID retractionEvidenceItemId = UUID.randomUUID();

        ClinicalFactRepository factRepository = mock(ClinicalFactRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        ClinicalFactRevisionOperationRepository operationRepository =
                mock(ClinicalFactRevisionOperationRepository.class);
        ClinicalFactRevisionEvidenceRepository evidenceRepository =
                mock(ClinicalFactRevisionEvidenceRepository.class);
        ClinicalFactLedgerService service = new ClinicalFactLedgerService(
                factRepository,
                visitRepository,
                mock(ClinicalFactEvidenceValidator.class),
                mock(AmbientTranscriptLedgerService.class),
                operationRepository,
                evidenceRepository);

        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        ClinicalFactEntity fact = new ClinicalFactEntity(
                organizationId,
                visitId,
                1,
                "fact-root",
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "FEVER",
                "fièvre",
                Polarity.POSITIVE,
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED,
                null,
                null,
                FactStatus.ASSERTED,
                null,
                userId);
        fact.addEvidence(new ClinicalFactEvidenceEntity(
                factEvidenceItemId,
                0,
                6,
                "fièvre",
                true));
        when(factRepository.findByVisitIdOrderBySequenceNoAsc(visitId))
                .thenReturn(List.of(fact));

        ClinicalFactRevisionOperationEntity retraction = new ClinicalFactRevisionOperationEntity(
                UUID.randomUUID(),
                organizationId,
                visitId,
                UUID.randomUUID(),
                0,
                OperationType.RETRACT,
                fact.getId(),
                null,
                RetractionReason.EXPLICIT_NEGATION);
        when(operationRepository.findByVisitIdAndOperationTypeOrderByCreatedAtAsc(
                visitId, OperationType.RETRACT))
                .thenReturn(List.of(retraction));
        when(evidenceRepository.findByOperationIdOrderByPrimarySupportDescQuoteStartCharAsc(retraction.getId()))
                .thenReturn(List.of(new ClinicalFactRevisionEvidenceEntity(
                        retraction.getId(),
                        organizationId,
                        visitId,
                        retractionEvidenceItemId,
                        0,
                        20,
                        "je n'ai pas de fièvre",
                        true)));

        var withRetractionEvidence = service.listEffectiveForTranscriptSnapshot(
                visitId,
                organizationId,
                Set.of(factEvidenceItemId, retractionEvidenceItemId));
        var afterRetractionEvidenceCorrection = service.listEffectiveForTranscriptSnapshot(
                visitId,
                organizationId,
                Set.of(factEvidenceItemId));

        assertThat(withRetractionEvidence.facts()).isEmpty();
        assertThat(afterRetractionEvidenceCorrection.facts())
                .extracting(ClinicalFactContract.FactView::id)
                .containsExactly(fact.getId());
    }
}
