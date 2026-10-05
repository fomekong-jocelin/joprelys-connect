package com.joprelys.backend.lead.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.service.DemoRequestRegistered;
import com.joprelys.backend.notification.infrastructure.mail.BrandedMailTemplateFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemoRequestMailTemplateFactoryTest {
    private final DemoRequestMailTemplateFactory templates = new DemoRequestMailTemplateFactory(new BrandedMailTemplateFactory());

    @Test
    void shouldUseSharedBrandingAndReadableFrenchRole() {
        var content = templates.create(request("directeur", "fr", "Bonjour\nUne démonstration, merci."));
        assertThat(content.html()).contains("cid:joprelys-logo", "max-width:600px", "#0b91b2",
                "Nouvelle demande de d&eacute;monstration", "Directeur / DAF", "Bonjour<br>Une d&eacute;monstration, merci.");
        assertThat(content.plainText()).contains("Fonction : Directeur / DAF", "Langue : Français");
        assertThat(content.html()).doesNotContain("{{", "Sécurité du compte");
    }

    @Test
    void shouldEscapeVisitorHtmlAndPreserveTextFallback() {
        var content = templates.create(request("<img src=x onerror=alert(1)>", "fr", "<script>alert(1)</script> & merci"));
        assertThat(content.html()).contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; merci", "&lt;img src=x onerror=alert(1)&gt;")
                .doesNotContain("<script>", "<img src=x");
        assertThat(content.plainText()).contains("<script>alert(1)</script> & merci");
    }

    @Test
    void shouldLocalizeEnglishAndHandleOptionalValues() {
        var content = templates.create(request("major", "en", null));
        assertThat(content.subject()).isEqualTo("Joprelys Connect — New demo request");
        assertThat(content.html()).contains("<html lang=\"en\">", "Head nurse / Supervisor", "Connected healthcare you can trust.");
        assertThat(content.plainText()).contains("Language : English", "Message : —").doesNotContain("null");
    }

    private DemoRequestRegistered request(String role, String locale, String message) {
        return new DemoRequestRegistered(UUID.randomUUID(), new DemoRequestDto("Test Visitor", "Test Clinic", role,
                "+237600000000", null, "Douala", message, "landing-page", locale));
    }
}
