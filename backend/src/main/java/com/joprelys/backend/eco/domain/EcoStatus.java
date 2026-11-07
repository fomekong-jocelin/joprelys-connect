package com.joprelys.backend.eco.domain;

/** Statut commun d'une liaison et d'un consentement ({@code active | revoked} dans les contrats). */
public enum EcoStatus {
    ACTIVE,
    REVOKED;

    public String contractValue() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
