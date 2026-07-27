package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEvidenceEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClinicalFactLedgerService {

    private final ClinicalFactRepository factRepository;
    private final VisitRepository visitRepository;
    private final ClinicalFactEvidenceValidator evidenceValidator;
    private final AmbientTranscriptLedgerService transcriptLedgerService;

    public ClinicalFactLedgerService(
            ClinicalFactRepository factRepository,
            VisitRepository visitRepository,
            ClinicalFactEvidenceValidator evidenceValidator,
            AmbientTranscriptLedgerService transcriptLedgerService) {
        this.factRepository = factRepository;
        this.visitRepository = visitRepository;
        this.evidenceValidator = evidenceValidator;
        this.transcriptLedgerService = transcriptLedgerService;
    }

    @Transactional
    public FactView appendValidatedFact(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            FactCandidate candidate) {
        requireIdentity(visitId, userId, organizationId);
        lockAuthorizedVisit(visitId, organizationId);
        var validated = evidenceValidator.validate(visitId, organizationId, candidate);

        String eventId = candidate.sourceEventId().trim();
        var existing = factRepository.findByVisitIdAndSourceEventId(visitId, eventId);
        if (existing.isPresent()) {
            if (!sameCandidate(existing.get(), candidate)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "AI_CLINICAL_FACT_EVENT_ID_REUSED");
            }
            return view(existing.get());
        }

        if (candidate.supersedesFactId() != null) {
            ClinicalFactEntity target = factRepository.findByIdAndVisitId(candidate.supersedesFactId(), visitId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "AI_CLINICAL_FACT_SUPERSESSION_TARGET_NOT_FOUND"));
            if (factRepository.findByVisitIdAndSupersedesFactId(visitId, target.getId()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "AI_CLINICAL_FACT_ALREADY_SUPERSEDED");
            }
        }

        long sequence = factRepository.findMaximumSequence(visitId) + 1;
        ClinicalFactEntity entity = new ClinicalFactEntity(
                organizationId,
                visitId,
                sequence,
                eventId,
                candidate.factType(),
                candidate.authority(),
                candidate.conceptCode().trim().toUpperCase(Locale.ROOT),
                candidate.conceptText().trim(),
                candidate.polarity(),
                trimToNull(candidate.valuePrimary()),
                trimToNull(candidate.valueSecondary()),
                upperToNull(candidate.unitCode()),
                trimToNull(candidate.temporalityText()),
                candidate.laterality(),
                trimToNull(candidate.frequencyText()),
                trimToNull(candidate.routeText()),
                candidate.status(),
                candidate.supersedesFactId(),
                userId);
        for (var evidence : validated.evidence()) {
            EvidenceSpanCandidate span = evidence.span();
            entity.addEvidence(new ClinicalFactEvidenceEntity(
                    span.transcriptItemId(),
                    span.quoteStartChar(),
                    span.quoteEndChar(),
                    span.quoteText(),
                    span.primarySupport()));
        }

        try {
            return view(factRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_CLINICAL_FACT_CONCURRENT_CONFLICT",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public FactLedgerView listEffective(UUID visitId, UUID organizationId) {
        Set<UUID> effectiveTranscriptItemIds = transcriptLedgerService
                .listFinal(visitId, organizationId)
                .items()
                .stream()
                .map(item -> item.id())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return listEffectiveForTranscriptSnapshot(
                visitId,
                organizationId,
                effectiveTranscriptItemIds);
    }

    /**
     * Resolves the effective fact leaves against one immutable transcript snapshot.
     * Package-private on purpose: projections in this application package can build a
     * self-consistent linked-evidence snapshot without exposing this internal primitive
     * through the API layer.
     */
    FactLedgerView listEffectiveForTranscriptSnapshot(
            UUID visitId,
            UUID organizationId,
            Set<UUID> effectiveTranscriptItemIds) {
        requireAuthorizedVisit(visitId, organizationId);
        if (effectiveTranscriptItemIds == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI_CLINICAL_FACT_TRANSCRIPT_SNAPSHOT_INVALID");
        }
        return resolveEffective(
                visitId,
                factRepository.findByVisitIdOrderBySequenceNoAsc(visitId),
                effectiveTranscriptItemIds);
    }

    private FactLedgerView resolveEffective(
            UUID visitId,
            List<ClinicalFactEntity> all,
            Set<UUID> effectiveTranscriptItemIds) {
        Set<UUID> superseded = new HashSet<>();
        for (ClinicalFactEntity fact : all) {
            if (fact.getSupersedesFactId() != null) superseded.add(fact.getSupersedesFactId());
        }
        List<FactView> effective = all.stream()
                .filter(fact -> !superseded.contains(fact.getId()))
                .filter(fact -> fact.getFactStatus() == FactStatus.ASSERTED)
                .filter(fact -> fact.getEvidence().stream().allMatch(
                        evidence -> effectiveTranscriptItemIds.contains(evidence.getTranscriptItemId())))
                .sorted(Comparator.comparingLong(ClinicalFactEntity::getSequenceNo))
                .map(this::view)
                .toList();
        return new FactLedgerView(visitId, effective);
    }

    @Transactional(readOnly = true)
    public FactLedgerView listAudit(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        return new FactLedgerView(
                visitId,
                factRepository.findByVisitIdOrderBySequenceNoAsc(visitId)
                        .stream()
                        .map(this::view)
                        .toList());
    }

    private boolean sameCandidate(ClinicalFactEntity existing, FactCandidate candidate) {
        if (existing.getFactType() != candidate.factType()
                || existing.getAuthority() != candidate.authority()
                || !existing.getConceptCode().equals(candidate.conceptCode().trim().toUpperCase(Locale.ROOT))
                || !existing.getConceptText().equals(candidate.conceptText().trim())
                || existing.getPolarity() != candidate.polarity()
                || !Objects.equals(existing.getValuePrimary(), trimToNull(candidate.valuePrimary()))
                || !Objects.equals(existing.getValueSecondary(), trimToNull(candidate.valueSecondary()))
                || !Objects.equals(existing.getUnitCode(), upperToNull(candidate.unitCode()))
                || !Objects.equals(existing.getTemporalityText(), trimToNull(candidate.temporalityText()))
                || existing.getLaterality() != candidate.laterality()
                || !Objects.equals(existing.getFrequencyText(), trimToNull(candidate.frequencyText()))
                || !Objects.equals(existing.getRouteText(), trimToNull(candidate.routeText()))
                || existing.getFactStatus() != candidate.status()
                || !Objects.equals(existing.getSupersedesFactId(), candidate.supersedesFactId())) {
            return false;
        }
        List<EvidenceSpanCandidate> expected = new ArrayList<>(candidate.evidence());
        expected.sort(Comparator.comparing(EvidenceSpanCandidate::transcriptItemId)
                .thenComparingInt(EvidenceSpanCandidate::quoteStartChar)
                .thenComparingInt(EvidenceSpanCandidate::quoteEndChar));
        List<ClinicalFactEvidenceEntity> actual = new ArrayList<>(existing.getEvidence());
        actual.sort(Comparator.comparing(ClinicalFactEvidenceEntity::getTranscriptItemId)
                .thenComparingInt(ClinicalFactEvidenceEntity::getQuoteStartChar)
                .thenComparingInt(ClinicalFactEvidenceEntity::getQuoteEndChar));
        if (expected.size() != actual.size()) return false;
        for (int index = 0; index < expected.size(); index++) {
            EvidenceSpanCandidate left = expected.get(index);
            ClinicalFactEvidenceEntity right = actual.get(index);
            if (!left.transcriptItemId().equals(right.getTranscriptItemId())
                    || left.quoteStartChar() != right.getQuoteStartChar()
                    || left.quoteEndChar() != right.getQuoteEndChar()
                    || !left.quoteText().equals(right.getQuoteText())
                    || left.primarySupport() != right.isPrimarySupport()) {
                return false;
            }
        }
        return true;
    }

    private VisitEntity lockAuthorizedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
        return visit;
    }

    private void requireAuthorizedVisit(UUID visitId, UUID organizationId) {
        if (visitId == null || organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_CLINICAL_FACT_IDENTITY_INVALID");
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private void requireIdentity(UUID visitId, UUID userId, UUID organizationId) {
        if (visitId == null || userId == null || organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_CLINICAL_FACT_IDENTITY_INVALID");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upperToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private FactView view(ClinicalFactEntity fact) {
        List<EvidenceSpanView> evidence = fact.getEvidence().stream()
                .map(item -> new EvidenceSpanView(
                        item.getId(),
                        item.getTranscriptItemId(),
                        item.getQuoteStartChar(),
                        item.getQuoteEndChar(),
                        item.getQuoteText(),
                        item.isPrimarySupport()))
                .toList();
        return new FactView(
                fact.getId(),
                fact.getSequenceNo(),
                fact.getSourceEventId(),
                fact.getFactType().name(),
                fact.getAuthority().name(),
                fact.getConceptCode(),
                fact.getConceptText(),
                fact.getPolarity().name(),
                fact.getValuePrimary(),
                fact.getValueSecondary(),
                fact.getUnitCode(),
                fact.getTemporalityText(),
                fact.getLaterality().name(),
                fact.getFrequencyText(),
                fact.getRouteText(),
                fact.getFactStatus().name(),
                fact.getSupersedesFactId(),
                fact.getCreatedAt(),
                evidence);
    }
}
