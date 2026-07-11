package com.joprelys.backend.patient.domain;

public enum PatientIdentityStatus {
    PROVISIONAL_URGENCY,
    DECLARED,
    VERIFIED,
    MERGED;

    public boolean canTransitionTo(PatientIdentityStatus target) {
        if (target == null || target == this) {
            return target == this;
        }
        return switch (this) {
            case PROVISIONAL_URGENCY -> target == DECLARED || target == VERIFIED || target == MERGED;
            case DECLARED -> target == VERIFIED || target == MERGED;
            case VERIFIED -> target == MERGED;
            case MERGED -> false;
        };
    }
}
