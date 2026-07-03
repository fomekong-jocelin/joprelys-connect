package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.FhirDiagnosticReportParser;
import com.joprelys.backend.lab.application.LabResultService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/lab-integration")
public class LabResultUploadController {

	private final LabResultService labResultService;
	private final FhirDiagnosticReportParser fhirParser;

	@Value("${joprelys.lab-integration.api-key:lab-partner-secret-token}")
	private String configuredApiKey;

	public LabResultUploadController(LabResultService labResultService,
			FhirDiagnosticReportParser fhirParser) {
		this.labResultService = labResultService;
		this.fhirParser = fhirParser;
	}

	@PostMapping("/upload")
	public ResponseEntity<Void> uploadResults(
			@RequestHeader(value = "X-API-KEY", required = false) String apiKey,
			@RequestBody LabResultUploadRequest request) {

		// 1. Valider la clé d'API
		if (apiKey == null || !apiKey.equals(configuredApiKey)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clé d'API invalide ou manquante.");
		}

		// 2. Traiter le téléversement
		labResultService.uploadResults(request);

		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	/**
	 * Import de résultats biologiques au format FHIR R4 DiagnosticReport.
	 * STORY-1104.
	 */
	@PostMapping("/fhir/diagnostic-report")
	public ResponseEntity<Void> uploadFhirDiagnosticReport(
			@RequestHeader(value = "X-API-KEY", required = false) String apiKey,
			@RequestBody FhirDiagnosticReportUploadRequest request) {

		// 1. Valider la clé d'API
		if (apiKey == null || !apiKey.equals(configuredApiKey)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Clé d'API invalide ou manquante.");
		}

		// 2. Parser le payload FHIR
		LabResultUploadRequest parsedRequest = fhirParser.parse(request.fhirJson(), request.validatorName());

		// 3. Traiter le téléversement
		labResultService.uploadResults(parsedRequest);

		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
}
