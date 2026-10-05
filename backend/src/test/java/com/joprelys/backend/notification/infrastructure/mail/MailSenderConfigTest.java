package com.joprelys.backend.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;

class MailSenderConfigTest {
    @Test
    void shouldPreserveSmtpDefaultsAndBoundNetworkWaits() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) new MailSenderConfig().javaMailSender(new MockEnvironment());
        assertThat(sender.getHost()).isEqualTo("mail.joprelys.com");
        assertThat(sender.getPort()).isEqualTo(465);
        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.ssl.enable", "true").containsEntry("mail.smtp.starttls.enable", "false")
                .containsEntry("mail.smtp.connectiontimeout", "3000").containsEntry("mail.smtp.timeout", "3000")
                .containsEntry("mail.smtp.writetimeout", "3000");
    }

    @Test
    void shouldHonorExistingConnectionAndNewTimeoutOverrides() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.mail.host", "smtp.example.com")
                .withProperty("spring.mail.port", "587").withProperty("spring.mail.username", "sender@example.com")
                .withProperty("spring.mail.password", "test-password")
                .withProperty("spring.mail.properties.mail.smtp.ssl.enable", "false")
                .withProperty("spring.mail.properties.mail.smtp.starttls.enable", "true")
                .withProperty("spring.mail.properties.mail.smtp.timeout", "5000");
        JavaMailSenderImpl sender = (JavaMailSenderImpl) new MailSenderConfig().javaMailSender(environment);
        assertThat(sender.getHost()).isEqualTo("smtp.example.com");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getUsername()).isEqualTo("sender@example.com");
        assertThat(sender.getPassword()).isEqualTo("test-password");
        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.ssl.enable", "false")
                .containsEntry("mail.smtp.starttls.enable", "true").containsEntry("mail.smtp.timeout", "5000");
    }
}
