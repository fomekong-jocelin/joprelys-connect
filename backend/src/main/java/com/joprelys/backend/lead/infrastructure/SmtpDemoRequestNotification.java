package com.joprelys.backend.lead.infrastructure;

import com.joprelys.backend.lead.dto.DemoRequestDto;
import com.joprelys.backend.lead.service.DemoRequestNotification;
import com.joprelys.backend.lead.service.DemoRequestRegistered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpDemoRequestNotification implements DemoRequestNotification {
    private final JavaMailSender mailSender;
    private final String sender;
    private final String recipient;

    public SmtpDemoRequestNotification(JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@joprelys.com}") String sender,
            @Value("${joprelys.contact.email:contact@joprelys.com}") String recipient) {
        this.mailSender = mailSender;
        this.sender = sender;
        this.recipient = recipient;
    }

    @Override
    public void notifyTeam(DemoRequestRegistered request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject("Joprelys Connect — Nouvelle demande de démonstration");
        message.setText(body(request));
        mailSender.send(message);
    }

    private String body(DemoRequestRegistered request) {
        DemoRequestDto lead = request.details();
        return "Une demande de démonstration a été enregistrée.\n\n"
                + "Référence : " + request.id() + "\n"
                + "Nom : " + lead.fullName() + "\n"
                + "Établissement : " + lead.organizationName() + "\n"
                + "Fonction : " + optional(lead.role()) + "\n"
                + "Téléphone : " + lead.phone() + "\n"
                + "Email : " + optional(lead.email()) + "\n"
                + "Ville : " + lead.city() + "\n"
                + "Langue : " + lead.locale() + "\n"
                + "Message : " + optional(lead.message()) + "\n";
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
