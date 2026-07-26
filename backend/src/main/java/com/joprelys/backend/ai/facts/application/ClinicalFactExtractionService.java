package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactExtractionEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactExtractionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class ClinicalFactExtractionService {

    static final String EXTRACTOR_VERSION = "facts-v1";
    private static final int MAX_BATCH_ITEMS = 8;
    private static final String SCHEMA_NAME = "joprelys_clinical_facts_v1";

    private final AmbientTranscriptLedgerService transcriptLedgerService;
    private final ClinicalFactLedgerService factLedgerService;
    private final ClinicalFactExtractionRepository extractionRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    public ClinicalFactExtractionService(
            AmbientTranscriptLedgerService transcriptLedgerService,
            ClinicalFactLedgerService factLedgerService,
            ClinicalFactExtractionRepository extractionRepository,
            AiProvider aiProvider,
            ObjectMapper objectMapper) {
        this.transcriptLedgerService = transcriptLedgerService;
        this.factLedgerService = factLedgerService;
        this.extractionRepository = extractionRepository;
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper;
    }

    public ExtractionReport extractNewFacts(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        var transcript = transcriptLedgerService.listFinal(visitId, organizationId);
        List<TranscriptItemView> eligible = transcript.items().stream()
                .filter(item -> "FINAL".equals(item.status()))
                .filter(item -> "DOCTOR".equals(item.speakerType()) || "PATIENT".equals(item.speakerType()))
                .toList();
        int unspecifiedSkipped = (int) transcript.items().stream()
                .filter(item -> "FINAL".equals(item.status()))
                .filter(item -> !"DOCTOR".equals(item.speakerType()) && !"PATIENT".equals(item.speakerType()))
                .count();

        List<TranscriptItemView> pending = new ArrayList<>();
        int alreadyProcessed = 0;
        for (TranscriptItemView item : eligible) {
            var existing = extractionRepository
                    .findByVisitIdAndTranscriptItemIdAndExtractorVersion(
                            visitId,
                            item.id(),
                            EXTRACTOR_VERSION);
            if (existing.isPresent()) {
                if (!existing.get().getTranscriptSha256().equals(sha256(item.text()))) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "AI_CLINICAL_FACT_EXTRACTION_HASH_MISMATCH");
                }
                alreadyProcessed++;
            } else {
                pending.add(item);
            }
        }

        int processedItems = 0;
        int candidateCount = 0;
        int acceptedCount = 0;
        int rejectedCount = 0;
        List<FactView> acceptedFacts = new ArrayList<>();
        List<RejectionView> rejections = new ArrayList<>();
        String lastModel = null;

        for (int offset = 0; offset < pending.size(); offset += MAX_BATCH_ITEMS) {
            List<TranscriptItemView> batch = pending.subList(
                    offset,
                    Math.min(offset + MAX_BATCH_ITEMS, pending.size()));
            BatchResult result = extractBatch(
                    visitId,
                    userId,
                    organizationId,
                    batch);
            processedItems += batch.size();
            candidateCount += result.candidateCount();
            acceptedCount += result.acceptedFacts().size();
            rejectedCount += result.rejections().size();
            acceptedFacts.addAll(result.acceptedFacts());
            rejections.addAll(result.rejections());
            if (result.model() != null && !result.model().isBlank()) lastModel = result.model();
        }

        return new ExtractionReport(
                visitId,
                processedItems,
                alreadyProcessed,
                unspecifiedSkipped,
                candidateCount,
                acceptedCount,
                rejectedCount,
                lastModel,
                List.copyOf(acceptedFacts),
                List.copyOf(rejections));
    }

    private BatchResult extractBatch(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            List<TranscriptItemView> batch) {
        Map<UUID, TranscriptItemView> byId = new LinkedHashMap<>();
        for (TranscriptItemView item : batch) byId.put(item.id(), item);

        String payload = transcriptPayload(batch);
        String content;
        String model;
        try {
            var response = aiProvider.chatStructured(
                    List.of(AiMessage.user(payload)),
                    systemPrompt(),
                    SCHEMA_NAME,
                    ClinicalFactExtractionSchema.schema());
            content = response.content();
            model = response.model();
        } catch (UnsupportedOperationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_CLINICAL_FACT_STRUCTURED_OUTPUT_UNAVAILABLE",
                    exception);
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_CLINICAL_FACT_EXTRACTION_UPSTREAM_FAILED",
                    exception);
        }

        ExtractionEnvelope envelope;
        try {
            envelope = objectMapper.readValue(content, ExtractionEnvelope.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_CLINICAL_FACT_STRUCTURED_OUTPUT_INVALID",
                    exception);
        }
        if (envelope == null || envelope.facts() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_CLINICAL_FACT_STRUCTURED_OUTPUT_INVALID");
        }

        Map<UUID, List<ExtractedFact>> grouped = new HashMap<>();
        for (ExtractedFact extracted : envelope.facts()) {
            UUID transcriptItemId = parseItemId(extracted.transcriptItemId());
            if (!byId.containsKey(transcriptItemId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI_CLINICAL_FACT_EVIDENCE_OUTSIDE_BATCH");
            }
            grouped.computeIfAbsent(transcriptItemId, ignored -> new ArrayList<>()).add(extracted);
        }

        List<FactView> accepted = new ArrayList<>();
        List<RejectionView> rejected = new ArrayList<>();
        Set<String> candidateKeys = new HashSet<>();

        for (TranscriptItemView item : batch) {
            List<ExtractedFact> candidates = grouped.getOrDefault(item.id(), List.of());
            int itemAccepted = 0;
            int itemRejected = 0;
            for (ExtractedFact extracted : candidates) {
                try {
                    FactCandidate candidate = toCandidate(item, extracted);
                    if (!candidateKeys.add(candidate.sourceEventId())) {
                        throw new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "AI_CLINICAL_FACT_DUPLICATE_CANDIDATE");
                    }
                    accepted.add(factLedgerService.appendValidatedFact(
                            visitId,
                            userId,
                            organizationId,
                            candidate));
                    itemAccepted++;
                } catch (ResponseStatusException exception) {
                    itemRejected++;
                    rejected.add(new RejectionView(
                            item.id(),
                            safeReason(exception),
                            extracted.factType(),
                            extracted.conceptCode()));
                } catch (RuntimeException exception) {
                    itemRejected++;
                    rejected.add(new RejectionView(
                            item.id(),
                            "AI_CLINICAL_FACT_CANDIDATE_INVALID",
                            extracted.factType(),
                            extracted.conceptCode()));
                }
            }
            saveExtractionJournal(
                    visitId,
                    userId,
                    organizationId,
                    item,
                    model,
                    candidates.size(),
                    itemAccepted,
                    itemRejected);
        }

        return new BatchResult(
                envelope.facts().size(),
                List.copyOf(accepted),
                List.copyOf(rejected),
                model);
    }

    private FactCandidate toCandidate(
            TranscriptItemView item,
            ExtractedFact extracted) {
        String quote = requireQuote(extracted.quoteText());
        int quoteStart = item.text().indexOf(quote);
        if (quoteStart < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_CLINICAL_FACT_QUOTE_NOT_EXACT");
        }
        if (item.text().indexOf(quote, quoteStart + 1) >= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_CLINICAL_FACT_QUOTE_AMBIGUOUS");
        }
        int quoteEnd = quoteStart + quote.length();

        FactType factType = FactType.valueOf(extracted.factType());
        Authority authority = Authority.valueOf(extracted.authority());
        if ("PATIENT".equals(item.speakerType()) && authority != Authority.PATIENT_REPORTED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_CLINICAL_FACT_PATIENT_AUTHORITY_INVALID");
        }
        Polarity polarity = Polarity.valueOf(extracted.polarity());
        Laterality laterality = Laterality.valueOf(extracted.laterality());
        String conceptCode = required(extracted.conceptCode(), "AI_CLINICAL_FACT_CONCEPT_CODE_INVALID");
        String conceptText = required(extracted.conceptText(), "AI_CLINICAL_FACT_CONCEPT_INVALID");

        String eventId = sourceEventId(
                item.id(),
                factType,
                conceptCode,
                quoteStart,
                quoteEnd);
        return new FactCandidate(
                eventId,
                factType,
                authority,
                conceptCode,
                conceptText,
                polarity,
                blankToNull(extracted.valuePrimary()),
                blankToNull(extracted.valueSecondary()),
                blankToNull(extracted.unitCode()),
                blankToNull(extracted.temporalityText()),
                laterality,
                blankToNull(extracted.frequencyText()),
                blankToNull(extracted.routeText()),
                FactStatus.ASSERTED,
                null,
                List.of(new EvidenceSpanCandidate(
                        item.id(),
                        quoteStart,
                        quoteEnd,
                        quote,
                        true)));
    }

    private void saveExtractionJournal(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            TranscriptItemView item,
            String model,
            int candidateCount,
            int acceptedCount,
            int rejectedCount) {
        ClinicalFactExtractionEntity entity = new ClinicalFactExtractionEntity(
                organizationId,
                visitId,
                item.id(),
                sha256(item.text()),
                EXTRACTOR_VERSION,
                blankToNull(model),
                candidateCount,
                acceptedCount,
                rejectedCount,
                userId);
        try {
            extractionRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            var existing = extractionRepository
                    .findByVisitIdAndTranscriptItemIdAndExtractorVersion(
                            visitId,
                            item.id(),
                            EXTRACTOR_VERSION)
                    .orElseThrow(() -> exception);
            if (!existing.getTranscriptSha256().equals(sha256(item.text()))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "AI_CLINICAL_FACT_EXTRACTION_HASH_MISMATCH",
                        exception);
            }
        }
    }

    private String transcriptPayload(List<TranscriptItemView> batch) {
        List<Map<String, Object>> items = batch.stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.id().toString(),
                        "speakerType", item.speakerType(),
                        "speakerLabel", item.speakerLabel() == null ? "" : item.speakerLabel(),
                        "locale", item.locale() == null ? "fr" : item.locale(),
                        "text", item.text()))
                .toList();
        try {
            return objectMapper.writeValueAsString(Map.of("items", items));
        } catch (Exception exception) {
            throw new IllegalStateException("AI_CLINICAL_FACT_INPUT_SERIALIZATION_FAILED", exception);
        }
    }

    private String systemPrompt() {
        return """
                You extract atomic clinical facts from canonical medical transcript items for Joprelys.
                You are NOT writing a medical note and you are NOT allowed to infer, summarize, diagnose, normalize away uncertainty, or add missing information.

                Hard rules:
                1. Every output fact must cite exactly one transcriptItemId from the input batch.
                2. quoteText must be an exact contiguous substring of that item's text. Never paraphrase the quote.
                3. Extract only information explicitly stated in quoteText. If there is no explicit clinical fact, return no fact for that item.
                4. Keep each fact atomic. For SYMPTOM, VITAL and MEDICATION, choose a short single-clause quote that binds concept and value/negation/temporality/laterality together. Do not combine comma-separated clinical relations.
                5. Never move a number, dose, unit, duration, side or negation from one concept to another.
                6. PATIENT speaker facts must use PATIENT_REPORTED. Never produce ASSESSMENT, PLAN or ORDER from a PATIENT speaker.
                7. DOCTOR speaker: measured/examined observations use CLINICIAN_OBSERVED; explicit diagnosis/assessment/prescription/order/plan decisions use CLINICIAN_DECISION; a doctor explicitly relaying what the patient reported may use PATIENT_REPORTED.
                8. Do not infer patient identity from an unspecified speaker; unspecified speakers are excluded before this call.
                9. conceptText must itself occur verbatim inside quoteText. conceptCode is an uppercase stable semantic code using letters, digits and underscores.
                10. Preserve polarity. NEGATIVE requires explicit negation in quoteText. UNCERTAIN requires explicit uncertainty.
                11. Preserve laterality only when explicit. Otherwise UNSPECIFIED.
                12. Preserve values; do not convert units. For numeric clinical values, valuePrimary/valueSecondary contain the numeric value tied to the concept. Use null when no explicit structured value exists.
                13. VITAL codes and units when applicable:
                    BLOOD_PRESSURE -> valuePrimary=systolic, valueSecondary=diastolic, unitCode=MMHG
                    HEART_RATE -> unitCode=BPM
                    TEMPERATURE -> unitCode=CELSIUS
                    RESPIRATORY_RATE -> unitCode=BREATHS_MIN
                    OXYGEN_SATURATION -> unitCode=PERCENT
                    WEIGHT -> unitCode=KG
                    HEIGHT -> unitCode=CM
                14. MEDICATION keeps drug name, dose, unit, frequency and route bound to the same explicit quote. Do not invent a prescription from a medication merely reported as current treatment.
                15. Returning an empty facts array is correct when the evidence does not support an atomic fact.
                """;
    }

    private UUID parseItemId(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_CLINICAL_FACT_EVIDENCE_ID_INVALID",
                    exception);
        }
    }

    private String sourceEventId(
            UUID itemId,
            FactType factType,
            String conceptCode,
            int quoteStart,
            int quoteEnd) {
        String canonical = itemId
                + "|" + factType.name()
                + "|" + conceptCode.trim().toUpperCase(Locale.ROOT)
                + "|" + quoteStart
                + "|" + quoteEnd;
        return "extract-v1-" + sha256(canonical);
    }

    private String requireQuote(String value) {
        String normalized = required(value, "AI_CLINICAL_FACT_QUOTE_INVALID");
        if (normalized.length() > 2_000) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_CLINICAL_FACT_QUOTE_INVALID");
        }
        return value;
    }

    private String required(String value, String errorCode) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeReason(ResponseStatusException exception) {
        return exception.getReason() == null || exception.getReason().isBlank()
                ? "AI_CLINICAL_FACT_REJECTED"
                : exception.getReason();
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

    public record ExtractionReport(
            UUID visitId,
            int processedItems,
            int alreadyProcessedItems,
            int unspecifiedSpeakerItems,
            int candidateCount,
            int acceptedCount,
            int rejectedCount,
            String model,
            List<FactView> acceptedFacts,
            List<RejectionView> rejections) {
    }

    public record RejectionView(
            UUID transcriptItemId,
            String reason,
            String factType,
            String conceptCode) {
    }

    private record BatchResult(
            int candidateCount,
            List<FactView> acceptedFacts,
            List<RejectionView> rejections,
            String model) {
    }

    public record ExtractionEnvelope(List<ExtractedFact> facts) {
    }

    public record ExtractedFact(
            String transcriptItemId,
            String factType,
            String authority,
            String conceptCode,
            String conceptText,
            String polarity,
            String valuePrimary,
            String valueSecondary,
            String unitCode,
            String temporalityText,
            String laterality,
            String frequencyText,
            String routeText,
            String quoteText) {
    }
}
