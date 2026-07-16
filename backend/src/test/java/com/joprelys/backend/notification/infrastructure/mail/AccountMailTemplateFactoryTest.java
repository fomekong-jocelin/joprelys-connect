package com.joprelys.backend.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountMailTemplateFactoryTest {

    private final AccountMailTemplateFactory factory = new AccountMailTemplateFactory();

    @Test
    void loginCodeShouldProduceBrandedHtmlAndPlainText() {
        var content = factory.loginCode("Admin Mandacare", "758151");

        assertThat(content.subject()).isEqualTo("Code de connexion Joprelys");
        assertThat(content.plainText())
                .contains("Bonjour Admin Mandacare,")
                .contains("Code de sécurité : 758151")
                .contains("expire dans 5 minutes");
        assertThat(content.html())
                .contains("cid:joprelys-logo")
                .contains("Confirmez votre connexion")
                .contains(">758151<")
                .contains("Joprelys Connect")
                .doesNotContain("<script");
    }

    @Test
    void temporaryPasswordShouldUseFirstLoginGuidance() {
        var content = factory.temporaryPassword("Dr Sophie", "Jop-ABC234");

        assertThat(content.subject()).isEqualTo("Votre compte Joprelys Connect");
        assertThat(content.plainText()).contains("Mot de passe temporaire : Jop-ABC234");
        assertThat(content.html())
                .contains("Votre compte est pr&ecirc;t")
                .contains("remplacez-le imm&eacute;diatement")
                .doesNotContain("expire dans 5 minutes");
    }

    @Test
    void dynamicValuesShouldBeHtmlEscaped() {
        var content = factory.passwordRecoveryCode("<img src=x onerror=alert(1)>", "12<34&56");

        assertThat(content.html())
                .contains("&lt;img src=x onerror=alert(1)&gt;")
                .contains("12&lt;34&amp;56")
                .doesNotContain("<img src=x")
                .doesNotContain("12<34&56");
        assertThat(content.plainText()).contains("12<34&56");
    }

    @Test
    void patientCodeShouldHaveDedicatedContext() {
        var content = factory.patientLoginCode("Patient Test", "404733");

        assertThat(content.subject()).contains("espace patient");
        assertThat(content.html()).contains("Acc&eacute;dez &agrave; votre espace patient");
    }
}
