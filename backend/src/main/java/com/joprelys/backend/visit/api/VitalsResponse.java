package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.domain.VitalSignAlertPolicy;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VitalsResponse(
		BigDecimal temperature,
		BigDecimal weight,
		Integer height,
		Integer pulse,
		Integer systolic,
		Integer diastolic,
		Integer spo2,
		BigDecimal glycemia,
		Integer respiratoryRate,
		Integer painScale,
		BigDecimal bmi,
		Instant recordedAt,
		List<VitalSignAlertPolicy.VitalAlert> alerts
) {
	public static VitalsResponse fromEntity(VitalsEntity entity) {
		if (entity == null) return null;
		return new VitalsResponse(
				entity.getTemperature(),
				entity.getWeight(),
				entity.getHeight(),
				entity.getPulse(),
				entity.getSystolic(),
				entity.getDiastolic(),
				entity.getSpo2(),
				entity.getGlycemia(),
				entity.getRespiratoryRate(),
				entity.getPainScale(),
				entity.getBmi(),
				entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt(),
				VitalSignAlertPolicy.evaluate(
						entity.getTemperature(),
						entity.getPulse(),
						entity.getSystolic(),
						entity.getDiastolic(),
						entity.getSpo2(),
						entity.getGlycemia(),
						entity.getRespiratoryRate(),
						entity.getPainScale())
		);
	}
}
