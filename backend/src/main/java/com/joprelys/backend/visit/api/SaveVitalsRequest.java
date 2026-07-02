package com.joprelys.backend.visit.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record SaveVitalsRequest(
		@DecimalMin(value = "30.0", message = "La température doit être supérieure ou égale à 30°C.")
		@DecimalMax(value = "45.0", message = "La température doit être inférieure ou égale à 45°C.")
		BigDecimal temperature,

		@DecimalMin(value = "1.0", message = "Le poids doit être supérieur ou égal à 1kg.")
		@DecimalMax(value = "500.0", message = "Le poids doit être inférieur ou égal à 500kg.")
		BigDecimal weight,

		@Min(value = 30, message = "La taille doit être supérieure ou égale à 30cm.")
		@Max(value = 250, message = "La taille doit être inférieure ou égale à 250cm.")
		Integer height,

		@Min(value = 20, message = "Le pouls doit être supérieur ou égal à 20 bpm.")
		@Max(value = 250, message = "Le pouls doit être inférieur ou égal à 250 bpm.")
		Integer pulse,

		@Min(value = 40, message = "La tension systolique doit être supérieure ou égale à 40 mmHg.")
		@Max(value = 250, message = "La tension systolique doit être inférieure ou égale à 250 mmHg.")
		Integer systolic,

		@Min(value = 30, message = "La tension diastolique doit être supérieure ou égale à 30 mmHg.")
		@Max(value = 150, message = "La tension diastolique doit être inférieure ou égale à 150 mmHg.")
		Integer diastolic,

		@Min(value = 50, message = "La SpO2 doit être supérieure ou égale à 50%.")
		@Max(value = 100, message = "La SpO2 doit être inférieure ou égale à 100%.")
		Integer spo2,

		@DecimalMin(value = "0.1", message = "La glycémie doit être supérieure ou égale à 0.1 g/L.")
		@DecimalMax(value = "10.0", message = "La glycémie doit être inférieure ou égale à 10.0 g/L.")
		BigDecimal glycemia,

		@Min(value = 5, message = "La fréquence respiratoire doit être supérieure ou égale à 5 cycles/min.")
		@Max(value = 100, message = "La fréquence respiratoire doit être inférieure ou égale à 100 cycles/min.")
		Integer respiratoryRate
) {
}
