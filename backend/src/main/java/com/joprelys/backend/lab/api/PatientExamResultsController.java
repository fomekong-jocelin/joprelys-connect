package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.LabResultService;
import com.joprelys.backend.patient.application.PatientService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
public class PatientExamResultsController {

	private final LabResultService labResultService;
	private final PatientService patientService;

	public PatientExamResultsController(LabResultService labResultService, PatientService patientService) {
		this.labResultService = labResultService;
		this.patientService = patientService;
	}

	@GetMapping("/api/patients/{id}/exam-results/export")
	@PreAuthorize("hasAnyAuthority('LAB_ORDER_READ', 'PATIENT_PORTAL_ACCESS')")
	public ResponseEntity<?> exportResults(
			@PathVariable("id") UUID patientId,
			@RequestParam(value = "format", defaultValue = "json") String format) {

		patientService.validateAccess(patientId, "lab_results");
		List<LabResultResponse> results = labResultService.getPatientResults(patientId);

		if ("csv".equalsIgnoreCase(format)) {
			StringBuilder csv = new StringBuilder();
			csv.append("resultNumber,examRequestNumber,analyteName,value,unit,referenceRange,interpretation,comment,validatedAt,status,version\n");
			for (LabResultResponse r : results) {
				csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d\n",
						escapeCsv(r.resultNumber()),
						escapeCsv(r.examRequestNumber()),
						escapeCsv(r.analyteName()),
						escapeCsv(r.value()),
						escapeCsv(r.unit()),
						escapeCsv(r.referenceRange()),
						escapeCsv(r.interpretation()),
						escapeCsv(r.comment()),
						r.validatedAt() != null ? r.validatedAt().toString() : "",
						escapeCsv(r.status()),
						r.version()
				));
			}
			byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"results-" + patientId + ".csv\"")
					.contentType(MediaType.parseMediaType("text/csv"))
					.body(out);
		}

		return ResponseEntity.ok(results);
	}

	private String escapeCsv(String val) {
		if (val == null) return "";
		if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
			return "\"" + val.replace("\"", "\"\"") + "\"";
		}
		return val;
	}
}
