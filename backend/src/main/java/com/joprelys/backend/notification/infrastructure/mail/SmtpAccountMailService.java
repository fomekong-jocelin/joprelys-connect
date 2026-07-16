package com.joprelys.backend.notification.infrastructure.mail;

import com.joprelys.backend.notification.application.AccountMailService;
import com.joprelys.backend.notification.application.MailDeliveryUnavailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpAccountMailService implements AccountMailService {

    private final JavaMailSender mailSender;
    private final AccountMailTemplateFactory templateFactory;
    private final String sender;

    public SmtpAccountMailService(
            JavaMailSender mailSender,
            AccountMailTemplateFactory templateFactory,
            @Value("${spring.mail.username:noreply@joprelys.com}") String sender) {
        this.mailSender = mailSender;
        this.templateFactory = templateFactory;
        this.sender = sender;
    }

    @Override
    public void sendTemporaryPassword(String recipient, String displayName, String temporaryPassword) {
        send(recipient, templateFactory.temporaryPassword(displayName, temporaryPassword));
    }

    @Override
    public void sendLoginCode(String recipient, String displayName, String code) {
        send(recipient, templateFactory.loginCode(displayName, code));
    }

    @Override
    public void sendPasswordRecoveryCode(String recipient, String displayName, String code) {
        send(recipient, templateFactory.passwordRecoveryCode(displayName, code));
    }

    @Override
    public void sendPatientLoginCode(String recipient, String displayName, String code) {
        send(recipient, templateFactory.patientLoginCode(displayName, code));
    }

    private void send(String recipient, AccountMailTemplateFactory.MailContent content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject(content.subject());
            helper.setText(content.plainText(), content.html());
            helper.addInline("joprelys-logo", new ClassPathResource("mail/logo_principal.png"), "image/png");
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            throw new MailDeliveryUnavailableException(exception);
        }
    }
}
