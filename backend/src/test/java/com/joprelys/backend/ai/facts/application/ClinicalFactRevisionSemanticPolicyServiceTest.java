package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionBatchRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionEvidenceRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClinicalFactRevisionSemanticPolicyServiceTest {

    @Test
    void shouldRejectDuplicateAddAndRequireKeep() {
        Fixture fixture = fixture();
        RevisionOperationRequest add = new RevisionOperationRequest(
                UUID.randomUUID(), OperationType.ADD, null, fixture.payload(), null);

        assertThatThrownBy(() -> fixture.service().apply(
                fixture.visitId(), fixture.userId(), fixture.organizationId(),
                new ApplyRevisionRequest(UUID.randomUUID(), "projection-v1", List.of(add))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_ADD_DUPLICATES_EFFECTIVE_FACT");

        verify(fixture.batchRepository(), never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectNoOpReplacementAndRequireKeep() {
        Fixture fixture = fixture();
        RevisionOperationRequest replace = new RevisionOperationRequest(
                UUID.randomUUID(), OperationType.REPLACE, fixture.fact().id(), fixture.payload(), null);

        assertThatThrownBy(() -> fixture.service().apply(
                fixture.visitId(), fixture.userId(), fixture.organizationId(),
                new ApplyRevisionRequest(UUID.randomUUID(), "projection-v1", List.of(replace))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_REPLACE_NO_CHANGE");

        verify(fixture.batchRepository(), never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private Fixture fixture() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID transcriptItemId = UUID.randomUUID();
        String quote = "douleur abdominale";
        EvidenceSpanCandidate evidence = new EvidenceSpanCandidate(
                transcriptItemId, 0, quote.length(), quote, true);
        FactPayload payload = new FactPayload(
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "ABDOMINAL_PAIN",
                quote,
                Polarity.POSITIVE,
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED,
                null,
                null,
                List.of(evidence));
        FactView fact = new FactView(
                UUID.randomUUID(),
                1,
                "existing-fact",
                FactType.SYMPTOM.name(),
                Authority.PATIENT_REPORTED.name(),
                "ABDOMINAL_PAIN",
                quote,
                Polarity.POSITIVE.name(),
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED.name(),
                null,
                null,
                FactStatus.ASSERTED.name(),
                null,
                Instant.now(),
                List.of(new EvidenceSpanView(
                        UUID.randomUUID(), transcriptItemId, 0, quote.length(), quote, true)));

        ClinicalNoteProjectionService projectionService = mock(ClinicalNoteProjectionService.class);
        ClinicalFactLedgerService factLedgerService = mock(ClinicalFactLedgerService.class);
        ClinicalFactRevisionBatchRepository batchRepository = mock(ClinicalFactRevisionBatchRepository.class);
        ClinicalFactRevisionOperationRepository operationRepository = mock(ClinicalFactRevisionOperationRepository.class);
        ClinicalFactRevisionEvidenceRepository evidenceRepository = mock(ClinicalFactRevisionEvidenceRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(projectionService.project(visitId, organizationId))
                .thenReturn(new NoteProjectionView(visitId, "projection-v1", 1, List.of()));
        when(factLedgerService.listEffective(visitId, organizationId))
                .thenReturn(new FactLedgerView(visitId, List.of(fact)));

        ClinicalFactRevisionService service = new ClinicalFactRevisionService(
                projectionService,
                factLedgerService,
                mock(ClinicalFactRetractionValidator.class),
                new ClinicalFactRevisionRequestHasher(),
                batchRepository,
                operationRepository,
                evidenceRepository,
                mock(ClinicalFactRepository.class),
                visitRepository);
        return new Fixture(
                visitId, userId, organizationId, fact, payload, service, batchRepository);
    }

    private record Fixture(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            FactView fact,
            FactPayload payload,
            ClinicalFactRevisionService service,
            ClinicalFactRevisionBatchRepository batchRepository) {
    }
}
