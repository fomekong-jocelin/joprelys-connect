package com.joprelys.backend.lead.infrastructure;

import com.joprelys.backend.lead.service.DemoRequestNotification;
import com.joprelys.backend.lead.service.DemoRequestRegistered;
import com.joprelys.backend.notification.application.MailDeliveryUnavailableException;
import com.joprelys.backend.notification.infrastructure.mail.BrandedSmtpMailSender;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmtpDemoRequestNotification implements DemoRequestNotification {
    private final BrandedSmtpMailSender mailSender;
    private final DemoRequestMailTemplateFactory templates;
    private final String recipient;

    public SmtpDemoRequestNotification(BrandedSmtpMailSender mailSender, DemoRequestMailTemplateFactory templates,
            @Value("${joprelys.contact.email:contact@joprelys.com}") String recipient) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.recipient = recipient;
    }

    @Override
    public void notifyTeam(DemoRequestRegistered request) {
        try {
            mailSender.send(recipient, templates.create(request));
        } catch (MessagingException failure) {
            throw new MailDeliveryUnavailableException(failure);
        }
    }
}
