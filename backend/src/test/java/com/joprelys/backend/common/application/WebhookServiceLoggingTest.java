package com.joprelys.backend.common.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.common.infrastructure.persistence.WebhookEntity;
import com.joprelys.backend.common.infrastructure.persistence.WebhookRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class WebhookServiceLoggingTest {

    @Test
    void webhookPayloadAndTargetUrlNeverReachTheLogs(CapturedOutput output) {
        UUID organizationId = UUID.randomUUID();
        WebhookRepository repository = mock(WebhookRepository.class);
        WebhookEntity webhook = new WebhookEntity(
                organizationId, "https://partner.example.test/hook?token=s3cr3t", "secret", "VISIT_CLOSED");
        when(repository.findByOrganizationIdAndStatus(organizationId, "ACTIVE")).thenReturn(List.of(webhook));

        new WebhookService(repository).triggerEvent(
                "VISIT_CLOSED", organizationId, "{\"patient\":\"Jeanne Fictive\",\"diagnostic\":\"Paludisme grave\"}");

        assertThat(output.getAll())
                .contains("[WEBHOOK MOCK] Event: VISIT_CLOSED")
                .doesNotContain("Jeanne Fictive")
                .doesNotContain("Paludisme")
                .doesNotContain("s3cr3t");
    }
}
