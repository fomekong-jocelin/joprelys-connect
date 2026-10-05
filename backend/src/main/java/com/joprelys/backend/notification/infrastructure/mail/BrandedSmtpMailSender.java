package com.joprelys.backend.notification.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class BrandedSmtpMailSender {
    private final JavaMailSender mailSender;
    private final String sender;

    public BrandedSmtpMailSender(JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@joprelys.com}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    public void send(String recipient, BrandedMailContent content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(sender);
        helper.setTo(recipient);
        helper.setSubject(content.subject());
        helper.setText(content.plainText(), content.html());
        helper.addInline("joprelys-logo", new ClassPathResource("mail/logo_principal.png"), "image/png");
        mailSender.send(message);
    }
}
