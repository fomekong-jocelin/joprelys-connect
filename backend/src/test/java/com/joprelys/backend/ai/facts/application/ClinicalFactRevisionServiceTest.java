package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionReason;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class ClinicalFactRevisionServiceTest {

    private final ClinicalNoteProjectionService projectionService = mock(ClinicalNoteProjectionService.class);
    private final ClinicalFactLedgerService factLedgerService = mock(ClinicalFactLedgerService.class);
    private final ClinicalFactRetractionValidator retractionValidator = mock(ClinicalFactRetractionValidator.class);
    private final ClinicalFactRevisionBatchRepository batchRepository = mock(ClinicalFactRevisionBatchRepository.class);
    private final ClinicalFactRevisionOperationRepository operationRepository = mock(ClinicalFactRevisionOperationRepository.class);
    private final ClinicalFactRevisionEvidenceRepository evidenceRepository = mock(ClinicalFactRevisionEvidenceRepository.class);
    private final ClinicalFactRepository factRepository = mock(ClinicalFactRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final ClinicalFactRevisionService service = new ClinicalFactRevisionService(
            projectionService,
            factLedgerService,
            retractionValidator,
            new ClinicalFactRevisionRequestHasher(),
            batchRepository,
            operationRepository,
            evidenceRepository,
            factRepository,
            visitRepository);

    @BeforeEach
    void configurePersistenceMocks() {
        when(batchRepository.save(any(ClinicalFactRevisionBatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(batchRepository.saveAndFlush(any(ClinicalFactRevisionBatchEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(operationRepository.save(any(ClinicalFactRevisionOperationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(operationRepository.findByBatchIdOrderByPositionNoAsc(any(UUID.class)))
                .thenReturn(List.of());
    }

    @Test
    void shouldFailStaleBeforeAnyClinicalMutation() {
        Context context = context();
        when(projectionService.project(context.visitId(), context.organizationId()))
                .thenReturn(projection(context.visitId(), "projection-v2"));
        ApplyRevisionRequest request = new ApplyRevisionRequest(
                UUID.randomUUID(),
                "projection-v1",
                List.of(keep(UUID.randomUUID())));

        assertThatThrownBy(() -> service.apply(
                context.visitId(), context.userId(), context.organizationId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_STALE");

        verify(batchRepository, never()).save(any());
        verify(factLedgerService, never()).appendValidatedFact(any(), any(), any(), any());
    }

    @Test
    void shouldKeepEffectiveFactWithoutCreatingAnotherFact() {
        Context context = context();
        FactView target = fact(UUID.randomUUID(), "douleur abdominale");
        prepareBase(context, target, "projection-v1", "projection-v1");
        ApplyRevisionRequest request = new ApplyRevisionRequest(
                UUID.randomUUID(),
                "projection-v1",
                List.of(keep(target.id())));

        var result = service.apply(
                context.visitId(), context.userId(), context.organizationId(), request);

        assertThat(result.resultProjectionVersion()).isEqualTo("projection-v1");
        verify(factLedgerService, never()).appendValidatedFact(any(), any(), any(), any());
        ArgumentCaptor<ClinicalFactRevisionOperationEntity> operation =
                ArgumentCaptor.forClass(ClinicalFactRevisionOperationEntity.class);
        verify(operationRepository).save(operation.capture());
        assertThat(operation.getValue().getOperationType()).isEqualTo(OperationType.KEEP);
        assertThat(operation.getValue().getTargetFactId()).isEqualTo(target.id());
        assertThat(operation.getValue().getResultFactId()).isNull();
    }

    @Test
    void shouldAddValidatedFactWithDeterministicRevisionSourceEvent() {
        Context context = context();
        prepareBase(context, null, "projection-v1", "projection-v2");
        UUID revisionId = UUID.randomUUID();
        RevisionOperationRequest operation = add(factPayload("douleur thoracique"));
        FactView created = fact(UUID.randomUUID(), "douleur thoracique");
        when(factLedgerService.appendValidatedFact(
                eq(context.visitId()),
                eq(context.userId()),
                eq(context.organizationId()),
                any(FactCandidate.class)))
                .thenReturn(created);

        service.apply(
                context.visitId(),
                context.userId(),
                context.organizationId(),
                new ApplyRevisionRequest(revisionId, "projection-v1", List.of(operation)));

        ArgumentCaptor<FactCandidate> candidate = ArgumentCaptor.forClass(FactCandidate.class);
        verify(factLedgerService).appendValidatedFact(
                eq(context.visitId()),
                eq(context.userId()),
                eq(context.organizationId()),
                candidate.capture());
        assertThat(candidate.getValue().status()).isEqualTo(FactStatus.ASSERTED);
        assertThat(candidate.getValue().supersedesFactId()).isNull();
        assertThat(candidate.getValue().sourceEventId())
                .isEqualTo("revision-v1-" + revisionId + "-" + operation.operationId());
    }

    @Test
    void shouldReplaceOnlyAnEffectiveTargetByAppendingSuccessor() {
        Context context = context();
        FactView target = fact(UUID.randomUUID(), "douleur abdominale");
        prepareBase(context, target, "projection-v1", "projection-v2");
        RevisionOperationRequest operation = replace(target.id(), factPayload("douleur abdominale droite"));
        when(factLedgerService.appendValidatedFact(
                eq(context.visitId()),
                eq(context.userId()),
                eq(context.organizationId()),
                any(FactCandidate.class)))
                .thenReturn(fact(UUID.randomUUID(), "douleur abdominale droite"));

        service.apply(
                context.visitId(),
                context.userId(),
                context.organizationId(),
                new ApplyRevisionRequest(UUID.randomUUID(), "projection-v1", List.of(operation)));

        ArgumentCaptor<FactCandidate> candidate = ArgumentCaptor.forClass(FactCandidate.class);
        verify(factLedgerService).appendValidatedFact(
                eq(context.visitId()),
                eq(context.userId()),
                eq(context.organizationId()),
                candidate.capture());
        assertThat(candidate.getValue().supersedesFactId()).isEqualTo(target.id());
        assertThat(candidate.getValue().status()).isEqualTo(FactStatus.ASSERTED);
    }

    @Test
    void shouldRetractWithValidatedFinalEvidenceWithoutDeletingTargetFact() {
        Context context = context();
        FactView target = fact(UUID.randomUUID(), "amoxicilline");
        prepareBase(context, target, "projection-v1", "projection-v2");
        EvidenceSpanCandidate evidence = evidence("annule amoxicilline");
        RetractionPayload retraction = new RetractionPayload(
                Authority.CLINICIAN_DECISION,
                RetractionReason.CLINICIAN_CANCELLATION,
                List.of(evidence));
        RevisionOperationRequest operation = new RevisionOperationRequest(
                UUID.randomUUID(),
                OperationType.RETRACT,
                target.id(),
                null,
                retraction);
        when(retractionValidator.validate(
                context.visitId(), context.organizationId(), target, retraction))
                .thenReturn(List.of(evidence));

        service.apply(
                context.visitId(),
                context.userId(),
                context.organizationId(),
                new ApplyRevisionRequest(UUID.randomUUID(), "projection-v1", List.of(operation)));

        verify(retractionValidator).validate(
                context.visitId(), context.organizationId(), target, retraction);
        verify(evidenceRepository).saveAll(any());
        verify(factRepository, never()).delete(any());
        verify(factRepository, never()).deleteById(any());
        ArgumentCaptor<ClinicalFactRevisionOperationEntity> persisted =
                ArgumentCaptor.forClass(ClinicalFactRevisionOperationEntity.class);
        verify(operationRepository).save(persisted.capture());
        assertThat(persisted.getValue().getOperationType()).isEqualTo(OperationType.RETRACT);
        assertThat(persisted.getValue().getTargetFactId()).isEqualTo(target.id());
        assertThat(persisted.getValue().getRetractionReason())
                .isEqualTo(RetractionReason.CLINICIAN_CANCELLATION);
    }

    @Test
    void shouldRejectTwoOperationsThatTargetSameFactInOneBatch() {
        Context context = context();
        FactView target = fact(UUID.randomUUID(), "douleur abdominale");
        when(projectionService.project(context.visitId(), context.organizationId()))
                .thenReturn(projection(context.visitId(), "projection-v1"));
        when(factLedgerService.listEffective(context.visitId(), context.organizationId()))
                .thenReturn(new FactLedgerView(context.visitId(), List.of(target)));
        ApplyRevisionRequest request = new ApplyRevisionRequest(
                UUID.randomUUID(),
                "projection-v1",
                List.of(keep(target.id()), replace(target.id(), factPayload("douleur abdominale droite"))));

        assertThatThrownBy(() -> service.apply(
                context.visitId(), context.userId(), context.organizationId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_TARGET_CONFLICT");

        verify(batchRepository, never()).save(any());
    }

    @Test
    void shouldRejectMutationOfFactThatIsNotEffectiveInBaseProjection() {
        Context context = context();
        when(projectionService.project(context.visitId(), context.organizationId()))
                .thenReturn(projection(context.visitId(), "projection-v1"));
        when(factLedgerService.listEffective(context.visitId(), context.organizationId()))
                .thenReturn(new FactLedgerView(context.visitId(), List.of()));
        ApplyRevisionRequest request = new ApplyRevisionRequest(
                UUID.randomUUID(),
                "projection-v1",
                List.of(keep(UUID.randomUUID())));

        assertThatThrownBy(() -> service.apply(
                context.visitId(), context.userId(), context.organizationId(), request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_TARGET_NOT_EFFECTIVE");
    }

    @Test
    void shouldReturnStrictlyIdempotentRetryWithoutReExecutingOperations() {
        Context context = context();
        UUID revisionId = UUID.randomUUID();
        ApplyRevisionRequest request = new ApplyRevisionRequest(
                revisionId,
                "projection-v1",
                List.of(add(factPayload("douleur abdominale"))));
        ClinicalFactRevisionRequestHasher hasher = new ClinicalFactRevisionRequestHasher();
        ClinicalFactRevisionBatchEntity existing = new ClinicalFactRevisionBatchEntity(
                context.organizationId(),
                context.visitId(),
                revisionId,
                "projection-v1",
                hasher.sha256(request),
                context.userId());
        existing.complete("projection-v2");
        when(batchRepository.findByVisitIdAndRevisionRequestId(context.visitId(), revisionId))
                .thenReturn(Optional.of(existing));

        var result = service.apply(
                context.visitId(), context.userId(), context.organizationId(), request);

        assertThat(result.resultProjectionVersion()).isEqualTo("projection-v2");
        verify(projectionService, never()).project(any(), any());
        verify(factLedgerService, never()).appendValidatedFact(any(), any(), any(), any());
    }

    @Test
    void shouldRejectRevisionIdRetryWhenPayloadChanged() {
        Context context = context();
        UUID revisionId = UUID.randomUUID();
        ApplyRevisionRequest original = new ApplyRevisionRequest(
                revisionId,
                "projection-v1",
                List.of(add(factPayload("douleur abdominale"))));
        ApplyRevisionRequest changed = new ApplyRevisionRequest(
                revisionId,
                "projection-v1",
                List.of(add(factPayload("douleur thoracique"))));
        ClinicalFactRevisionBatchEntity existing = new ClinicalFactRevisionBatchEntity(
                context.organizationId(),
                context.visitId(),
                revisionId,
                "projection-v1",
                new ClinicalFactRevisionRequestHasher().sha256(original),
                context.userId());
        when(batchRepository.findByVisitIdAndRevisionRequestId(context.visitId(), revisionId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.apply(
                context.visitId(), context.userId(), context.organizationId(), changed))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_ID_REUSED");
    }

    private void prepareBase(
            Context context,
            FactView target,
            String baseVersion,
            String resultVersion) {
        when(projectionService.project(context.visitId(), context.organizationId()))
                .thenReturn(
                        projection(context.visitId(), baseVersion),
                        projection(context.visitId(), resultVersion));
        when(factLedgerService.listEffective(context.visitId(), context.organizationId()))
                .thenReturn(new FactLedgerView(
                        context.visitId(),
                        target == null ? List.of() : List.of(target)));
    }

    private Context context() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        return new Context(visitId, userId, organizationId);
    }

    private RevisionOperationRequest keep(UUID targetFactId) {
        return new RevisionOperationRequest(
                UUID.randomUUID(), OperationType.KEEP, targetFactId, null, null);
    }

    private RevisionOperationRequest add(FactPayload payload) {
        return new RevisionOperationRequest(
                UUID.randomUUID(), OperationType.ADD, null, payload, null);
    }

    private RevisionOperationRequest replace(UUID targetFactId, FactPayload payload) {
        return new RevisionOperationRequest(
                UUID.randomUUID(), OperationType.REPLACE, targetFactId, payload, null);
    }

    private FactPayload factPayload(String concept) {
        return new FactPayload(
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "SYMPTOM_TEST",
                concept,
                Polarity.POSITIVE,
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED,
                null,
                null,
                List.of(evidence(concept)));
    }

    private FactView fact(UUID id, String concept) {
        EvidenceSpanCandidate source = evidence(concept);
        return new FactView(
                id,
                1,
                "existing-" + id,
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
                List.of(new EvidenceSpanView(
                        UUID.randomUUID(),
                        source.transcriptItemId(),
                        source.quoteStartChar(),
                        source.quoteEndChar(),
                        source.quoteText(),
                        true)));
    }

    private EvidenceSpanCandidate evidence(String quote) {
        return new EvidenceSpanCandidate(
                UUID.randomUUID(),
                0,
                quote.length(),
                quote,
                true);
    }

    private NoteProjectionView projection(UUID visitId, String version) {
        return new NoteProjectionView(visitId, version, 1, List.of());
    }

    private record Context(UUID visitId, UUID userId, UUID organizationId) {
    }
}
