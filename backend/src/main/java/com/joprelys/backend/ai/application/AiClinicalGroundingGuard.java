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
        if (parsed == null) {
            return null;
        }

        boolean prescriptionClarification = PRESCRIPTION.equals(resolvedClarificationField);
        boolean ungroundedPrescriptionClarification = parsed.needsClarification()
                && parsed.clarification() != null
                && PRESCRIPTION.equals(parsed.clarification().field())
                && !prescriptionClarification
                && !hasExplicitMedicationSignal(latestClinicianUtterance);

        List<ParsedChange> grounded = new ArrayList<>();
        boolean prescriptionDropped = false;

        for (ParsedChange change : parsed.changes()) {
            if (!isPrescriptionChange(change)) {
                grounded.add(change);
                continue;
            }

            boolean allowed = prescriptionClarification
                    || ("SET".equals(change.operation())
                    && hasExplicitDrugGrounding(change, latestClinicianUtterance))
                    || ("CLEAR".equals(change.operation())
                    && hasExplicitPrescriptionCancellation(latestClinicianUtterance));

            if (allowed) {
                grounded.add(change);
            } else {
                prescriptionDropped = true;
                log.warn("Blocked ungrounded AI prescription proposal; no explicit medication evidence in latest clinician utterance");
            }
        }

        if (!prescriptionDropped && !ungroundedPrescriptionClarification) {
            return parsed;
        }

        if (ungroundedPrescriptionClarification) {
            log.warn("Blocked ungrounded AI prescription clarification; no explicit medication signal in latest clinician utterance");
        }

        boolean english = "en".equalsIgnoreCase(locale);
        String safeMessage = grounded.isEmpty()
                ? (english
                        ? "I captured your dictation. No medication was added without an explicit prescription."
                        : "J’ai pris en compte votre dictée. Aucun médicament n’a été ajouté sans prescription explicite.")
                : (english
                        ? "I structured only what you explicitly dictated. No medication was added without an explicit prescription."
                        : "J’ai structuré les éléments explicitement dictés. Aucun médicament n’a été ajouté sans prescription explicite.");

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
                .replaceAll("[^a-z0-9.,]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
