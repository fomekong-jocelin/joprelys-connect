package com.joprelys.backend.notification.infrastructure.mail;

import com.joprelys.backend.notification.application.AccountMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpAccountMailService implements AccountMailService {

    private final JavaMailSender mailSender;
    private final String sender;

    public SmtpAccountMailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@joprelys.com}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    @Override
    public void sendTemporaryPassword(String recipient, String displayName, String temporaryPassword) {
        send(recipient, "Votre compte Joprelys Connect",
                greeting(displayName) + "\n\nVotre compte a été créé. Votre mot de passe temporaire est : "
                        + temporaryPassword
                        + "\n\nConnectez-vous puis remplacez-le dès que possible.\n\nL'équipe Joprelys");
    }

    @Override
    public void sendLoginCode(String recipient, String displayName, String code) {
        sendCode(recipient, displayName, "Code de connexion Joprelys", code);
    }

    @Override
    public void sendPasswordRecoveryCode(String recipient, String displayName, String code) {
        sendCode(recipient, displayName, "Réinitialisation de votre mot de passe Joprelys", code);
    }

    @Override
    public void sendPatientLoginCode(String recipient, String displayName, String code) {
        sendCode(recipient, displayName, "Code d'accès à votre espace patient Joprelys", code);
    }

    private void sendCode(String recipient, String displayName, String subject, String code) {
        send(recipient, subject, greeting(displayName) + "\n\nVotre code de sécurité est : " + code
                + "\n\nCe code expire dans 5 minutes. Ne le communiquez à personne.\n\nL'équipe Joprelys");
    }

    private void send(String recipient, String subject, String body) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private static String greeting(String displayName) {
        return "Bonjour " + (displayName == null || displayName.isBlank() ? "" : displayName.trim() + ",");
    }
}
