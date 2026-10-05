package com.joprelys.backend.lead.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.service.DemoRequestRegistered;
import com.joprelys.backend.notification.infrastructure.mail.BrandedMailTemplateFactory;
import com.joprelys.backend.notification.infrastructure.mail.BrandedSmtpMailSender;
import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpDemoRequestNotificationTest {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final DemoRequestMailTemplateFactory templates = new DemoRequestMailTemplateFactory(new BrandedMailTemplateFactory());
    private final SmtpDemoRequestNotification notification =
            new SmtpDemoRequestNotification(new BrandedSmtpMailSender(sender, "noreply@joprelys.com"), templates, "contact@joprelys.com");

    SmtpDemoRequestNotificationTest() {
        when(sender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void shouldSendBrandedHtmlAndPlainTextWithInlineLogoWithoutUsingVisitorHeaders() throws Exception {
        DemoRequestRegistered request = new DemoRequestRegistered(UUID.randomUUID(), new DemoRequestDto(
                "Visitor", "Clinic", "Director", "+237600000000", "visitor@example.com", "Douala",
                "<script>text</script>\nMy priorities", "landing-page", "en"));
        notification.notifyTeam(request);
        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(message.capture());
        message.getValue().saveChanges();
        assertThat(message.getValue().getAllRecipients()).extracting(Object::toString).containsExactly("contact@joprelys.com");
        assertThat(message.getValue().getFrom()).extracting(Object::toString).containsExactly("noreply@joprelys.com");
        assertThat(message.getValue().getHeader("Reply-To")).isNull();
        assertThat(message.getValue().getSubject()).doesNotContain("Visitor");
        String plain = findText(message.getValue(), "text/plain");
        String html = findText(message.getValue(), "text/html");
        assertThat(plain).contains(request.id().toString(), "Visitor", "Clinic", "Director",
                "+237600000000", "visitor@example.com", "Douala", "English", "My priorities");
        assertThat(html).contains("cid:joprelys-logo", "New demo request", "&lt;script&gt;text&lt;/script&gt;<br>My priorities")
                .doesNotContain("<script>");
        assertThat(hasLogo(message.getValue())).isTrue();
    }

    @Test
    void shouldSupportAnOverriddenRecipientAndAbsentOptionalFields() throws Exception {
        new SmtpDemoRequestNotification(new BrandedSmtpMailSender(sender, "noreply@joprelys.com"), templates, "team@example.com").notifyTeam(
                new DemoRequestRegistered(UUID.randomUUID(), new DemoRequestDto(
                        "Visitor", "Clinic", null, "600000000", null, "Douala", null, "landing-page", "fr")));
        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getAllRecipients()).extracting(Object::toString).containsExactly("team@example.com");
        message.getValue().saveChanges();
        assertThat(findText(message.getValue(), "text/plain")).doesNotContain("null");
    }

    @Test
    void shouldPropagateSmtpFailureToTheListener() {
        doThrow(new MailSendException("unavailable")).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> notification.notifyTeam(new DemoRequestRegistered(UUID.randomUUID(),
                new DemoRequestDto("Visitor", "Clinic", null, "600000000", null, "Douala", null, null, "fr"))))
                .isInstanceOf(MailSendException.class);
    }

    private String findText(Part part, String type) throws Exception {
        if (part.isMimeType(type)) return (String) part.getContent();
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String result = findText(multipart.getBodyPart(i), type);
                if (result != null) return result;
            }
        }
        return null;
    }

    private boolean hasLogo(Part part) throws Exception {
        if (part.isMimeType("image/png") && part.getHeader("Content-ID") != null) {
            return "<joprelys-logo>".equals(part.getHeader("Content-ID")[0]);
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                if (hasLogo(multipart.getBodyPart(i))) return true;
            }
        }
        return false;
    }
}
