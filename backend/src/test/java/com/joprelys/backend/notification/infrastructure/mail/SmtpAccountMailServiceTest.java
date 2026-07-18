package com.joprelys.backend.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;

class SmtpAccountMailServiceTest {

    @Test
    void shouldClassifyInvalidRecipientFromSendFailedException() throws Exception {
        Address invalid = new InternetAddress("missing@example.invalid");
        SendFailedException rejected = new SendFailedException(
                "550 mailbox unavailable",
                null,
                null,
                null,
                new Address[]{invalid});

        assertThat(SmtpAccountMailService.isRecipientRejected(
                new MailSendException("SMTP rejected the recipient", rejected)))
                .isTrue();
    }

    @Test
    void shouldInspectMessagingExceptionNextExceptionChain() throws Exception {
        SendFailedException rejected = new SendFailedException(
                "550 mailbox unavailable",
                null,
                null,
                null,
                new Address[]{new InternetAddress("missing@example.invalid")});
        MessagingException wrapper = new MessagingException("delivery failed");
        wrapper.setNextException(rejected);

        assertThat(SmtpAccountMailService.isRecipientRejected(wrapper)).isTrue();
    }

    @Test
    void shouldTreatMalformedRecipientAsRejected() {
        assertThat(SmtpAccountMailService.isRecipientRejected(
                new MailParseException("invalid recipient")))
                .isTrue();
    }

    @Test
    void shouldNotClassifyAuthenticationFailureAsRejectedRecipient() {
        assertThat(SmtpAccountMailService.isRecipientRejected(
                new MailAuthenticationException("invalid SMTP credentials")))
                .isFalse();
    }
}
