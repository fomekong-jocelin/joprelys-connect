package com.joprelys.backend.notification.application;

public class MailDeliveryUnavailableException extends RuntimeException {

    public MailDeliveryUnavailableException(Throwable cause) {
        super("The mail delivery service is unavailable", cause);
    }
}
