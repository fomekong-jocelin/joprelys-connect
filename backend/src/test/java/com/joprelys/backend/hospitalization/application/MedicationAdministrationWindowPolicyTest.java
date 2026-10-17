package com.joprelys.backend.hospitalization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MedicationAdministrationWindowPolicyTest {

    private final MedicationAdministrationWindowPolicy policy = new MedicationAdministrationWindowPolicy();

    @Test
    void derivesTheWindowFromUnambiguousFrequencies() {
        assertThat(policy.window("toutes les 8 h", null)).isEqualTo(Duration.ofHours(7));
        assertThat(policy.window(null, "1 g IV/24h")).isEqualTo(Duration.ofHours(23));
        assertThat(policy.window("3 fois par jour", null)).isEqualTo(Duration.ofHours(7));
        assertThat(policy.window("x2/j", null)).isEqualTo(Duration.ofHours(11));
        assertThat(policy.window("q1h", null)).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void fallsBackToOneHourWhenTheFrequencyIsUnknown() {
        assertThat(policy.window("si douleur", "selon besoin")).isEqualTo(Duration.ofHours(1));
        assertThat(policy.window(null, null)).isEqualTo(Duration.ofHours(1));
    }
}
