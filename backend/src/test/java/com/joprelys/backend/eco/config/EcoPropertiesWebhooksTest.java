package com.joprelys.backend.eco.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EcoPropertiesWebhooksTest {

    private static final String SECRET = "ci-test-eco-webhook-secret-not-for-production";

    @Test
    void subscriberWithoutUrlOrSecretIsIgnored() {
        EcoProperties.Webhooks webhooks = webhooks(new EcoProperties.Subscriber("", ""));
        assertThat(webhooks.subscriber("allopharma")).isEmpty();
        assertThat(webhooks.subscriber("inconnue")).isEmpty();
    }

    @Test
    void httpsOrLocalSubscriberIsAccepted() {
        assertThat(webhooks(new EcoProperties.Subscriber("https://api.allopharma.test/api/eco/v1/webhooks", SECRET))
                .subscriber("allopharma")).isPresent();
        assertThat(webhooks(new EcoProperties.Subscriber("http://localhost:8080/api/eco/v1/webhooks", SECRET))
                .subscriber("allopharma")).isPresent();
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> webhooks(new EcoProperties.Subscriber("https://api.allopharma.test/w", "trop-court")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("trop court")
                .hasMessageNotContaining("trop-court");
    }

    @Test
    void plainHttpToARemoteHostFailsFast() {
        assertThatThrownBy(() -> webhooks(new EcoProperties.Subscriber("http://api.allopharma.test/w", SECRET)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    private static EcoProperties.Webhooks webhooks(EcoProperties.Subscriber subscriber) {
        return new EcoProperties.Webhooks(Map.of("allopharma", subscriber), null);
    }
}
