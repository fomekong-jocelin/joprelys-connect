package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ClinicalFactEvidenceValidator {

    private static final Pattern EVENT_ID = Pattern.compile("[A-Za-z0-9._:-]{1,180}");
    private static final Pattern CONCEPT_CODE = Pattern.compile("[A-Z0-9_:-]{1,64}");
    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])\\d+(?:[.,]\\d+)?");
    private static final Pattern BLOOD_PRESSURE = Pattern.compile(
            "(\\d{2,3})\\s*(?:/|sur|over)\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOSE = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(mg|g|mcg|ug|µg|ml|l|ui|iu)\\b", Pattern.CASE_INSENSITIVE);
    private static final Set<String> NEGATIONS = Set.of(
            "pas", "sans", "aucun", "aucune", "non", "nie", "nient",
            "deny", "denies", "no", "not", "without");
    private static final Set<String> UNCERTAINTY = Set.of(
            "possible", "possiblement", "probable", "probablement", "suspect",
            "suspecte", "suspected", "likely", "maybe");
    private static final Map<String, VitalDefinition> VITALS = vitalDefinitions();

    private final AmbientTranscriptLedgerService transcriptLedgerService;

    public ClinicalFactEvidenceValidator(AmbientTranscriptLedgerService transcriptLedgerService) {
        this.transcriptLedgerService = transcriptLedgerService;
    }

    public ValidatedFact validate(UUID visitId, UUID organizationId, FactCandidate candidate) {
        validateShape(candidate);
        Map<UUID, TranscriptItemView> effective = new HashMap<>();
        transcriptLedgerService.listFinal(visitId, organizationId).items()
                .forEach(item -> effective.put(item.id(), item));

        List<ValidatedEvidence> evidence = new ArrayList<>();
        for (EvidenceSpanCandidate span : candidate.evidence()) {
            TranscriptItemView item = effective.get(span.transcriptItemId());
            if (item == null || !"FINAL".equals(item.status())) {
                throw conflict("AI_CLINICAL_FACT_EVIDENCE_NOT_EFFECTIVE");
            }
            validateExactSpan(span, item);
            evidence.add(new ValidatedEvidence(span, item));
        }
        validateSpeakerPolicy(candidate.authority(), evidence);
        ValidatedEvidence primary = evidence.stream()
                .filter(value -> value.span().primarySupport())
                .findFirst()
                .orElseThrow(() -> invalid("AI_CLINICAL_FACT_PRIMARY_EVIDENCE_REQUIRED"));
        validateRelationship(candidate, primary.span().quoteText());
        return new ValidatedFact(candidate, List.copyOf(evidence));
    }

    private void validateShape(FactCandidate candidate) {
        if (candidate == null || candidate.factType() == null || candidate.authority() == null
                || candidate.polarity() == null || candidate.laterality() == null || candidate.status() == null) {
            throw invalid("AI_CLINICAL_FACT_INVALID");
        }
        if (candidate.sourceEventId() == null || !EVENT_ID.matcher(candidate.sourceEventId().trim()).matches()) {
            throw invalid("AI_CLINICAL_FACT_EVENT_ID_INVALID");
        }
        if (candidate.conceptCode() == null
                || !CONCEPT_CODE.matcher(candidate.conceptCode().trim().toUpperCase(Locale.ROOT)).matches()) {
            throw invalid("AI_CLINICAL_FACT_CONCEPT_CODE_INVALID");
        }
        requireText(candidate.conceptText(), 256, "AI_CLINICAL_FACT_CONCEPT_INVALID");
        optionalText(candidate.valuePrimary(), 128);
        optionalText(candidate.valueSecondary(), 128);
        optionalText(candidate.unitCode(), 32);
        optionalText(candidate.temporalityText(), 256);
        optionalText(candidate.frequencyText(), 128);
        optionalText(candidate.routeText(), 64);
        if (candidate.evidence() == null || candidate.evidence().isEmpty() || candidate.evidence().size() > 8) {
            throw invalid("AI_CLINICAL_FACT_EVIDENCE_INVALID");
        }
        if (candidate.evidence().stream().filter(EvidenceSpanCandidate::primarySupport).count() != 1) {
            throw invalid("AI_CLINICAL_FACT_PRIMARY_EVIDENCE_INVALID");
        }
        if (candidate.status() == FactStatus.RETRACTED && candidate.supersedesFactId() == null) {
            throw invalid("AI_CLINICAL_FACT_RETRACTION_TARGET_REQUIRED");
        }
    }

    private void validateExactSpan(EvidenceSpanCandidate span, TranscriptItemView item) {
        if (span == null || span.transcriptItemId() == null || span.quoteText() == null) {
            throw invalid("AI_CLINICAL_FACT_EVIDENCE_INVALID");
        }
        String text = item.text();
        if (text == null || span.quoteStartChar() < 0 || span.quoteEndChar() <= span.quoteStartChar()
                || span.quoteEndChar() > text.length() || span.quoteText().length() > 2_000) {
            throw invalid("AI_CLINICAL_FACT_EVIDENCE_RANGE_INVALID");
        }
        if (!text.substring(span.quoteStartChar(), span.quoteEndChar()).equals(span.quoteText())) {
            throw conflict("AI_CLINICAL_FACT_EVIDENCE_QUOTE_MISMATCH");
        }
    }

    private void validateSpeakerPolicy(Authority authority, List<ValidatedEvidence> evidence) {
        boolean doctor = evidence.stream().anyMatch(value -> "DOCTOR".equals(value.item().speakerType()));
        boolean explicit = evidence.stream().anyMatch(value ->
                "DOCTOR".equals(value.item().speakerType()) || "PATIENT".equals(value.item().speakerType()));
        if ((authority == Authority.CLINICIAN_OBSERVED || authority == Authority.CLINICIAN_DECISION) && !doctor) {
            throw invalid("AI_CLINICAL_FACT_DOCTOR_EVIDENCE_REQUIRED");
        }
        if (authority == Authority.PATIENT_REPORTED && !explicit) {
            throw invalid("AI_CLINICAL_FACT_EXPLICIT_SPEAKER_REQUIRED");
        }
    }

    private void validateRelationship(FactCandidate candidate, String rawQuote) {
        String quote = normalize(rawQuote);
        String concept = normalize(candidate.conceptText());
        if (concept.isBlank() || !quote.contains(concept)) {
            throw invalid("AI_CLINICAL_FACT_CONCEPT_NOT_IN_EVIDENCE");
        }
        if (isAtomic(candidate.factType()) && containsClauseBoundary(rawQuote)) {
            throw invalid("AI_CLINICAL_FACT_RELATION_AMBIGUOUS");
        }

        validatePolarity(candidate.polarity(), quote, concept);
        validateLaterality(candidate.laterality(), quote);
        requireOptionalInQuote(candidate.temporalityText(), quote, "AI_CLINICAL_FACT_TEMPORALITY_NOT_IN_EVIDENCE");
        requireOptionalInQuote(candidate.frequencyText(), quote, "AI_CLINICAL_FACT_FREQUENCY_NOT_IN_EVIDENCE");
        requireOptionalInQuote(candidate.routeText(), quote, "AI_CLINICAL_FACT_ROUTE_NOT_IN_EVIDENCE");

        switch (candidate.factType()) {
            case VITAL -> validateVital(candidate, quote, concept);
            case MEDICATION -> validateMedication(candidate, quote, concept);
            case SYMPTOM -> validateSymptom(candidate, quote, concept);
            case ALLERGY, HISTORY, ASSESSMENT, PLAN, ORDER -> validateVerbatimAttributes(candidate, quote);
        }
    }

    private boolean isAtomic(FactType type) {
        return type == FactType.VITAL || type == FactType.MEDICATION || type == FactType.SYMPTOM;
    }

    private void validateVital(FactCandidate candidate, String quote, String concept) {
        VitalDefinition definition = VITALS.get(candidate.conceptCode().trim().toUpperCase(Locale.ROOT));
        if (definition == null
                || definition.synonyms().stream().noneMatch(value -> normalize(value).equals(concept))) {
            throw invalid("AI_CLINICAL_FACT_VITAL_CONCEPT_INVALID");
        }
        if (candidate.valuePrimary() == null || candidate.valuePrimary().isBlank()) {
            throw invalid("AI_CLINICAL_FACT_VITAL_VALUE_REQUIRED");
        }
        String unit = candidate.unitCode() == null ? "" : candidate.unitCode().trim().toUpperCase(Locale.ROOT);
        if (!definition.allowedUnits().contains(unit)) {
            throw invalid("AI_CLINICAL_FACT_VITAL_UNIT_INVALID");
        }
        int conceptIndex = quote.indexOf(concept);
        if (definition.bloodPressure()) {
            Measurement nearest = nearestPair(BLOOD_PRESSURE.matcher(quote), conceptIndex);
            if (nearest == null || !sameNumber(candidate.valuePrimary(), nearest.primary())
                    || !sameNumber(candidate.valueSecondary(), nearest.secondary())) {
                throw invalid("AI_CLINICAL_FACT_VITAL_RELATION_MISMATCH");
            }
            return;
        }
        if (candidate.valueSecondary() != null && !candidate.valueSecondary().isBlank()) {
            throw invalid("AI_CLINICAL_FACT_VITAL_SECONDARY_UNEXPECTED");
        }
        Measurement nearest = nearestSingle(NUMBER.matcher(quote), conceptIndex);
        if (nearest == null || !sameNumber(candidate.valuePrimary(), nearest.primary())) {
            throw invalid("AI_CLINICAL_FACT_VITAL_RELATION_MISMATCH");
        }
    }

    private void validateMedication(FactCandidate candidate, String quote, String concept) {
        if (candidate.valuePrimary() == null || candidate.valuePrimary().isBlank()) {
            validateVerbatimAttributes(candidate, quote);
            return;
        }
        if (candidate.unitCode() == null || candidate.unitCode().isBlank()) {
            throw invalid("AI_CLINICAL_FACT_MEDICATION_UNIT_REQUIRED");
        }
        DoseMeasurement nearest = nearestDose(DOSE.matcher(quote), quote.indexOf(concept));
        if (nearest == null || !sameNumber(candidate.valuePrimary(), nearest.value())
                || !canonicalDoseUnit(candidate.unitCode()).equals(canonicalDoseUnit(nearest.unit()))) {
            throw invalid("AI_CLINICAL_FACT_MEDICATION_RELATION_MISMATCH");
        }
        if (candidate.valueSecondary() != null && !candidate.valueSecondary().isBlank()) {
            throw invalid("AI_CLINICAL_FACT_MEDICATION_SECONDARY_UNEXPECTED");
        }
    }

    private void validateSymptom(FactCandidate candidate, String quote, String concept) {
        validateVerbatimAttributes(candidate, quote);
        boolean negated = hasNegationNearConcept(quote, concept);
        if ((candidate.polarity() == Polarity.NEGATIVE && !negated)
                || (candidate.polarity() == Polarity.POSITIVE && negated)) {
            throw invalid("AI_CLINICAL_FACT_NEGATION_RELATION_MISMATCH");
        }
    }

    private void validateVerbatimAttributes(FactCandidate candidate, String quote) {
        requireOptionalInQuote(candidate.valuePrimary(), quote, "AI_CLINICAL_FACT_VALUE_NOT_IN_EVIDENCE");
        requireOptionalInQuote(candidate.valueSecondary(), quote, "AI_CLINICAL_FACT_VALUE_NOT_IN_EVIDENCE");
        if (candidate.unitCode() != null && !candidate.unitCode().isBlank()) {
            String unit = normalize(candidate.unitCode()).replace("percent", "%");
            if (!quote.contains(unit) && !(unit.equals("%") && quote.contains("pour cent"))) {
                throw invalid("AI_CLINICAL_FACT_UNIT_NOT_IN_EVIDENCE");
            }
        }
    }

    private void validatePolarity(Polarity polarity, String quote, String concept) {
        if (polarity == Polarity.NEGATIVE && !hasNegationNearConcept(quote, concept)) {
            throw invalid("AI_CLINICAL_FACT_NEGATION_NOT_IN_EVIDENCE");
        }
        if (polarity == Polarity.UNCERTAIN && tokens(quote).stream().noneMatch(UNCERTAINTY::contains)) {
            throw invalid("AI_CLINICAL_FACT_UNCERTAINTY_NOT_IN_EVIDENCE");
        }
    }

    private void validateLaterality(Laterality laterality, String quote) {
        if (laterality == Laterality.UNSPECIFIED) return;
        Set<String> expected = switch (laterality) {
            case LEFT -> Set.of("gauche", "left");
            case RIGHT -> Set.of("droit", "droite", "right");
            case BILATERAL -> Set.of("bilateral", "bilaterale", "bilateralement", "deux cotes");
            case UNSPECIFIED -> Set.of();
        };
        if (expected.stream().noneMatch(quote::contains)) {
            throw invalid("AI_CLINICAL_FACT_LATERALITY_NOT_IN_EVIDENCE");
        }
    }

    private boolean hasNegationNearConcept(String quote, String concept) {
        int conceptIndex = quote.indexOf(concept);
        if (conceptIndex < 0) return false;
        String prefix = quote.substring(Math.max(0, conceptIndex - 35), conceptIndex);
        return tokens(prefix).stream().anyMatch(NEGATIONS::contains)
                || normalize(quote.substring(
                        Math.max(0, conceptIndex - 8),
                        Math.min(quote.length(), conceptIndex + concept.length() + 8)))
                        .contains("sans " + concept);
    }

    private boolean containsClauseBoundary(String quote) {
        if (quote.indexOf(';') >= 0 || quote.indexOf('\n') >= 0 || quote.indexOf('!') >= 0 || quote.indexOf('?') >= 0) {
            return true;
        }
        for (int index = 0; index < quote.length(); index++) {
            if (quote.charAt(index) != ',') continue;
            boolean decimalComma = index > 0 && index + 1 < quote.length()
                    && Character.isDigit(quote.charAt(index - 1))
                    && Character.isDigit(quote.charAt(index + 1));
            if (!decimalComma) return true;
        }
        return false;
    }

    private Measurement nearestPair(Matcher matcher, int conceptIndex) {
        List<Measurement> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(new Measurement(matcher.group(1), matcher.group(2), Math.abs(conceptIndex - matcher.start())));
        }
        return values.stream().min(Comparator.comparingInt(Measurement::distance)).orElse(null);
    }

    private Measurement nearestSingle(Matcher matcher, int conceptIndex) {
        List<Measurement> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(new Measurement(matcher.group(), null, Math.abs(conceptIndex - matcher.start())));
        }
        return values.stream().min(Comparator.comparingInt(Measurement::distance)).orElse(null);
    }

    private DoseMeasurement nearestDose(Matcher matcher, int conceptIndex) {
        List<DoseMeasurement> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(new DoseMeasurement(matcher.group(1), matcher.group(2), Math.abs(conceptIndex - matcher.start())));
        }
        return values.stream().min(Comparator.comparingInt(DoseMeasurement::distance)).orElse(null);
    }

    private boolean sameNumber(String expected, String actual) {
        if (expected == null || actual == null) return false;
        try {
            return new BigDecimal(expected.trim().replace(',', '.'))
                    .compareTo(new BigDecimal(actual.trim().replace(',', '.'))) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String canonicalDoseUnit(String value) {
        if (value == null) return "";
        return normalize(value).replace("µ", "u").replace("mcg", "ug").replace("iu", "ui")
                .toUpperCase(Locale.ROOT);
    }

    private void requireOptionalInQuote(String value, String quote, String code) {
        if (value != null && !value.isBlank() && !quote.contains(normalize(value))) throw invalid(code);
    }

    private void requireText(String value, int max, String code) {
        if (value == null || value.isBlank() || value.trim().length() > max) throw invalid(code);
    }

    private void optionalText(String value, int max) {
        if (value != null && value.trim().length() > max) throw invalid("AI_CLINICAL_FACT_INVALID");
    }

    private List<String> tokens(String value) {
        return List.of(normalize(value).split("\\s+"));
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^a-z0-9%/.,' -]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private static Map<String, VitalDefinition> vitalDefinitions() {
        Map<String, VitalDefinition> values = new HashMap<>();
        values.put("BLOOD_PRESSURE", vital(true, Set.of("MMHG"),
                "tension", "tension arterielle", "pression arterielle", "blood pressure", "bp", "ta"));
        values.put("HEART_RATE", vital(false, Set.of("BPM"),
                "pouls", "frequence cardiaque", "heart rate", "hr", "fc"));
        values.put("TEMPERATURE", vital(false, Set.of("CELSIUS"), "temperature", "temp"));
        values.put("RESPIRATORY_RATE", vital(false, Set.of("BREATHS_MIN"),
                "frequence respiratoire", "respiratory rate", "fr"));
        values.put("OXYGEN_SATURATION", vital(false, Set.of("PERCENT"),
                "saturation", "sao2", "spo2", "oxygen saturation"));
        values.put("WEIGHT", vital(false, Set.of("KG"), "poids", "weight"));
        values.put("HEIGHT", vital(false, Set.of("CM"), "taille", "height"));
        return Map.copyOf(values);
    }

    private static VitalDefinition vital(boolean bloodPressure, Set<String> units, String... synonyms) {
        return new VitalDefinition(Set.of(synonyms), units, bloodPressure);
    }

    public record ValidatedEvidence(EvidenceSpanCandidate span, TranscriptItemView item) {}
    public record ValidatedFact(FactCandidate candidate, List<ValidatedEvidence> evidence) {}
    private record Measurement(String primary, String secondary, int distance) {}
    private record DoseMeasurement(String value, String unit, int distance) {}
    private record VitalDefinition(Set<String> synonyms, Set<String> allowedUnits, boolean bloodPressure) {}
}
