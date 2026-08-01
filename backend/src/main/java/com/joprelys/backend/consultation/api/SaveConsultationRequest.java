package com.joprelys.backend.consultation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveConsultationRequest(

		@NotBlank(message = "Les symptômes sont obligatoires.")
		@Size(max = 5000, message = "Les symptômes ne doivent pas dépasser 5000 caractères.")
		String symptoms,

		@Size(max = 5000, message = "L'examen clinique ne doit pas dépasser 5000 caractères.")
		String clinicalExam,

		@NotBlank(message = "Le diagnostic est obligatoire.")
		@Size(max = 5000, message = "Le diagnostic ne doit pas dépasser 5000 caractères.")
		String diagnosis,

		@Size(max = 5000, message = "La conclusion ne doit pas dépasser 5000 caractères.")
		String conclusion,

		@Size(max = 3000, message = "Les conseils ne doivent pas dépasser 3000 caractères.")
		String advice,

		@Size(max = 1000, message = "Le suivi recommandé ne doit pas dépasser 1000 caractères.")
		String followUp
) {
}
