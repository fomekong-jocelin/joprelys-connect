package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionBatchEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionBatchRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionEvidenceRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalFactRevisionConcurrencyAuditTest {

    @Test
    void shouldLockVisitBeforeReadingBaseProjection() {
        Fixture fixture = fixture();
        UUID targetFactId = UUID.randomUUID();
        FactView target = fact(targetFactId, "douleur abdominale");
        when(fixture.projectionService().project(fixture.visitId(), fixture.organizationId()))
                .thenReturn(new NoteProjectionView(fixture.visitId(), "projection-v1", 1, List.of()));
        when(fixture.factLedgerService().listEffective(fixture.visitId(), fixture.organizationId()))
                .thenReturn(new FactLedgerView(fixture.visitId(), List.of(target)));
        when(fixture.batchRepository().save(any(ClinicalFactRevisionBatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.batchRepository().saveAndFlush(any(ClinicalFactRevisionBatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.operationRepository().save(any(ClinicalFactRevisionOperationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.operationRepository().findByBatchIdOrderByPositionNoAsc(any(UUID.class)))
                .thenReturn(List.of());

        fixture.service().apply(
                fixture.visitId(),
                fixture.userId(),
                fixture.organizationId(),
                new ApplyRevisionRequest(
                        UUID.randomUUID(),
                        "projection-v1",
                        List.of(new RevisionOperationRequest(
                                UUID.randomUUID(), OperationType.KEEP, targetFactId, null, null))));

        var ordered = inOrder(fixture.visitRepository(), fixture.projectionService());
        ordered.verify(fixture.visitRepository()).findByIdForUpdate(fixture.visitId());
        ordered.verify(fixture.projectionService()).project(fixture.visitId(), fixture.organizationId());
    }

    @Test
    void shouldReturnRevisionHistoryInRepositoryOrderWithoutReExecutingClinicalMutations() {
        Fixture fixture = fixture();
        when(fixture.visitRepository().findById(fixture.visitId()))
                .thenReturn(Optional.of(fixture.visit()));
        UUID revisionId = UUID.randomUUID();
        ClinicalFactRevisionBatchEntity batch = new ClinicalFactRevisionBatchEntity(
                fixture.organizationId(),
                fixture.visitId(),
                revisionId,
                "projection-v1",
                "a".repeat(64),
                fixture.userId());
        batch.complete("projection-v2");
        ClinicalFactRevisionOperationEntity keep = new ClinicalFactRevisionOperationEntity(
                batch.getId(),
                fixture.organizationId(),
                fixture.visitId(),
                UUID.randomUUID(),
                0,
                OperationType.KEEP,
                UUID.randomUUID(),
                null,
                null);
        when(fixture.batchRepository().findByVisitIdOrderByCreatedAtDesc(fixture.visitId()))
                .thenReturn(List.of(batch));
        when(fixture.operationRepository().findByBatchIdOrderByPositionNoAsc(batch.getId()))
                .thenReturn(List.of(keep));

        var history = fixture.service().history(fixture.visitId(), fixture.organizationId());

        assertThat(history.revisions()).hasSize(1);
        assertThat(history.revisions().getFirst().revisionId()).isEqualTo(revisionId);
        assertThat(history.revisions().getFirst().baseProjectionVersion()).isEqualTo("projection-v1");
        assertThat(history.revisions().getFirst().resultProjectionVersion()).isEqualTo("projection-v2");
        assertThat(history.revisions().getFirst().operations()).hasSize(1);
        assertThat(history.revisions().getFirst().operations().getFirst().type()).isEqualTo("KEEP");
    }

    private FactView fact(UUID id, String concept) {
        return new FactView(
                id,
                1,
                "source-" + id,
                FactType.SYMPTOM.name(),
                Authority.PATIENT_REPORTED.name(),
                "SYMPTOM_TEST",
                concept,
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
                List.of());
    }

    private Fixture fixture() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        ClinicalNoteProjectionService projectionService = mock(ClinicalNoteProjectionService.class);
        ClinicalFactLedgerService factLedgerService = mock(ClinicalFactLedgerService.class);
        ClinicalFactRevisionBatchRepository batchRepository = mock(ClinicalFactRevisionBatchRepository.class);
        ClinicalFactRevisionOperationRepository operationRepository = mock(ClinicalFactRevisionOperationRepository.class);
        ClinicalFactRevisionEvidenceRepository evidenceRepository = mock(ClinicalFactRevisionEvidenceRepository.class);
        ClinicalFactRepository factRepository = mock(ClinicalFactRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        ClinicalFactRevisionService service = new ClinicalFactRevisionService(
                projectionService,
                factLedgerService,
                mock(ClinicalFactRetractionValidator.class),
                new ClinicalFactRevisionRequestHasher(),
                batchRepository,
                operationRepository,
                evidenceRepository,
                factRepository,
                visitRepository);
        return new Fixture(
                visitId,
                userId,
                organizationId,
                visit,
                service,
                projectionService,
                factLedgerService,
                batchRepository,
                operationRepository,
                visitRepository);
    }

    private record Fixture(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            VisitEntity visit,
            ClinicalFactRevisionService service,
            ClinicalNoteProjectionService projectionService,
            ClinicalFactLedgerService factLedgerService,
            ClinicalFactRevisionBatchRepository batchRepository,
            ClinicalFactRevisionOperationRepository operationRepository,
            VisitRepository visitRepository) {
    }
}
