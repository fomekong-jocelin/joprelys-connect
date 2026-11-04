package com.joprelys.backend.prescription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

class PrescriptionPinGuardTest {

    private final PrescriptionPinGuard guard = new PrescriptionPinGuard(
            mock(JdbcTemplate.class), mock(PlatformTransactionManager.class), Clock.systemUTC());

    @Test
    void generatedPinsHaveSixDigits() {
        for (int i = 0; i < 200; i++) {
            assertThat(guard.generatePin()).matches("[0-9]{6}");
        }
    }
}
