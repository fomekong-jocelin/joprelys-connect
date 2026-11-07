package com.joprelys.backend.eco.application;

import java.util.Optional;

/**
 * Appelant authentifié par Joprelys ID.
 *
 * @param subject     sujet du jeton ({@code sub})
 * @param clientId    client OAuth à l'origine du jeton ({@code azp})
 * @param application application de l'écosystème correspondant au client (configuration)
 * @param dpu         DPU porté par le jeton (scope {@code eco:patient-link}), absent pour un compte de service
 */
public record EcoCaller(String subject, String clientId, String application, Optional<String> dpu) {
}
