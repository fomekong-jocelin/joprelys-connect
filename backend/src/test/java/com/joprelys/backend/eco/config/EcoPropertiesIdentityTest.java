package com.joprelys.backend.eco.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** S8 (revue sécurité) : la configuration Joprelys ID échoue fermé au démarrage. */
class EcoPropertiesIdentityTest {

    private static final String REALM = "/realms/joprelys-id";
    private static final String CERTS = REALM + "/protocol/openid-connect/certs";

    @Test
    void httpsOrLocalIdentityIsAccepted() {
        assertThatCode(() -> new EcoProperties.Identity("https://id.joprelys.test" + REALM,
                "https://id.joprelys.test" + CERTS, "joprelys-api")).doesNotThrowAnyException();
        assertThatCode(() -> new EcoProperties.Identity("http://localhost:8180" + REALM,
                "http://localhost:8180" + CERTS, "joprelys-api")).doesNotThrowAnyException();
    }

    @Test
    void missingValueFailsFast() {
        assertThatThrownBy(() -> new EcoProperties.Identity("", "https://id.joprelys.test" + CERTS, "joprelys-api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requis");
        assertThatThrownBy(() -> new EcoProperties.Identity("https://id.joprelys.test" + REALM, null, "joprelys-api"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void plainHttpToARemoteIdentityProviderFailsFast() {
        assertThatThrownBy(() -> new EcoProperties.Identity("https://id.joprelys.test" + REALM,
                "http://id.joprelys.test" + CERTS, "joprelys-api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwk-set-uri")
                .hasMessageContaining("HTTPS");
    }
}
