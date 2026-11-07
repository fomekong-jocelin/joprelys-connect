package com.joprelys.backend.eco.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Auteur d'une révocation. */
public enum RevokedBy {
    PATIENT,
    GUARDIAN,
    SYSTEM;

    public String contractValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<RevokedBy> fromContract(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.contractValue().equals(value)).findFirst();
    }
}
