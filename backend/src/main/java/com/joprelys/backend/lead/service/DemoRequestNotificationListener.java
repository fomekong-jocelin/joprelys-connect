package com.joprelys.backend.lead.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DemoRequestNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(DemoRequestNotificationListener.class);
    private final DemoRequestNotification notification;

    public DemoRequestNotificationListener(DemoRequestNotification notification) {
        this.notification = notification;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistered(DemoRequestRegistered request) {
        try {
            notification.notifyTeam(request);
            log.info("DEMO_REQUEST_NOTIFICATION_SENT [id={}]", request.id());
        } catch (RuntimeException failure) {
            // The request is already committed. Do not expose contact data or SMTP credentials.
            log.error("DEMO_REQUEST_NOTIFICATION_FAILED [id={}, cause={}]", request.id(), failure.getClass().getSimpleName());
        }
    }
}
