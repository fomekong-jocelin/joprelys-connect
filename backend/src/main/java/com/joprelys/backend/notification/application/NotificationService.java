package com.joprelys.backend.notification.application;

import com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity;
import com.joprelys.backend.notification.infrastructure.persistence.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationEntity sendNotification(UUID patientId, String title, String message, String type) {
        var notification = new NotificationEntity(patientId, title, message, type);
        var saved = notificationRepository.save(notification);
        
        // Simuler l'envoi via un canal Email/SMS/WhatsApp en loggant dans la console
        log.info("[NOTIFICATION GATEWAY MOCK] Channel: EMAIL/SMS | Recipient Patient: {} | Title: {} | Message: {}", 
                patientId, title, message);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getPatientNotifications(UUID patientId) {
        return notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional
    public NotificationEntity markAsRead(UUID patientId, UUID notificationId) {
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable."));

        if (!notification.getPatientId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette notification ne vous concerne pas.");
        }

        notification.setStatus("LU");
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID patientId) {
        var notifications = notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        boolean changed = false;
        for (var notif : notifications) {
            if ("NON_LU".equals(notif.getStatus())) {
                notif.setStatus("LU");
                changed = true;
            }
        }
        if (changed) {
            notificationRepository.saveAll(notifications);
        }
    }
}
