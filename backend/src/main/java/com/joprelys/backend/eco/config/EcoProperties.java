package com.joprelys.backend.eco.config;

import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'API écosystème (ECO-20261107, ADR-20261107-JOPRELYS-ID-KEYCLOAK).
 *
 * @param sourceTenant identifiant court de l'établissement émetteur, publié dans {@code meta.source}
 *                     sous la forme {@code joprelys:<sourceTenant>}
 * @param identity     fournisseur d'identité de liaison (Joprelys ID)
 * @param applications client OAuth ({@code azp}) → code de l'application de l'écosystème autorisée
 */
@ConfigurationProperties(prefix = "joprelys.eco")
public record EcoProperties(String sourceTenant, Identity identity, Map<String, String> applications) {

    public EcoProperties {
        if (sourceTenant == null || sourceTenant.isBlank() || identity == null) {
            throw new IllegalStateException(
                    "Configuration joprelys.eco incomplète : source-tenant et identity (issuer-uri, jwk-set-uri, audience) sont requis");
        }
        applications = applications == null ? Map.of() : Map.copyOf(applications);
    }

    /** Valeur de {@code meta.source} et de {@code issuer} dans les contrats. */
    public String source() {
        return "joprelys:" + sourceTenant;
    }

    public Optional<String> applicationFor(String clientId) {
        return Optional.ofNullable(clientId).map(applications::get);
    }

    public record Identity(String issuerUri, String jwkSetUri, String audience) {
    }
}
