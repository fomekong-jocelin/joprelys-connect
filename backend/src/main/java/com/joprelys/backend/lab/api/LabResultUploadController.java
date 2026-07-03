package com.joprelys.backend.lab.api;

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

	@Value("${joprelys.lab-integration.api-key:lab-partner-secret-token}")
	private String configuredApiKey;

	public LabResultUploadController(LabResultService labResultService) {
		this.labResultService = labResultService;
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
}
