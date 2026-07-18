package com.joprelys.backend.notification.infrastructure.mail;

import com.joprelys.backend.notification.application.AccountMailService;
import com.joprelys.backend.notification.application.MailDeliveryUnavailableException;
import com.joprelys.backend.notification.application.MailRecipientRejectedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
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
            if (isRecipientRejected(exception)) {
                throw new MailRecipientRejectedException(exception);
            }
            throw new MailDeliveryUnavailableException(exception);
        }
    }

    static boolean isRecipientRejected(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Throwable> pending = new ArrayDeque<>();
        pending.add(failure);

        while (!pending.isEmpty()) {
            Throwable current = pending.removeFirst();
            if (current == null || !visited.add(current)) {
                continue;
            }
            if (current instanceof MailParseException) {
                return true;
            }
            if (current instanceof SendFailedException sendFailed
                    && sendFailed.getInvalidAddresses() != null
                    && sendFailed.getInvalidAddresses().length > 0) {
                return true;
            }
            if (isPermanentSmtpAddressFailure(current)) {
                return true;
            }
            if (current instanceof MailSendException mailSendException) {
                for (Exception nested : mailSendException.getMessageExceptions()) {
                    pending.addLast(nested);
                }
            }
            if (current instanceof MessagingException messagingException
                    && messagingException.getNextException() != null) {
                pending.addLast(messagingException.getNextException());
            }
            if (current.getCause() != null) {
                pending.addLast(current.getCause());
            }
        }
        return false;
    }

    private static boolean isPermanentSmtpAddressFailure(Throwable failure) {
        if (!"SMTPAddressFailedException".equals(failure.getClass().getSimpleName())) {
            return false;
        }
        try {
            Object value = failure.getClass().getMethod("getReturnCode").invoke(failure);
            return value instanceof Integer returnCode && returnCode >= 500 && returnCode < 600;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }
}
