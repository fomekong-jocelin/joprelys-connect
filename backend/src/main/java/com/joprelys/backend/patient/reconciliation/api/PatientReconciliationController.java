package com.joprelys.backend.patient.reconciliation.api;

import com.joprelys.backend.patient.reconciliation.application.PatientReconciliationWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient-reconciliations")
@PreAuthorize("hasAuthority('PATIENT_MERGE')")
@Tag(name = "Patient reconciliation", description = "Régularisation URG-TEMP et rapprochement DPU")
public class PatientReconciliationController {

    private final PatientReconciliationWorkflowService workflowService;

    public PatientReconciliationController(PatientReconciliationWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/queue")
    @Operation(summary = "Lister les dossiers URG-TEMP à régulariser")
    public List<PatientReconciliationQueueItemResponse> listQueue() {
        return workflowService.listQueue();
    }

    @GetMapping("/{sourcePatientId}/candidates")
    @Operation(summary = "Proposer les DPU candidats avec score et raisons")
    public List<PatientReconciliationCandidateResponse> findCandidates(
            @PathVariable UUID sourcePatientId) {
        return workflowService.findCandidates(sourcePatientId);
    }

    @PostMapping("/{sourcePatientId}/decisions")
    @Operation(summary = "Enregistrer une décision humaine de régularisation")
    public PatientReconciliationDecisionResponse decide(
            @PathVariable UUID sourcePatientId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PatientReconciliationDecisionRequest request) {
        return workflowService.decide(sourcePatientId, request, idempotencyKey);
    }

    @PostMapping("/{sourcePatientId}/corrections")
    @Operation(summary = "Corriger un mauvais rapprochement sans déplacer les données historiques")
    public PatientReconciliationDecisionResponse correct(
            @PathVariable UUID sourcePatientId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PatientReconciliationCorrectionRequest request) {
        return workflowService.correct(sourcePatientId, request, idempotencyKey);
    }
}
