package com.joprelys.backend.medication.reference;

import java.util.List;

public interface MedicationReferencePort {

    String source();

    default int priority() {
        return 100;
    }

    List<MedicationConcept> findCandidates(String query, int maximum);
}
