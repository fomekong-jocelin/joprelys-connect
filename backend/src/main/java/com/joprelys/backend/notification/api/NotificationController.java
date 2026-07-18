package com.joprelys.backend.notification.api;

import com.joprelys.backend.common.api.PageResponse;
import com.joprelys.backend.notification.application.NotificationService;
import com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity;
import com.joprelys.backend.patient.application.PatientAccessGuardService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST dédié aux notifications du portail patient.
 * Complète les endpoints existants dans {@link com.joprelys.backend.patient.api.PatientPortalController}
 * sans les dupliquer.
 */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAuthority('PATIENT_NOTIFICATION_MANAGE')")
public class NotificationController {

    private final NotificationService notificationService;
    private final PatientAccessGuardService patientAccessGuardService;

    public NotificationController(NotificationService notificationService,
                                PatientAccessGuardService patientAccessGuardService) {
        this.notificationService = notificationService;
        this.patientAccessGuardService = patientAccessGuardService;
    }

    /**
     * Retourne le nombre de notifications non lues du patient authentifié.
     * Utilisé pour le badge dans la sidebar/topbar Angular.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount(Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        long count = notificationService.getUnreadCount(patient.getId());
        return ResponseEntity.ok(new UnreadCountDto(count));
    }

    /**
     * Retourne les notifications paginées du patient authentifié.
     */
    @GetMapping
    public ResponseEntity<PageResponse<NotificationEntity>> getNotifications(
            Authentication authentication,
            Pageable pageable) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        return ResponseEntity.ok(PageResponse.fromPage(
                notificationService.getNotificationsPaginated(patient.getId(), pageable)));
    }

    /**
     * Supprime une notification spécifique après vérification de propriété.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            Authentication authentication) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        notificationService.deleteNotification(patient.getId(), id);
        return ResponseEntity.noContent().build();
    }

    public record UnreadCountDto(long count) {}
}
