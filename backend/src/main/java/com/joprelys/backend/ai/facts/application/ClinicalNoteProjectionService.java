package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.LinkedEvidenceView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteEntryView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteSectionView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deterministic, source-linked projection of the canonical clinical fact ledger.
 *
 * <p>No model is called here. A projection can only contain effective facts whose evidence
 * still belongs to the current FINAL transcript. This is the backend foundation for a
 * Nabla/Abridge-style linked-evidence note without free-form note hallucination.</p>
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class ClinicalNoteProjectionService {

    static final String PROJECTION_SCHEMA_VERSION = "clinical-note-projection-v1";

    private static final List<String> SECTION_ORDER = List.of(
            "HISTORY_OF_PRESENT_ILLNESS",
            "MEDICAL_HISTORY",
            "ALLERGIES",
            "VITALS",
            "ASSESSMENT",
            "MEDICATIONS",
            "ORDERS",
            "PLAN");

    private final ClinicalFactLedgerService factLedgerService;
    private final AmbientTranscriptLedgerService transcriptLedgerService;

    public ClinicalNoteProjectionService(
            ClinicalFactLedgerService factLedgerService,
            AmbientTranscriptLedgerService transcriptLedgerService) {
        this.factLedgerService = factLedgerService;
        this.transcriptLedgerService = transcriptLedgerService;
    }

    @Transactional(readOnly = true)
    public NoteProjectionView project(UUID visitId, UUID organizationId) {
        var transcript = transcriptLedgerService.listFinal(visitId, organizationId);
        Map<UUID, TranscriptItemView> transcriptById = transcript.items().stream()
                .filter(item -> "FINAL".equals(item.status()))
                .collect(Collectors.toMap(
                        TranscriptItemView::id,
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));
        Set<UUID> transcriptIds = Set.copyOf(transcriptById.keySet());

        var facts = factLedgerService.listEffectiveForTranscriptSnapshot(
                visitId,
                organizationId,
                transcriptIds);

        Map<String, List<NoteEntryView>> sections = new LinkedHashMap<>();
        SECTION_ORDER.forEach(section -> sections.put(section, new ArrayList<>()));

        long maxFactSequence = 0;
        for (FactView fact : facts.facts()) {
            maxFactSequence = Math.max(maxFactSequence, fact.sequence());
            sections.get(sectionFor(fact.factType())).add(toEntry(fact, transcriptById));
        }

        List<NoteSectionView> renderedSections = SECTION_ORDER.stream()
                .map(code -> new NoteSectionView(code, List.copyOf(sections.get(code))))
                .filter(section -> !section.entries().isEmpty())
                .toList();

        String version = projectionVersion(visitId, renderedSections);
        return new NoteProjectionView(
                visitId,
                version,
                maxFactSequence,
                renderedSections);
    }

    private NoteEntryView toEntry(
            FactView fact,
            Map<UUID, TranscriptItemView> transcriptById) {
        List<LinkedEvidenceView> evidence = fact.evidence().stream()
                .map(span -> {
                    TranscriptItemView item = transcriptById.get(span.transcriptItemId());
                    if (item == null) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "AI_CLINICAL_NOTE_PROJECTION_STALE");
                    }
                    if (span.quoteStartChar() < 0
                            || span.quoteEndChar() < span.quoteStartChar()
                            || span.quoteEndChar() > item.text().length()
                            || !item.text()
                                    .substring(span.quoteStartChar(), span.quoteEndChar())
                                    .equals(span.quoteText())) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "AI_CLINICAL_NOTE_EVIDENCE_STALE");
                    }
                    return new LinkedEvidenceView(
                            item.id(),
                            item.speakerType(),
                            item.speakerLabel(),
                            item.startOffsetMs(),
                            item.endOffsetMs(),
                            span.quoteStartChar(),
                            span.quoteEndChar(),
                            span.quoteText(),
                            span.primarySupport());
                })
                .toList();

        if (evidence.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_CLINICAL_NOTE_EVIDENCE_REQUIRED");
        }

        return new NoteEntryView(
                fact.id(),
                fact.sequence(),
                fact.factType(),
                fact.authority(),
                fact.conceptCode(),
                fact.conceptText(),
                fact.polarity(),
                fact.valuePrimary(),
                fact.valueSecondary(),
                fact.unitCode(),
                fact.temporalityText(),
                fact.laterality(),
                fact.frequencyText(),
                fact.routeText(),
                evidence);
    }

    private String sectionFor(String factType) {
        return switch (factType) {
            case "SYMPTOM" -> "HISTORY_OF_PRESENT_ILLNESS";
            case "HISTORY" -> "MEDICAL_HISTORY";
            case "ALLERGY" -> "ALLERGIES";
            case "VITAL" -> "VITALS";
            case "ASSESSMENT" -> "ASSESSMENT";
            case "MEDICATION" -> "MEDICATIONS";
            case "ORDER" -> "ORDERS";
            case "PLAN" -> "PLAN";
            default -> throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI_CLINICAL_NOTE_FACT_TYPE_UNSUPPORTED");
        };
    }

    private String projectionVersion(UUID visitId, List<NoteSectionView> sections) {
        StringBuilder canonical = new StringBuilder(PROJECTION_SCHEMA_VERSION)
                .append('|')
                .append(visitId);
        for (NoteSectionView section : sections) {
            canonical.append("\nS|").append(section.code());
            for (NoteEntryView entry : section.entries()) {
                canonical.append("\nF|")
                        .append(entry.factId()).append('|')
                        .append(entry.factSequence()).append('|')
                        .append(nullSafe(entry.factType())).append('|')
                        .append(nullSafe(entry.authority())).append('|')
                        .append(nullSafe(entry.conceptCode())).append('|')
                        .append(nullSafe(entry.conceptText())).append('|')
                        .append(nullSafe(entry.polarity())).append('|')
                        .append(nullSafe(entry.valuePrimary())).append('|')
                        .append(nullSafe(entry.valueSecondary())).append('|')
                        .append(nullSafe(entry.unitCode())).append('|')
                        .append(nullSafe(entry.temporalityText())).append('|')
                        .append(nullSafe(entry.laterality())).append('|')
                        .append(nullSafe(entry.frequencyText())).append('|')
                        .append(nullSafe(entry.routeText()));
                for (LinkedEvidenceView evidence : entry.evidence()) {
                    canonical.append("\nE|")
                            .append(evidence.transcriptItemId()).append('|')
                            .append(nullSafe(evidence.speakerType())).append('|')
                            .append(nullSafe(evidence.speakerLabel())).append('|')
                            .append(evidence.startOffsetMs()).append('|')
                            .append(evidence.endOffsetMs()).append('|')
                            .append(evidence.quoteStartChar()).append('|')
                            .append(evidence.quoteEndChar()).append('|')
                            .append(evidence.primarySupport()).append('|')
                            .append(nullSafe(evidence.quoteText()));
                }
            }
        }
        return PROJECTION_SCHEMA_VERSION + ":" + sha256(canonical.toString());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
