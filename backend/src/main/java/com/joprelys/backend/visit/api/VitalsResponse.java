package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;

public record VitalsResponse(
		Double temperature,
		Double weight,
		Integer height,
		Integer pulse,
		Integer systolic,
		Integer diastolic,
		Integer spo2,
		Double glycemia,
		Integer respiratoryRate,
		Double bmi
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
				entity.getBmi()
		);
	}
}
