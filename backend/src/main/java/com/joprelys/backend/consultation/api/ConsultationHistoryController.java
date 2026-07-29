package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.application.ConsultationService;
import com.joprelys.backend.patient.application.PatientAccessPolicyService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients")
public class ConsultationHistoryController {

    private final ConsultationService consultationService;
    private final PatientAccessPolicyService accessPolicy;
    private final PatientRepository patientRepository;

    public ConsultationHistoryController(
            ConsultationService consultationService,
            PatientAccessPolicyService accessPolicy,
            PatientRepository patientRepository) {
        this.consultationService = consultationService;
        this.accessPolicy = accessPolicy;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/{patientId}/consultations")
    @PreAuthorize("hasAuthority('CLINICAL_READ')")
    public List<ConsultationResponse> getPatientConsultations(@PathVariable UUID patientId) {
        accessPolicy.validateAccess(patientId, "medical_records");
        var patient = patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return consultationService.getDetailedConsultationsByPatientId(patientId);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }
}
