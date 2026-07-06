package com.joprelys.backend.fhir;

import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.fhir.model.*;

import java.util.List;
import java.util.stream.Collectors;

public class FhirDiagnosticReportMapper {

	public static FhirDiagnosticReportDto toFhir(List<LabResultEntity> results) {
		if (results == null || results.isEmpty()) {
			return null;
		}

		LabResultEntity mainResult = results.get(0);

		String patientId = mainResult.getPatient() != null && mainResult.getPatient().getId() != null
				? mainResult.getPatient().getId().toString() : null;
		String encounterId = mainResult.getLabOrder() != null && mainResult.getLabOrder().getId() != null
				? mainResult.getLabOrder().getId().toString() : null;

		Reference subjectRef = patientId != null ? new Reference("Patient/" + patientId) : null;
		Reference encounterRef = encounterId != null ? new Reference("Encounter/" + encounterId) : null;

		String effectiveDateTime = mainResult.getSampleCollectedAt() != null
				? mainResult.getSampleCollectedAt().toString() : null;
		String issued = mainResult.getValidatedAt() != null
				? mainResult.getValidatedAt().toString() : null;

		CodeableConcept code = new CodeableConcept(
				new Coding("http://loinc.org", "11502-2", "Laboratory report"),
				"Laboratory report"
		);

		List<Reference> performer = mainResult.getValidatorUserId() != null
				? List.of(new Reference("Practitioner/" + mainResult.getValidatorUserId().toString()))
				: null;

		List<Reference> resultObsRefs = results.stream()
				.map(r -> new Reference("Observation/" + r.getId().toString()))
				.collect(Collectors.toList());

		String status = "final";
		if (mainResult.getStatus() != null) {
			status = mainResult.getStatus().name().toLowerCase();
		}

		return new FhirDiagnosticReportDto(
				mainResult.getResultNumber(),
				status,
				code,
				subjectRef,
				encounterRef,
				effectiveDateTime,
				issued,
				performer,
				resultObsRefs,
				mainResult.getConclusion()
		);
	}
}
