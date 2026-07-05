package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.PatientMedicalInfoService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients/{patientId}")
@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
public class PatientMedicalInfoController {

    private final PatientMedicalInfoService patientMedicalInfoService;
    private final PatientService patientService;
    private final PatientRepository patientRepository;

    public PatientMedicalInfoController(
            PatientMedicalInfoService patientMedicalInfoService,
            PatientService patientService,
            PatientRepository patientRepository) {
        this.patientMedicalInfoService = patientMedicalInfoService;
        this.patientService = patientService;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/allergies")
    public List<PatientAllergyResponse> getAllergies(@PathVariable UUID patientId) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.listAllergies(patientId);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientAllergyResponse addAllergy(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientAllergyRequest request) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.addAllergy(patientId, request);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PutMapping("/allergies/{allergyId}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientAllergyResponse updateAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @Valid @RequestBody CreatePatientAllergyRequest request) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.updateAllergy(patientId, allergyId, request);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @GetMapping("/medical-history")
    public List<PatientMedicalHistoryResponse> getMedicalHistory(@PathVariable UUID patientId) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.listMedicalHistory(patientId);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PostMapping("/medical-history")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientMedicalHistoryResponse addMedicalHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientMedicalHistoryRequest request) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.addMedicalHistory(patientId, request);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PutMapping("/medical-history/{historyId}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientMedicalHistoryResponse updateMedicalHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID historyId,
            @Valid @RequestBody CreatePatientMedicalHistoryRequest request) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.updateMedicalHistory(patientId, historyId, request);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @GetMapping("/vaccinations")
    public List<PatientVaccinationResponse> getVaccinations(@PathVariable UUID patientId) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.listVaccinations(patientId);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PostMapping("/vaccinations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientVaccinationResponse addVaccination(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientVaccinationRequest request) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.addVaccination(patientId, request);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }

    @PutMapping("/vaccinations/{vaccinationId}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientVaccinationResponse updateVaccination(
            @PathVariable UUID patientId,
            @PathVariable UUID vaccinationId,
            @Valid @RequestBody CreatePatientVaccinationRequest request) {
        patientService.validateAccess(patientId, "allergies_history");
        var patient = patientRepository.findByIdGlobally(patientId).orElseThrow();
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return patientMedicalInfoService.updateVaccination(patientId, vaccinationId, request);
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }
}
