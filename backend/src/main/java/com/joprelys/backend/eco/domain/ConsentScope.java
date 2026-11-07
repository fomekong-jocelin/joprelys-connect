package com.joprelys.backend.eco.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Périmètres de partage d'un consentement (§ 2.3 de la feuille de route, eco-contracts v1). */
public enum ConsentScope {
    PRESCRIPTIONS,
    LAB_RESULTS,
    IMMUNIZATIONS,
    APPOINTMENTS,
    EMERGENCY_SUMMARY;

    public String contractValue() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static Optional<ConsentScope> fromContract(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.contractValue().equals(value)).findFirst();
    }
}
