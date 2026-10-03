package com.joprelys.backend.visit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.visit.domain.VitalSignAlertPolicy.Severity;
import com.joprelys.backend.visit.domain.VitalSignAlertPolicy.VitalAlert;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VitalSignAlertPolicyTest {

	@Test
	void normalAdultVitalsRaiseNoAlert() {
		assertThat(VitalSignAlertPolicy.evaluate(
				new BigDecimal("37.0"), 75, 120, 80, 98, new BigDecimal("0.95"), 16, 2)).isEmpty();
	}

	@Test
	void abnormalValuesAreFlaggedWithSeverity() {
		assertThat(VitalSignAlertPolicy.evaluate(
				new BigDecimal("40.1"), 135, 185, 112, 89, new BigDecimal("0.50"), 32, 8))
				.containsExactly(
						new VitalAlert("TEMPERATURE_HIGH", Severity.CRITICAL),
						new VitalAlert("SPO2_LOW", Severity.CRITICAL),
						new VitalAlert("BLOOD_PRESSURE_HIGH", Severity.CRITICAL),
						new VitalAlert("PULSE_HIGH", Severity.CRITICAL),
						new VitalAlert("RESPIRATORY_RATE_HIGH", Severity.CRITICAL),
						new VitalAlert("GLYCEMIA_LOW", Severity.CRITICAL),
						new VitalAlert("PAIN_SEVERE", Severity.WARNING));
	}

	@Test
	void borderlineValuesAreWarnings() {
		assertThat(VitalSignAlertPolicy.evaluate(
				new BigDecimal("38.2"), null, 85, null, 93, null, null, null))
				.containsExactly(
						new VitalAlert("TEMPERATURE_HIGH", Severity.WARNING),
						new VitalAlert("SPO2_LOW", Severity.WARNING),
						new VitalAlert("BLOOD_PRESSURE_LOW", Severity.WARNING));
	}
}
