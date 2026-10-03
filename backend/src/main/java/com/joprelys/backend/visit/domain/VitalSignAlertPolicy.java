package com.joprelys.backend.visit.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Seuils d'alerte adulte sur les constantes (aide à la priorisation, pas un diagnostic).
 * Valeurs à faire valider par le médecin référent ; elles ne couvrent pas les normes pédiatriques.
 */
public final class VitalSignAlertPolicy {

	public enum Severity { WARNING, CRITICAL }

	public record VitalAlert(String code, Severity severity) {
	}

	private VitalSignAlertPolicy() {
	}

	public static List<VitalAlert> evaluate(
			BigDecimal temperature,
			Integer pulse,
			Integer systolic,
			Integer diastolic,
			Integer spo2,
			BigDecimal glycemia,
			Integer respiratoryRate,
			Integer painScale) {
		List<VitalAlert> alerts = new ArrayList<>();

		if (temperature != null) {
			double t = temperature.doubleValue();
			if (t >= 40.0) alerts.add(critical("TEMPERATURE_HIGH"));
			else if (t >= 38.0) alerts.add(warning("TEMPERATURE_HIGH"));
			else if (t < 35.0) alerts.add(critical("TEMPERATURE_LOW"));
			else if (t < 35.5) alerts.add(warning("TEMPERATURE_LOW"));
		}
		if (spo2 != null) {
			if (spo2 < 90) alerts.add(critical("SPO2_LOW"));
			else if (spo2 < 94) alerts.add(warning("SPO2_LOW"));
		}
		if (systolic != null || diastolic != null) {
			int sys = systolic != null ? systolic : 0;
			int dia = diastolic != null ? diastolic : 0;
			if (sys >= 180 || dia >= 110) alerts.add(critical("BLOOD_PRESSURE_HIGH"));
			else if (sys >= 140 || dia >= 90) alerts.add(warning("BLOOD_PRESSURE_HIGH"));
			if (systolic != null && systolic < 80) alerts.add(critical("BLOOD_PRESSURE_LOW"));
			else if (systolic != null && systolic < 90) alerts.add(warning("BLOOD_PRESSURE_LOW"));
		}
		if (pulse != null) {
			if (pulse > 130) alerts.add(critical("PULSE_HIGH"));
			else if (pulse > 100) alerts.add(warning("PULSE_HIGH"));
			else if (pulse < 40) alerts.add(critical("PULSE_LOW"));
			else if (pulse < 50) alerts.add(warning("PULSE_LOW"));
		}
		if (respiratoryRate != null) {
			if (respiratoryRate > 30) alerts.add(critical("RESPIRATORY_RATE_HIGH"));
			else if (respiratoryRate > 20) alerts.add(warning("RESPIRATORY_RATE_HIGH"));
			else if (respiratoryRate < 10) alerts.add(critical("RESPIRATORY_RATE_LOW"));
			else if (respiratoryRate < 12) alerts.add(warning("RESPIRATORY_RATE_LOW"));
		}
		if (glycemia != null) {
			double g = glycemia.doubleValue();
			if (g < 0.54) alerts.add(critical("GLYCEMIA_LOW"));
			else if (g < 0.70) alerts.add(warning("GLYCEMIA_LOW"));
			else if (g >= 3.0) alerts.add(critical("GLYCEMIA_HIGH"));
			else if (g >= 2.0) alerts.add(warning("GLYCEMIA_HIGH"));
		}
		if (painScale != null && painScale >= 7) {
			alerts.add(warning("PAIN_SEVERE"));
		}
		return List.copyOf(alerts);
	}

	private static VitalAlert warning(String code) {
		return new VitalAlert(code, Severity.WARNING);
	}

	private static VitalAlert critical(String code) {
		return new VitalAlert(code, Severity.CRITICAL);
	}
}
