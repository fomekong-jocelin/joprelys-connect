package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEvidenceEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
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

class ClinicalFactLedgerServiceTest {

    private final ClinicalFactRepository factRepository = mock(ClinicalFactRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final ClinicalFactEvidenceValidator validator = mock(ClinicalFactEvidenceValidator.class);
    private final AmbientTranscriptLedgerService transcriptLedger = mock(AmbientTranscriptLedgerService.class);
    private final ClinicalFactRevisionOperationRepository revisionOperationRepository =
            mock(ClinicalFactRevisionOperationRepository.class);
    private final ClinicalFactRevisionEvidenceRepository revisionEvidenceRepository =
            mock(ClinicalFactRevisionEvidenceRepository.class);
    private final ClinicalFactLedgerService service = new ClinicalFactLedgerService(
            factRepository,
            visitRepository,
            validator,
            transcriptLedger,
            revisionOperationRepository,
            revisionEvidenceRepository);

    @Test
    void shouldPersistValidatedFactWithExactEvidenceAndSequence() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        FactCandidate candidate = symptomCandidate("fact-1", null, FactStatus.ASSERTED);
        when(validator.validate(visitId, organizationId, candidate))
                .thenReturn(validated(candidate));
        when(factRepository.findByVisitIdAndSourceEventId(visitId, "fact-1"))
                .thenReturn(Optional.empty());
        when(factRepository.findMaximumSequence(visitId)).thenReturn(0L);
        when(factRepository.saveAndFlush(any(ClinicalFactEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.appendValidatedFact(
                visitId, userId, organizationId, candidate);

        assertThat(result.sequence()).isEqualTo(1);
        assertThat(result.factType()).isEqualTo("SYMPTOM");
        assertThat(result.authority()).isEqualTo("PATIENT_REPORTED");
        assertThat(result.evidence()).hasSize(1);
        assertThat(result.evidence().getFirst().quoteText()).isEqualTo("Douleur genou gauche");
    }

    @Test
    void shouldReturnExistingFactForStrictlyIdenticalSourceEventRetry() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        FactCandidate candidate = symptomCandidate("fact-retry", null, FactStatus.ASSERTED);
        ClinicalFactEntity existing = entity(
                organizationId, visitId, 4, candidate, userId);
        addEvidence(existing, candidate.evidence().getFirst());
        when(validator.validate(visitId, organizationId, candidate))
                .thenReturn(validated(candidate));
        when(factRepository.findByVisitIdAndSourceEventId(visitId, "fact-retry"))
                .thenReturn(Optional.of(existing));

        var result = service.appendValidatedFact(
                visitId, userId, organizationId, candidate);

        assertThat(result.sequence()).isEqualTo(4);
        verify(factRepository, never()).saveAndFlush(any(ClinicalFactEntity.class));
    }

    @Test
    void shouldRejectSourceEventIdReuseWithDifferentClinicalMeaning() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        FactCandidate original = symptomCandidate("fact-same-id", null, FactStatus.ASSERTED);
        ClinicalFactEntity existing = entity(
                organizationId, visitId, 1, original, userId);
        addEvidence(existing, original.evidence().getFirst());
        FactCandidate changed = new FactCandidate(
                original.sourceEventId(),
                original.factType(),
                original.authority(),
                original.conceptCode(),
                original.conceptText(),
                original.polarity(),
                original.valuePrimary(),
                original.valueSecondary(),
                original.unitCode(),
                "depuis 3 jours",
                original.laterality(),
                original.frequencyText(),
                original.routeText(),
                original.status(),
                original.supersedesFactId(),
                original.evidence());
        when(validator.validate(visitId, organizationId, changed))
                .thenReturn(validated(changed));
        when(factRepository.findByVisitIdAndSourceEventId(visitId, "fact-same-id"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.appendValidatedFact(
                visitId, userId, organizationId, changed))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_EVENT_ID_REUSED");
    }

    @Test
    void shouldRejectSecondSuccessorForSameFactVersion() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);
        FactCandidate rootCandidate = symptomCandidate("root", null, FactStatus.ASSERTED);
        ClinicalFactEntity root = entity(
                organizationId, visitId, 1, rootCandidate, userId);
        FactCandidate successorCandidate = symptomCandidate(
                "successor-existing", root.getId(), FactStatus.ASSERTED);
        ClinicalFactEntity existingSuccessor = entity(
                organizationId, visitId, 2, successorCandidate, userId);
        FactCandidate competing = symptomCandidate(
                "successor-new", root.getId(), FactStatus.ASSERTED);

        when(validator.validate(visitId, organizationId, competing))
                .thenReturn(validated(competing));
        when(factRepository.findByVisitIdAndSourceEventId(visitId, "successor-new"))
                .thenReturn(Optional.empty());
        when(factRepository.findByIdAndVisitId(root.getId(), visitId))
                .thenReturn(Optional.of(root));
        when(factRepository.findByVisitIdAndSupersedesFactId(visitId, root.getId()))
                .thenReturn(Optional.of(existingSuccessor));

        assertThatThrownBy(() -> service.appendValidatedFact(
                visitId, userId, organizationId, competing))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_ALREADY_SUPERSEDED");
    }

    @Test
    void shouldRemoveExplicitlyRetractedLeafFromEffectiveViewButKeepAudit() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareReadableVisit(visitId, organizationId);

        FactCandidate rootCandidate = symptomCandidate("root-audit", null, FactStatus.ASSERTED);
        ClinicalFactEntity root = entity(
                organizationId, visitId, 1, rootCandidate, userId);
        addEvidence(root, rootCandidate.evidence().getFirst());
        FactCandidate retractionCandidate = symptomCandidate(
                "retract-audit", root.getId(), FactStatus.RETRACTED);
        ClinicalFactEntity retraction = entity(
                organizationId, visitId, 2, retractionCandidate, userId);
        addEvidence(retraction, retractionCandidate.evidence().getFirst());
        when(factRepository.findByVisitIdOrderBySequenceNoAsc(visitId))
                .thenReturn(List.of(root, retraction));
        effectiveTranscript(visitId, organizationId, rootCandidate.evidence().getFirst());

        var effective = service.listEffective(visitId, organizationId);
        var audit = service.listAudit(visitId, organizationId);

        assertThat(effective.facts()).isEmpty();
        assertThat(audit.facts()).hasSize(2);
        assertThat(audit.facts().get(0).status()).isEqualTo("ASSERTED");
        assertThat(audit.facts().get(1).status()).isEqualTo("RETRACTED");
        assertThat(audit.facts().get(1).supersedesFactId()).isEqualTo(root.getId());
    }

    @Test
    void shouldHideAssertedLeafWhenItsTranscriptEvidenceIsNoLongerEffectiveButKeepAudit() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareReadableVisit(visitId, organizationId);
        FactCandidate candidate = symptomCandidate("stale-evidence", null, FactStatus.ASSERTED);
        ClinicalFactEntity fact = entity(
                organizationId, visitId, 1, candidate, userId);
        addEvidence(fact, candidate.evidence().getFirst());
        when(factRepository.findByVisitIdOrderBySequenceNoAsc(visitId))
                .thenReturn(List.of(fact));
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of()));

        var effective = service.listEffective(visitId, organizationId);
        var audit = service.listAudit(visitId, organizationId);

        assertThat(effective.facts()).isEmpty();
        assertThat(audit.facts()).hasSize(1);
        assertThat(audit.facts().getFirst().sourceEventId()).isEqualTo("stale-evidence");
    }

    @Test
    void shouldKeepAssertedLeafWhenAllEvidenceItemsRemainEffective() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareReadableVisit(visitId, organizationId);
        FactCandidate candidate = symptomCandidate("current-evidence", null, FactStatus.ASSERTED);
        ClinicalFactEntity fact = entity(
                organizationId, visitId, 1, candidate, userId);
        addEvidence(fact, candidate.evidence().getFirst());
        when(factRepository.findByVisitIdOrderBySequenceNoAsc(visitId))
                .thenReturn(List.of(fact));
        effectiveTranscript(visitId, organizationId, candidate.evidence().getFirst());

        var effective = service.listEffective(visitId, organizationId);

        assertThat(effective.facts()).hasSize(1);
        assertThat(effective.facts().getFirst().sourceEventId()).isEqualTo("current-evidence");
    }

    private FactCandidate symptomCandidate(
            String eventId,
            UUID supersedes,
            FactStatus status) {
        UUID transcriptItemId = UUID.nameUUIDFromBytes("transcript-item".getBytes());
        EvidenceSpanCandidate evidence = new EvidenceSpanCandidate(
                transcriptItemId,
                0,
                "Douleur genou gauche".length(),
                "Douleur genou gauche",
                true);
        return new FactCandidate(
                eventId,
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "KNEE_PAIN",
                "Douleur genou",
                Polarity.POSITIVE,
                null,
                null,
                null,
                null,
                Laterality.LEFT,
                null,
                null,
                status,
                supersedes,
                List.of(evidence));
    }

    private ClinicalFactEvidenceValidator.ValidatedFact validated(FactCandidate candidate) {
        EvidenceSpanCandidate span = candidate.evidence().getFirst();
        TranscriptItemView item = transcriptItem(span);
        return new ClinicalFactEvidenceValidator.ValidatedFact(
                candidate,
                List.of(new ClinicalFactEvidenceValidator.ValidatedEvidence(span, item)));
    }

    private TranscriptItemView transcriptItem(EvidenceSpanCandidate span) {
        return new TranscriptItemView(
                span.transcriptItemId(),
                1,
                "source-1",
                "AMBIENT_DIARIZED",
                "PATIENT",
                "patient",
                span.quoteText(),
                "fr",
                0,
                1_000,
                "FINAL",
                null,
                Instant.now());
    }

    private ClinicalFactEntity entity(
            UUID organizationId,
            UUID visitId,
            long sequence,
            FactCandidate candidate,
            UUID userId) {
        return new ClinicalFactEntity(
                organizationId,
                visitId,
                sequence,
                candidate.sourceEventId(),
                candidate.factType(),
                candidate.authority(),
                candidate.conceptCode(),
                candidate.conceptText(),
                candidate.polarity(),
                candidate.valuePrimary(),
                candidate.valueSecondary(),
                candidate.unitCode(),
                candidate.temporalityText(),
                candidate.laterality(),
                candidate.frequencyText(),
                candidate.routeText(),
                candidate.status(),
                candidate.supersedesFactId(),
                userId);
    }

    private void addEvidence(ClinicalFactEntity entity, EvidenceSpanCandidate span) {
        entity.addEvidence(new ClinicalFactEvidenceEntity(
                span.transcriptItemId(),
                span.quoteStartChar(),
                span.quoteEndChar(),
                span.quoteText(),
                span.primarySupport()));
    }

    private void effectiveTranscript(
            UUID visitId,
            UUID organizationId,
            EvidenceSpanCandidate span) {
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(
                        visitId,
                        List.of(transcriptItem(span))));
    }

    private void prepareLockedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
    }

    private void prepareReadableVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
    }
}
