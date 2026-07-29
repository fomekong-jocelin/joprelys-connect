package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic grounding guard applied after LLM parsing and before a clinical
 * proposal can be created. The prompt is guidance; this class is enforcement.
 */
final class AiClinicalGroundingGuard {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalGroundingGuard.class);
    private static final String PRESCRIPTION = "prescription";

    private final ObjectMapper objectMapper;

    AiClinicalGroundingGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ParsedResponse enforce(
            ParsedResponse parsed,
            String latestClinicianUtterance,
            String resolvedClarificationField,
            String locale) {
        return enforce(parsed, latestClinicianUtterance, resolvedClarificationField, "LEGACY", locale);
    }

    ParsedResponse enforce(
            ParsedResponse parsed,
            String latestClinicianUtterance,
            String resolvedClarificationField,
            String inputSource,
            String locale) {
        if (parsed == null) {
            return null;
        }

        boolean prescriptionClarification = PRESCRIPTION.equals(resolvedClarificationField);
        boolean trustedClinicianSource = isTrustedClinicianSource(inputSource);
        boolean medicationSignal = hasExplicitMedicationSignal(latestClinicianUtterance);
        boolean prescriptionIntent = hasExplicitPrescriptionIntent(latestClinicianUtterance);
        boolean clarificationGrounded = prescriptionClarification
                || (trustedClinicianSource ? medicationSignal : prescriptionIntent);
        boolean ungroundedPrescriptionClarification = parsed.needsClarification()
                && parsed.clarification() != null
                && PRESCRIPTION.equals(parsed.clarification().field())
                && !clarificationGrounded;

        List<ParsedChange> grounded = new ArrayList<>();
        boolean prescriptionDropped = false;

        for (ParsedChange change : parsed.changes()) {
            if (!isPrescriptionChange(change)) {
                grounded.add(change);
                continue;
            }

            boolean explicitDrug = hasExplicitDrugGrounding(change, latestClinicianUtterance);
            boolean allowedSet = "SET".equals(change.operation())
                    && explicitDrug
                    && (trustedClinicianSource || prescriptionIntent);
            boolean allowed = prescriptionClarification
                    || allowedSet
                    || ("CLEAR".equals(change.operation())
                    && hasExplicitPrescriptionCancellation(latestClinicianUtterance));

            if (allowed) {
                grounded.add(change);
            } else {
                prescriptionDropped = true;
                log.warn(
                        "Blocked ungrounded AI prescription proposal source={} explicitDrug={} prescriptionIntent={}",
                        inputSource,
                        explicitDrug,
                        prescriptionIntent);
            }
        }

        if (!prescriptionDropped && !ungroundedPrescriptionClarification) {
            return parsed;
        }

        if (ungroundedPrescriptionClarification) {
            log.warn(
                    "Blocked ungrounded AI prescription clarification source={} medicationSignal={} prescriptionIntent={}",
                    inputSource,
                    medicationSignal,
                    prescriptionIntent);
        }

        boolean english = "en".equalsIgnoreCase(locale);
        String safeMessage = grounded.isEmpty()
                ? (english
                        ? "I captured your dictation. No medication was added without an explicit clinician prescription."
                        : "J’ai pris en compte la dictée. Aucun médicament n’a été ajouté sans prescription explicite du médecin.")
                : (english
                        ? "I structured only the explicitly grounded information. No medication was added without an explicit clinician prescription."
                        : "J’ai structuré uniquement les éléments explicitement fondés. Aucun médicament n’a été ajouté sans prescription explicite du médecin.");

        return new ParsedResponse(
                List.copyOf(grounded),
                safeMessage,
                ungroundedPrescriptionClarification ? false : parsed.needsClarification(),
                ungroundedPrescriptionClarification ? null : parsed.clarification());
    }

    private boolean isPrescriptionChange(ParsedChange change) {
        return PRESCRIPTION.equals(change.field());
    }

    @SuppressWarnings("unchecked")
    private boolean hasExplicitDrugGrounding(ParsedChange change, String utterance) {
        if (utterance == null || utterance.isBlank() || change.proposedValue() == null) {
            return false;
        }
        try {
            Object parsed = objectMapper.readValue(change.proposedValue(), Object.class);
            if (!(parsed instanceof List<?> list) || list.isEmpty()) {
                return false;
            }
            String normalizedUtterance = normalize(utterance);
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    return false;
                }
                Object rawDrugName = map.get("drugName");
                if (!(rawDrugName instanceof String drugName) || drugName.isBlank()) {
                    return false;
                }
                if (!containsDrugName(normalizedUtterance, normalize(drugName))) {
                    return false;
                }
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean containsDrugName(String utterance, String drugName) {
        if (drugName.length() < 3) {
            return false;
        }
        if (utterance.contains(drugName)) {
            return true;
        }
        String[] tokens = drugName.split("\\s+");
        int meaningful = 0;
        for (String token : tokens) {
            if (token.length() < 3) {
                continue;
            }
            meaningful++;
            if (!utterance.contains(token)) {
                return false;
            }
        }
        return meaningful > 0;
    }

    /**
     * Dictation is explicitly clinician-authored, so a medication mention may be
     * structured even when the doctor omits the words "je prescris" on each line.
     * Realtime conversation is speaker-unverified and is therefore excluded here.
     */
    private boolean isTrustedClinicianSource(String inputSource) {
        if (inputSource == null) return false;
        return switch (inputSource.toUpperCase(Locale.ROOT)) {
            case "DICTATION", "TEXT", "AUDIO", "CLARIFICATION", "FINAL_REVIEW", "LEGACY" -> true;
            default -> false;
        };
    }

    private boolean hasExplicitMedicationSignal(String utterance) {
        String normalized = normalize(utterance);
        if (normalized.isBlank()) {
            return false;
        }
        return containsAny(normalized,
                "prescris", "prescrire", "prescription", "ordonnance", "medicament", "traitement",
                "comprime", "gelule", "sirop", "injection", "dose", "posologie", "voie orale",
                "drug", "medication", "medicine", "tablet", "capsule", "syrup", "prescribe")
                || normalized.matches(".*\\b\\d+(?:[.,]\\d+)?\\s*(mg|g|ml|mcg|ug|ui)\\b.*");
    }

    /** Speaker-unverified realtime requires an explicit clinician prescribing act. */
    private boolean hasExplicitPrescriptionIntent(String utterance) {
        String normalized = normalize(utterance);
        if (normalized.isBlank()) return false;
        return containsAny(normalized,
                "je prescris", "je lui prescris", "nous prescrivons", "prescrire", "prescription de",
                "j ordonne", "nous ordonnons", "ordonnance de", "mettre sur l ordonnance",
                "i prescribe", "we prescribe", "prescribe", "prescription for", "order medication");
    }

    private boolean hasExplicitPrescriptionCancellation(String utterance) {
        String normalized = normalize(utterance);
        boolean cancellation = containsAny(normalized,
                "annule", "annuler", "supprime", "supprimer", "retire", "retirer",
                "arrete", "arreter", "stop", "cancel", "remove", "discontinue");
        boolean medicationContext = containsAny(normalized,
                "ordonnance", "prescription", "medicament", "traitement", "drug", "medication", "medicine");
        return cancellation && medicationContext;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("(?<=\\d)(?=[a-z])", " ")
                .replaceAll("(?<=[a-z])(?=\\d)", " ")
                .replaceAll("[^a-z0-9.,]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
