package com.joprelys.backend.ai.facts.domain;

public final class ClinicalFactTypes {

    private ClinicalFactTypes() {
    }

    public enum FactType {
        SYMPTOM,
        VITAL,
        MEDICATION,
        ALLERGY,
        HISTORY,
        ASSESSMENT,
        PLAN,
        ORDER
    }

    public enum Authority {
        PATIENT_REPORTED,
        CLINICIAN_OBSERVED,
        CLINICIAN_DECISION
    }

    public enum Polarity {
        POSITIVE,
        NEGATIVE,
        UNCERTAIN
    }

    public enum Laterality {
        LEFT,
        RIGHT,
        BILATERAL,
        UNSPECIFIED
    }

    public enum FactStatus {
        ASSERTED,
        RETRACTED
    }
}
