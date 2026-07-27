package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionReason;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClinicalFactRetractionValidator {

    private static final Set<String> CORRECTION_MARKERS = Set.of(
            "correction", "corrige", "corriger", "erreur", "ignorez", "ignorer",
            "retirez", "retirer", "supprimez", "supprimer", "je me suis trompe",
            "mistake", "correct", "ignore", "disregard", "remove", "retract");
    private static final Set<String> NEGATION_MARKERS = Set.of(
            "pas", "sans", "aucun", "aucune", "non", "nie", "nient",
            "no", "not", "without", "deny", "denies");
    private static final Set<String> CANCELLATION_MARKERS = Set.of(
            "annule", "annuler", "annulez", "arrete", "arreter", "arretez",
            "stop", "cancel", "cancelled", "canceling", "discontinue", "discontinued");

    private final AmbientTranscriptLedgerService transcriptLedgerService;

    public ClinicalFactRetractionValidator(AmbientTranscriptLedgerService transcriptLedgerService) {
        this.transcriptLedgerService = transcriptLedgerService;
    }

    public List<EvidenceSpanCandidate> validate(
            UUID visitId,
            UUID organizationId,
            FactView target,
            RetractionPayload payload) {
        if (target == null || payload == null || payload.authority() == null || payload.reason() == null
                || payload.evidence() == null || payload.evidence().isEmpty() || payload.evidence().size() > 8) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_INVALID");
        }
        if (payload.evidence().stream().filter(EvidenceSpanCandidate::primarySupport).count() != 1) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_PRIMARY_EVIDENCE_INVALID");
        }

        Map<UUID, TranscriptItemView> effective = new HashMap<>();
        transcriptLedgerService.listFinal(visitId, organizationId).items()
                .forEach(item -> effective.put(item.id(), item));

        for (EvidenceSpanCandidate span : payload.evidence()) {
            TranscriptItemView item = effective.get(span.transcriptItemId());
            if (item == null || !"FINAL".equals(item.status())) {
                throw conflict("AI_CLINICAL_FACT_RETRACTION_EVIDENCE_NOT_EFFECTIVE");
            }
            validateExactSpan(span, item);
        }

        validateSpeakerPolicy(target, payload.authority(), payload.evidence(), effective);
        EvidenceSpanCandidate primary = payload.evidence().stream()
                .filter(EvidenceSpanCandidate::primarySupport)
                .findFirst()
                .orElseThrow(() -> invalid("AI_CLINICAL_FACT_RETRACTION_PRIMARY_EVIDENCE_INVALID"));
        validateReason(target, payload.reason(), primary, effective.get(primary.transcriptItemId()));
        return List.copyOf(payload.evidence());
    }

    private void validateExactSpan(EvidenceSpanCandidate span, TranscriptItemView item) {
        if (span == null || span.transcriptItemId() == null || span.quoteText() == null) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_EVIDENCE_INVALID");
        }
        String text = item.text();
        if (text == null || span.quoteStartChar() < 0 || span.quoteEndChar() <= span.quoteStartChar()
                || span.quoteEndChar() > text.length() || span.quoteText().length() > 2_000) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_EVIDENCE_RANGE_INVALID");
        }
        if (!text.substring(span.quoteStartChar(), span.quoteEndChar()).equals(span.quoteText())) {
            throw conflict("AI_CLINICAL_FACT_RETRACTION_EVIDENCE_QUOTE_MISMATCH");
        }
    }

    private void validateSpeakerPolicy(
            FactView target,
            Authority authority,
            List<EvidenceSpanCandidate> evidence,
            Map<UUID, TranscriptItemView> effective) {
        boolean doctor = evidence.stream()
                .map(EvidenceSpanCandidate::transcriptItemId)
                .map(effective::get)
                .anyMatch(item -> item != null && "DOCTOR".equals(item.speakerType()));
        boolean explicit = evidence.stream()
                .map(EvidenceSpanCandidate::transcriptItemId)
                .map(effective::get)
                .anyMatch(item -> item != null
                        && ("DOCTOR".equals(item.speakerType()) || "PATIENT".equals(item.speakerType())));

        Authority targetAuthority;
        try {
            targetAuthority = Authority.valueOf(target.authority());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_TARGET_AUTHORITY_INVALID");
        }

        if (targetAuthority != Authority.PATIENT_REPORTED && !doctor) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_TARGET_DOCTOR_EVIDENCE_REQUIRED");
        }
        if ((authority == Authority.CLINICIAN_OBSERVED || authority == Authority.CLINICIAN_DECISION) && !doctor) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_DOCTOR_EVIDENCE_REQUIRED");
        }
        if (authority == Authority.PATIENT_REPORTED && !explicit) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_EXPLICIT_SPEAKER_REQUIRED");
        }
    }

    private void validateReason(
            FactView target,
            RetractionReason reason,
            EvidenceSpanCandidate primary,
            TranscriptItemView primaryItem) {
        String quote = normalize(primary.quoteText());
        String concept = normalize(target.conceptText());
        if (concept.isBlank() || !quote.contains(concept)) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_CONCEPT_NOT_IN_EVIDENCE");
        }

        switch (reason) {
            case EXPLICIT_CORRECTION -> requireMarker(
                    quote, CORRECTION_MARKERS, "AI_CLINICAL_FACT_RETRACTION_CORRECTION_NOT_EXPLICIT");
            case EXPLICIT_NEGATION -> {
                if (!hasNegationNearConcept(quote, concept)) {
                    throw invalid("AI_CLINICAL_FACT_RETRACTION_NEGATION_NOT_EXPLICIT");
                }
            }
            case CLINICIAN_CANCELLATION -> {
                FactType type = FactType.valueOf(target.factType());
                if (type != FactType.MEDICATION && type != FactType.ORDER && type != FactType.PLAN) {
                    throw invalid("AI_CLINICAL_FACT_RETRACTION_CANCELLATION_TYPE_INVALID");
                }
                if (primaryItem == null || !"DOCTOR".equals(primaryItem.speakerType())) {
                    throw invalid("AI_CLINICAL_FACT_RETRACTION_CANCELLATION_DOCTOR_REQUIRED");
                }
                requireMarker(
                        quote, CANCELLATION_MARKERS, "AI_CLINICAL_FACT_RETRACTION_CANCELLATION_NOT_EXPLICIT");
            }
        }
    }

    private void requireMarker(String quote, Set<String> markers, String errorCode) {
        if (markers.stream().noneMatch(marker -> containsTokenOrPhrase(quote, marker))) {
            throw invalid(errorCode);
        }
    }

    private boolean hasNegationNearConcept(String quote, String concept) {
        int conceptIndex = quote.indexOf(concept);
        if (conceptIndex < 0) return false;
        String context = quote.substring(
                Math.max(0, conceptIndex - 40),
                Math.min(quote.length(), conceptIndex + concept.length() + 20));
        return NEGATION_MARKERS.stream().anyMatch(marker -> containsTokenOrPhrase(context, marker));
    }

    private boolean containsTokenOrPhrase(String value, String marker) {
        if (marker.indexOf(' ') >= 0) return value.contains(marker);
        return (" " + value + " ").contains(" " + marker + " ");
    }

    private String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'');
        return decomposed.replaceAll("[^a-z0-9%/]+", " ").trim().replaceAll("\\s+", " ");
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
