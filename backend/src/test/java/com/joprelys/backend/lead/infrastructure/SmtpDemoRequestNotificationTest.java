package com.joprelys.backend.lead.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.service.DemoRequestRegistered;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpDemoRequestNotificationTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final SmtpDemoRequestNotification notification =
            new SmtpDemoRequestNotification(sender, "noreply@joprelys.com", "contact@joprelys.com");

    @Test
    void shouldSendAllDetailsAsPlainTextToTheTeamWithoutUsingVisitorHeaders() {
        DemoRequestRegistered request = new DemoRequestRegistered(UUID.randomUUID(), new DemoRequestDto(
                "Visitor", "Clinic", "Director", "+237600000000", "visitor@example.com", "Douala",
                "<script>text</script>\nMy priorities", "landing-page", "en"));
        notification.notifyTeam(request);
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("contact@joprelys.com");
        assertThat(message.getValue().getFrom()).isEqualTo("noreply@joprelys.com");
        assertThat(message.getValue().getReplyTo()).isNull();
        assertThat(message.getValue().getSubject()).doesNotContain("Visitor");
        assertThat(message.getValue().getText()).contains(request.id().toString(), "Visitor", "Clinic",
                "Director", "+237600000000", "visitor@example.com", "Douala", "en", "My priorities");
    }

    @Test
    void shouldSupportAnOverriddenRecipientAndAbsentOptionalFields() {
        new SmtpDemoRequestNotification(sender, "noreply@joprelys.com", "team@example.com").notifyTeam(
                new DemoRequestRegistered(UUID.randomUUID(), new DemoRequestDto(
                        "Visitor", "Clinic", null, "600000000", null, "Douala", null, "landing-page", "fr")));
        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("team@example.com");
        assertThat(message.getValue().getText()).doesNotContain("null");
    }

    @Test
    void shouldPropagateSmtpFailureToTheListener() {
        doThrow(new MailSendException("unavailable")).when(sender).send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> notification.notifyTeam(new DemoRequestRegistered(UUID.randomUUID(),
                new DemoRequestDto("Visitor", "Clinic", null, "600000000", null, "Douala", null, null, "fr"))))
                .isInstanceOf(MailSendException.class);
    }
}
