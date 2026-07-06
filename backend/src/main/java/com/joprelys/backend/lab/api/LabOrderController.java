package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.LabOrderService;
import com.joprelys.backend.lab.application.LabResultService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lab-orders")
@Tag(name = "Examens", description = "Demandes et résultats d'examens médicaux")
public class LabOrderController {

	private final LabOrderService labOrderService;
	private final LabResultService labResultService;
	private final PatientService patientService;
	private final PatientRepository patientRepository;

	public LabOrderController(
			LabOrderService labOrderService,
			LabResultService labResultService,
			PatientService patientService,
			PatientRepository patientRepository) {
		this.labOrderService = labOrderService;
		this.labResultService = labResultService;
		this.patientService = patientService;
		this.patientRepository = patientRepository;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
	@Operation(summary = "Créer une demande d'examen", description = "Crée une nouvelle demande d'examen médical pour un patient.", responses = {
			@ApiResponse(responseCode = "200", description = "Demande d'examen créée avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public LabOrderResponse create(
			@Valid @RequestBody CreateLabOrderRequest request,
			Authentication authentication) {
		patientService.validateAccess(request.patientId(), "lab_results");
		var patient = patientRepository.findByIdGlobally(request.patientId()).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			String practitionerEmail = authentication.getName();
			return labOrderService.create(request, practitionerEmail);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping("/patient/{patientId}")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'PATIENT', 'BIOLOGISTE')")
	@Operation(summary = "Lister les examens d'un patient", description = "Retourne la liste des demandes d'examen pour un patient donné.", responses = {
			@ApiResponse(responseCode = "200", description = "Liste des examens retournée"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public List<LabOrderResponse> getPatientLabOrders(@Parameter(description = "Identifiant du patient") @PathVariable UUID patientId) {
		patientService.validateAccess(patientId, "lab_results");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return labOrderService.getPatientLabOrders(patientId);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('BIOLOGISTE', 'ADMIN_JOPRELYS')")
	@Operation(summary = "Lister toutes les demandes d'examen", description = "Retourne la liste de toutes les demandes d'examen.", responses = {
			@ApiResponse(responseCode = "200", description = "Liste des demandes d'examen retournée"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public List<LabOrderResponse> getLabOrders() {
		return labOrderService.getLabOrders();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('BIOLOGISTE', 'ADMIN_JOPRELYS')")
	@Operation(summary = "Récupérer une demande d'examen", description = "Retourne les détails d'une demande d'examen par son identifiant.", responses = {
			@ApiResponse(responseCode = "200", description = "Demande d'examen trouvée"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public LabOrderResponse getLabOrder(@Parameter(description = "Identifiant de la demande d'examen") @PathVariable UUID id) {
		return labOrderService.getLabOrder(id);
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAnyRole('BIOLOGISTE', 'ADMIN_JOPRELYS')")
	@Operation(summary = "Mettre à jour le statut d'un examen", description = "Modifie le statut d'une demande d'examen médical.", responses = {
			@ApiResponse(responseCode = "200", description = "Statut mis à jour avec succès"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public LabOrderResponse updateStatus(
			@Parameter(description = "Identifiant de la demande d'examen") @PathVariable UUID id,
			@Valid @RequestBody UpdateLabOrderStatusRequest request,
			Authentication authentication) {
		String practitionerEmail = authentication.getName();
		return labOrderService.updateStatus(id, request.status(), practitionerEmail);
	}

	@GetMapping("/patient/{patientId}/results")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'PATIENT', 'BIOLOGISTE')")
	@Operation(summary = "Récupérer les résultats d'examens d'un patient", description = "Retourne les résultats d'examens médicaux pour un patient donné.", responses = {
			@ApiResponse(responseCode = "200", description = "Résultats d'examens retournés"),
			@ApiResponse(responseCode = "404", description = "Introuvable")
	})
	public List<LabResultResponse> getPatientResults(@Parameter(description = "Identifiant du patient") @PathVariable UUID patientId) {
		patientService.validateAccess(patientId, "lab_results");
		var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
		UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
		try {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
			return labResultService.getPatientResults(patientId);
		} finally {
			com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
		}
	}

	@GetMapping("/results/{resultId}/pdf")
	@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE', 'BIOLOGISTE')")
	@Operation(summary = "Télécharger le PDF d'un résultat d'examen", description = "Retourne le fichier PDF correspondant au résultat d'examen donné.")
	public org.springframework.http.ResponseEntity<byte[]> downloadResultPdf(@PathVariable UUID resultId) {
		var result = labResultService.getResultById(resultId);
		byte[] pdfBytes = labResultService.getResultPdfBytes(resultId);
		return org.springframework.http.ResponseEntity.ok()
				.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result-" + result.resultNumber() + ".pdf\"")
				.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
				.body(pdfBytes);
	}
}
