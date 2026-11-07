package com.joprelys.backend.eco.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EcoWebhookSignerTest {

    private static final String SECRET = "ci-test-eco-webhook-secret-not-for-production";

    @Test
    void signsTimestampDotBodyWithHmacSha256() {
        // Vecteur calculé indépendamment (Python hmac/hashlib) : le récepteur AllôPharma doit trouver la même valeur.
        assertThat(EcoWebhookSigner.sign(SECRET, 1794046501L, "{\"specversion\":\"1.0\"}"))
                .isEqualTo("t=1794046501,v1=65358192f93d21bce66d54628d7191b36ba758a2792006297eb8769149838825");
    }

    @Test
    void signatureChangesWithBodyTimestampOrSecret() {
        String reference = EcoWebhookSigner.sign(SECRET, 1794046501L, "{}");
        assertThat(EcoWebhookSigner.sign(SECRET, 1794046501L, "{ }")).isNotEqualTo(reference);
        assertThat(EcoWebhookSigner.sign(SECRET, 1794046502L, "{}")).isNotEqualTo(reference);
        assertThat(EcoWebhookSigner.sign(SECRET + "x", 1794046501L, "{}")).isNotEqualTo(reference);
    }
}
