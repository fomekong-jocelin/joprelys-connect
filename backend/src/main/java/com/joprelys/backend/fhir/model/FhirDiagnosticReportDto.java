package com.joprelys.backend.fhir.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FhirDiagnosticReportDto(
		String resourceType,
		String id,
		String status,
		CodeableConcept code,
		Reference subject,
		Reference encounter,
		String effectiveDateTime,
		String issued,
		List<Reference> performer,
		List<Reference> result,
		String conclusion
) {
	public FhirDiagnosticReportDto(
			String id,
			String status,
			CodeableConcept code,
			Reference subject,
			Reference encounter,
			String effectiveDateTime,
			String issued,
			List<Reference> performer,
			List<Reference> result,
			String conclusion
	) {
		this("DiagnosticReport", id, status, code, subject, encounter, effectiveDateTime, issued, performer, result, conclusion);
	}
}
