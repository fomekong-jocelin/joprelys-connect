package com.joprelys.backend.eco.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Lien entre le titulaire du compte et la personne dont le DPU est relié. */
public enum LinkRelationship {
    SELF,
    CHILD,
    DEPENDENT_ADULT;

    public String contractValue() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static Optional<LinkRelationship> fromContract(String value) {
        return Arrays.stream(values()).filter(candidate -> candidate.contractValue().equals(value)).findFirst();
    }
}
