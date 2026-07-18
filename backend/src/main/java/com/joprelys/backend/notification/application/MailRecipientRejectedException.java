package com.joprelys.backend.notification.application;

public class MailRecipientRejectedException extends RuntimeException {

    public MailRecipientRejectedException(Throwable cause) {
        super("The mail recipient was rejected", cause);
    }
}
