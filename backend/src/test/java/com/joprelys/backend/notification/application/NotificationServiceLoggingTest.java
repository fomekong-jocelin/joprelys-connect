package com.joprelys.backend.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.notification.infrastructure.persistence.NotificationEntity;
import com.joprelys.backend.notification.infrastructure.persistence.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class NotificationServiceLoggingTest {

    @Test
    void notificationTitleAndContentNeverReachTheLogs(CapturedOutput output) {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationService service = new NotificationService(repository);

        service.sendNotification(UUID.randomUUID(), "Résultat VIH disponible",
                "Votre sérologie est positive, merci de rappeler le service.", "LAB_RESULT");

        assertThat(output.getAll())
                .contains("[NOTIFICATION GATEWAY MOCK]")
                .doesNotContain("VIH")
                .doesNotContain("sérologie");
    }
}
