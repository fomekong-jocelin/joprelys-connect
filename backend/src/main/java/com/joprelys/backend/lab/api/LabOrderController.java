package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.application.LabOrderService;
import com.joprelys.backend.lab.application.LabResultService;
import com.joprelys.backend.patient.application.PatientAccessPolicyService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/lab-orders")
@Tag(name = "Examens", description = "Demandes et résultats d'examens médicaux")
public class LabOrderController {

    private final LabOrderService labOrderService;
    private final LabResultService labResultService;
    private final PatientAccessPolicyService accessPolicy;
    private final PatientRepository patientRepository;

    public LabOrderController(
            LabOrderService labOrderService,
            LabResultService labResultService,
            PatientAccessPolicyService accessPolicy,
            PatientRepository patientRepository) {
        this.labOrderService = labOrderService;
        this.labResultService = labResultService;
        this.accessPolicy = accessPolicy;
        this.patientRepository = patientRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAB_ORDER_CREATE')")
    @Operation(summary = "Créer une demande d'examen", description = "Crée une nouvelle demande d'examen médical pour un patient.")
    public LabOrderResponse create(
            @Valid @RequestBody CreateLabOrderRequest request,
            Authentication authentication) {
        accessPolicy.validateAccess(request.patientId(), "lab_results");
        var patient = resolvePatient(request.patientId());
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return labOrderService.create(request, authentication.getName());
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('LAB_ORDER_READ')")
    @Operation(summary = "Lister les examens d'un patient", description = "Retourne la liste des demandes d'examen pour un patient donné. Une liste vide est retournée lorsqu'aucun examen n'existe.")
    public List<LabOrderResponse> getPatientLabOrders(
            @Parameter(description = "Identifiant du patient") @PathVariable UUID patientId) {
        accessPolicy.validateAccess(patientId, "lab_results");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return labOrderService.getPatientLabOrders(patientId);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LAB_QUEUE_READ')")
    @Operation(summary = "Lister toutes les demandes d'examen", description = "Retourne la liste de toutes les demandes d'examen.", responses = {
            @ApiResponse(responseCode = "200", description = "Liste des demandes d'examen retournée")
    })
    public List<LabOrderResponse> getLabOrders() {
        return labOrderService.getLabOrders();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LAB_QUEUE_READ')")
    @Operation(summary = "Récupérer une demande d'examen", description = "Retourne les détails d'une demande d'examen par son identifiant.", responses = {
            @ApiResponse(responseCode = "200", description = "Demande d'examen trouvée"),
            @ApiResponse(responseCode = "404", description = "Demande d'examen inexistante")
    })
    public LabOrderResponse getLabOrder(
            @Parameter(description = "Identifiant de la demande d'examen") @PathVariable UUID id) {
        return labOrderService.getLabOrder(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('LAB_ORDER_WRITE')")
    @Operation(summary = "Mettre à jour le statut d'une demande", description = "Modifie le statut global d'une demande d'examen médical. Conservé pour compatibilité avec les intégrations existantes.")
    public LabOrderResponse updateStatus(
            @Parameter(description = "Identifiant de la demande d'examen") @PathVariable UUID id,
            @Valid @RequestBody UpdateLabOrderStatusRequest request,
            Authentication authentication) {
        return labOrderService.updateStatus(id, request.status(), authentication.getName());
    }

    @PatchMapping("/{orderId}/items/{itemId}/status")
    @PreAuthorize("hasAuthority('LAB_ORDER_WRITE')")
    @Operation(summary = "Mettre à jour le statut d'un examen", description = "Fait avancer un examen précis sans modifier directement les autres examens de la demande.")
    public LabOrderResponse updateItemStatus(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateLabOrderStatusRequest request,
            Authentication authentication) {
        return labOrderService.updateItemStatus(orderId, itemId, request.status(), authentication.getName());
    }

    @GetMapping("/patient/{patientId}/results")
    @PreAuthorize("hasAuthority('LAB_ORDER_READ')")
    @Operation(summary = "Récupérer les résultats d'examens d'un patient", description = "Retourne les résultats d'examens médicaux. Une liste vide est retournée lorsqu'aucun résultat n'existe.")
    public List<LabResultResponse> getPatientResults(
            @Parameter(description = "Identifiant du patient") @PathVariable UUID patientId) {
        accessPolicy.validateAccess(patientId, "lab_results");
        var patient = resolvePatient(patientId);
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return labResultService.getPatientResults(patientId);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @GetMapping("/results/{resultId}/pdf")
    @PreAuthorize("hasAuthority('LAB_ORDER_READ')")
    @Operation(summary = "Télécharger le PDF d'un résultat d'examen", description = "Retourne le fichier PDF correspondant au résultat d'examen donné.")
    public org.springframework.http.ResponseEntity<byte[]> downloadResultPdf(@PathVariable UUID resultId) {
        var result = labResultService.getResultById(resultId);
        byte[] pdfBytes = labResultService.getResultPdfBytes(resultId);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"result-" + result.resultNumber() + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/results")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LAB_ORDER_WRITE')")
    @Operation(summary = "Saisir les résultats d'un examen", description = "Permet à un biologiste ou utilisateur habilité de saisir et valider les résultats d'un examen.")
    public void uploadResults(
            @Valid @RequestBody LabResultUploadRequest request,
            Authentication authentication) {
        String validator = (request.validatorName() != null && !request.validatorName().isBlank())
                ? request.validatorName()
                : (authentication != null ? authentication.getName() : "Praticien");
        LabResultUploadRequest finalRequest = new LabResultUploadRequest(
                request.examRequestNumber(),
                validator,
                request.validatorUserId(),
                request.status(),
                request.sampleCollectedAt(),
                request.resultAt(),
                request.validatedAt(),
                request.conclusion(),
                request.results(),
                request.pdfBase64(),
                request.labOrderItemId());
        labResultService.uploadResults(finalRequest);
    }

    private com.joprelys.backend.patient.infrastructure.persistence.PatientEntity resolvePatient(UUID patientId) {
        return patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));
    }
}
