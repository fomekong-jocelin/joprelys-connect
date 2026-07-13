package com.joprelys.backend.emergency.triage.domain;

public final class EmergencyTriageVocabulary {

    private EmergencyTriageVocabulary() {
    }

    public enum AssessmentType {
        INITIAL,
        REASSESSMENT
    }

    public enum AirwayStatus {
        NOT_ASSESSED,
        PATENT,
        AT_RISK,
        OBSTRUCTED
    }

    public enum BreathingStatus {
        NOT_ASSESSED,
        ADEQUATE,
        DISTRESS,
        FAILURE
    }

    public enum CirculationStatus {
        NOT_ASSESSED,
        STABLE,
        COMPROMISED,
        SHOCK
    }

    public enum DisabilityStatus {
        NOT_ASSESSED,
        ALERT,
        RESPONDS_TO_VOICE,
        RESPONDS_TO_PAIN,
        UNRESPONSIVE
    }

    public enum ExposureStatus {
        NOT_ASSESSED,
        NO_CRITICAL_FINDING,
        TRAUMA,
        HYPOTHERMIA,
        HYPERTHERMIA,
        OTHER
    }

    public enum RecommendedOrientation {
        RESUSCITATION,
        OPERATING_ROOM,
        HOSPITALIZATION,
        CONSULTATION,
        TRANSFER,
        DISCHARGE,
        DEATH
    }
}