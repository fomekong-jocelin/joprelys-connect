package com.joprelys.backend.common.application;

import com.joprelys.backend.common.infrastructure.persistence.WebhookEntity;
import com.joprelys.backend.common.infrastructure.persistence.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookServiceTest {

    private WebhookRepository webhookRepository;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookRepository = mock(WebhookRepository.class);
        webhookService = new WebhookService(webhookRepository);
    }

    @Test
    void givenValidData_whenCreateWebhook_thenSaved() {
        UUID orgId = UUID.randomUUID();
        when(webhookRepository.save(any(WebhookEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookEntity result = webhookService.createWebhook(orgId, "https://partner.com/webhook", "secret123", "LAB_RESULT_AVAILABLE,PRESCRIPTION_CREATED");

        assertNotNull(result);
        assertEquals(orgId, result.getOrganizationId());
        assertEquals("https://partner.com/webhook", result.getUrl());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(0, result.getFailureCount());
        verify(webhookRepository, times(1)).save(any(WebhookEntity.class));
    }

    @Test
    void givenNonHttpsUrl_whenCreateWebhook_thenThrowsBadRequest() {
        UUID orgId = UUID.randomUUID();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> webhookService.createWebhook(orgId, "http://partner.com/webhook", "secret", "EVENT"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void givenBlankUrl_whenCreateWebhook_thenThrowsBadRequest() {
        UUID orgId = UUID.randomUUID();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> webhookService.createWebhook(orgId, "", "secret", "EVENT"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void givenOrgId_whenGetOrganizationWebhooks_thenReturnsActiveOnly() {
        UUID orgId = UUID.randomUUID();
        WebhookEntity wh1 = new WebhookEntity(orgId, "https://a.com", "s", "EVENT");
        WebhookEntity wh2 = new WebhookEntity(orgId, "https://b.com", "s", "EVENT");
        wh2.setStatus("INACTIVE");
        when(webhookRepository.findByOrganizationIdAndStatus(orgId, "ACTIVE")).thenReturn(List.of(wh1));

        List<WebhookEntity> result = webhookService.getOrganizationWebhooks(orgId);

        assertEquals(1, result.size());
        assertEquals("https://a.com", result.get(0).getUrl());
    }

    @Test
    void givenOwnWebhook_whenUpdate_thenUpdated() {
        UUID orgId = UUID.randomUUID();
        UUID webhookId = UUID.randomUUID();
        WebhookEntity existing = new WebhookEntity(orgId, "https://old.com", "old", "EVENT");
        existing.setId(webhookId);
        when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(existing));
        when(webhookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WebhookEntity result = webhookService.updateWebhook(webhookId, orgId, "https://new.com", "new", "NEW_EVENT");

        assertEquals("https://new.com", result.getUrl());
        assertEquals("new", result.getSecret());
        assertEquals("NEW_EVENT", result.getEventTypes());
    }

    @Test
    void givenOtherOrgWebhook_whenUpdate_thenThrowsForbidden() {
        UUID orgId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        UUID webhookId = UUID.randomUUID();
        WebhookEntity existing = new WebhookEntity(otherOrgId, "https://a.com", "s", "EVENT");
        existing.setId(webhookId);
        when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> webhookService.updateWebhook(webhookId, orgId, "https://new.com", "new", "EVENT"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void givenOwnWebhook_whenDelete_thenStatusSetToInactive() {
        UUID orgId = UUID.randomUUID();
        UUID webhookId = UUID.randomUUID();
        WebhookEntity existing = new WebhookEntity(orgId, "https://a.com", "s", "EVENT");
        existing.setId(webhookId);
        when(webhookRepository.findById(webhookId)).thenReturn(Optional.of(existing));
        when(webhookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        webhookService.deleteWebhook(webhookId, orgId);

        assertEquals("INACTIVE", existing.getStatus());
    }

    @Test
    void givenWebhookWithMatchingEvent_whenTrigger_thenLogMock() {
        UUID orgId = UUID.randomUUID();
        WebhookEntity wh = new WebhookEntity(orgId, "https://a.com", "s", "LAB_RESULT_AVAILABLE");
        when(webhookRepository.findByOrganizationIdAndStatus(orgId, "ACTIVE")).thenReturn(List.of(wh));

        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> webhookService.triggerEvent("LAB_RESULT_AVAILABLE", orgId, "{\"patientId\":\"123\"}"));
    }

    @Test
    void givenWebhookWithDifferentEvent_whenTrigger_thenNoAction() {
        UUID orgId = UUID.randomUUID();
        WebhookEntity wh = new WebhookEntity(orgId, "https://a.com", "s", "PRESCRIPTION_CREATED");
        when(webhookRepository.findByOrganizationIdAndStatus(orgId, "ACTIVE")).thenReturn(List.of(wh));

        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> webhookService.triggerEvent("LAB_RESULT_AVAILABLE", orgId, "{}"));
    }
}
