package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.domain.VitalSignAlertPolicy;
import com.joprelys.backend.visit.infrastructure.persistence.VitalMeasurementEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VitalMeasurementResponse(
		UUID id,
		Instant recordedAt,
		UUID recordedBy,
		String recordedByName,
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
		List<VitalSignAlertPolicy.VitalAlert> alerts
) {
	public static VitalMeasurementResponse fromEntity(VitalMeasurementEntity entity) {
		return new VitalMeasurementResponse(
				entity.getId(),
				entity.getRecordedAt(),
				entity.getRecordedBy(),
				entity.getRecordedByName(),
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
				VitalSignAlertPolicy.evaluate(
						entity.getTemperature(),
						entity.getPulse(),
						entity.getSystolic(),
						entity.getDiastolic(),
						entity.getSpo2(),
						entity.getGlycemia(),
						entity.getRespiratoryRate(),
						entity.getPainScale()));
	}
}
