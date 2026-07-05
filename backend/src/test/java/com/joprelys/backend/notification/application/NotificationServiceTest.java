package com.joprelys.backend.notification.application;

import com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity;
import com.joprelys.backend.notification.infrastructure.persistence.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void givenValidData_whenSendNotification_thenSavedAndReturned() {
        UUID patientId = UUID.randomUUID();
        String title = "Test notification";
        String message = "Message de test";
        String type = "INFO";

        when(notificationRepository.save(any(NotificationEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NotificationEntity result = notificationService.sendNotification(patientId, title, message, type);

        assertNotNull(result);
        assertEquals(patientId, result.getPatientId());
        assertEquals(title, result.getTitle());
        assertEquals(message, result.getMessage());
        assertEquals(type, result.getType());
        assertEquals("NON_LU", result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(notificationRepository, times(1)).save(any(NotificationEntity.class));
    }

    @Test
    void givenPatientWithUnreadNotifications_whenGetUnreadCount_thenReturnsCorrectCount() {
        UUID patientId = UUID.randomUUID();
        when(notificationRepository.countByPatientIdAndStatus(patientId, "NON_LU")).thenReturn(5L);

        long count = notificationService.getUnreadCount(patientId);

        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countByPatientIdAndStatus(patientId, "NON_LU");
    }

    @Test
    void givenPatientWithNotifications_whenGetNotificationsPaginated_thenReturnsPage() {
        UUID patientId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        NotificationEntity notif = new NotificationEntity(patientId, "Titre", "Message", "INFO");
        Page<NotificationEntity> page = new PageImpl<>(List.of(notif), pageable, 1);

        when(notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable)).thenReturn(page);

        Page<NotificationEntity> result = notificationService.getNotificationsPaginated(patientId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Titre", result.getContent().get(0).getTitle());
    }

    @Test
    void givenOwnNotification_whenMarkAsRead_thenStatusUpdated() {
        UUID patientId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity(patientId, "Titre", "Message", "INFO");
        notif.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationEntity result = notificationService.markAsRead(patientId, notifId);

        assertEquals("LU", result.getStatus());
        verify(notificationRepository, times(1)).save(notif);
    }

    @Test
    void givenOtherPatientNotification_whenMarkAsRead_thenThrowsForbidden() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity(otherPatientId, "Titre", "Message", "INFO");
        notif.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> notificationService.markAsRead(patientId, notifId));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void givenOwnNotifications_whenMarkAllAsRead_thenAllUpdated() {
        UUID patientId = UUID.randomUUID();
        NotificationEntity notif1 = new NotificationEntity(patientId, "T1", "M1", "INFO");
        NotificationEntity notif2 = new NotificationEntity(patientId, "T2", "M2", "INFO");
        notif2.setStatus("LU");

        when(notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of(notif1, notif2));
        when(notificationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAllAsRead(patientId);

        assertEquals("LU", notif1.getStatus());
        assertEquals("LU", notif2.getStatus());
        verify(notificationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void givenOwnNotification_whenDelete_thenRemoved() {
        UUID patientId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity(patientId, "Titre", "Message", "INFO");
        notif.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));
        doNothing().when(notificationRepository).delete(notif);

        notificationService.deleteNotification(patientId, notifId);

        verify(notificationRepository, times(1)).delete(notif);
    }

    @Test
    void givenOtherPatientNotification_whenDelete_thenThrowsForbidden() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity(otherPatientId, "Titre", "Message", "INFO");
        notif.setId(notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> notificationService.deleteNotification(patientId, notifId));
        assertEquals(403, ex.getStatusCode().value());
        verify(notificationRepository, never()).delete(any());
    }
}
