package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import java.math.BigDecimal;

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
		BigDecimal bmi
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
				entity.getBmi()
		);
	}
}
