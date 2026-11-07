package com.joprelys.backend.eco.domain;

import java.util.Locale;

/** Bénéficiaire d'un consentement. */
public enum GranteeType {
    APPLICATION,
    ORGANIZATION,
    PRACTITIONER;

    public String contractValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
