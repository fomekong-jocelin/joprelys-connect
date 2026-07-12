package com.joprelys.backend.patient.reconciliation.domain;

public enum PatientReconciliationDecision {
    CREATE_NEW_DPU,
    LINK_EXISTING_DPU,
    DEFER,
    CORRECT_LINK
}
