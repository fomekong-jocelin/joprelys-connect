package com.joprelys.backend.medication.reference;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MedicationReferenceDuplicateDetector {

    private static final Logger log = LoggerFactory.getLogger(
            MedicationReferenceDuplicateDetector.class);

    private final MedicationReferenceService referenceService;

    public MedicationReferenceDuplicateDetector(
            MedicationReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    public Optional<MedicationReferenceMatch> findEquivalent(
            String proposedName,
            List<String> existingNames) {
        if (proposedName == null || proposedName.isBlank()
                || existingNames == null || existingNames.isEmpty()) {
            return Optional.empty();
        }

        for (String existingName : existingNames) {
            if (existingName == null || existingName.isBlank()) {
                continue;
            }
            MedicationEquivalence equivalence = referenceService.compare(
                    proposedName.trim(), existingName.trim());
            if (equivalence.status() != MedicationEquivalence.Status.SAME_CONCEPT) {
                continue;
            }
            log.info(
                    "Medication reference duplicate evidence source={} concept={} proposed={} existing={}",
                    equivalence.source(),
                    equivalence.leftConceptId(),
                    proposedName,
                    existingName);
            return Optional.of(new MedicationReferenceMatch(
                    proposedName.trim(),
                    existingName.trim(),
                    equivalence.source(),
                    equivalence.leftConceptId(),
                    equivalence.explanation()));
        }
        return Optional.empty();
    }
}
