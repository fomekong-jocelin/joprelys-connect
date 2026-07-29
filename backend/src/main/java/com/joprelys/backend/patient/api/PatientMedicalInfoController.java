package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.PatientAccessPolicyService;
import com.joprelys.backend.patient.application.PatientMedicalInfoService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patients/{patientId}")
@PreAuthorize("hasAuthority('CLINICAL_READ')")
public class PatientMedicalInfoController {

    private final PatientMedicalInfoService patientMedicalInfoService;
    private final PatientAccessPolicyService accessPolicy;
    private final PatientRepository patientRepository;

    public PatientMedicalInfoController(
            PatientMedicalInfoService patientMedicalInfoService,
            PatientAccessPolicyService accessPolicy,
            PatientRepository patientRepository) {
        this.patientMedicalInfoService = patientMedicalInfoService;
        this.accessPolicy = accessPolicy;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/allergies")
    public List<PatientAllergyResponse> getAllergies(@PathVariable UUID patientId) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.listAllergies(patientId));
    }

    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public PatientAllergyResponse addAllergy(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientAllergyRequest request) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.addAllergy(patientId, request));
    }

    @PutMapping("/allergies/{allergyId}")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public PatientAllergyResponse updateAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @Valid @RequestBody CreatePatientAllergyRequest request) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.updateAllergy(patientId, allergyId, request));
    }

    @GetMapping("/medical-history")
    public List<PatientMedicalHistoryResponse> getMedicalHistory(@PathVariable UUID patientId) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.listMedicalHistory(patientId));
    }

    @PostMapping("/medical-history")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public PatientMedicalHistoryResponse addMedicalHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientMedicalHistoryRequest request) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.addMedicalHistory(patientId, request));
    }

    @PutMapping("/medical-history/{historyId}")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public PatientMedicalHistoryResponse updateMedicalHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID historyId,
            @Valid @RequestBody CreatePatientMedicalHistoryRequest request) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.updateMedicalHistory(patientId, historyId, request));
    }

    @GetMapping("/vaccinations")
    public List<PatientVaccinationResponse> getVaccinations(@PathVariable UUID patientId) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.listVaccinations(patientId));
    }

    @PostMapping("/vaccinations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public PatientVaccinationResponse addVaccination(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientVaccinationRequest request) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.addVaccination(patientId, request));
    }

    @PutMapping("/vaccinations/{vaccinationId}")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public PatientVaccinationResponse updateVaccination(
            @PathVariable UUID patientId,
            @PathVariable UUID vaccinationId,
            @Valid @RequestBody CreatePatientVaccinationRequest request) {
        var patient = prepareAccess(patientId);
        return withTenant(patient, () -> patientMedicalInfoService.updateVaccination(patientId, vaccinationId, request));
    }

    @DeleteMapping("/allergies/{allergyId}")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId) {
        var patient = prepareAccess(patientId);
        withTenant(patient, () -> {
            patientMedicalInfoService.deleteAllergy(patientId, allergyId);
            return null;
        });
    }

    @DeleteMapping("/medical-history/{historyId}")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedicalHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID historyId) {
        var patient = prepareAccess(patientId);
        withTenant(patient, () -> {
            patientMedicalInfoService.deleteMedicalHistory(patientId, historyId);
            return null;
        });
    }

    private com.joprelys.backend.patient.infrastructure.persistence.PatientEntity prepareAccess(UUID patientId) {
        accessPolicy.validateAccess(patientId, "allergies_history");
        return patientRepository.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));
    }

    private <T> T withTenant(
            com.joprelys.backend.patient.infrastructure.persistence.PatientEntity patient,
            java.util.function.Supplier<T> action) {
        UUID originalTenantId = com.joprelys.backend.auth.security.TenantContext.getTenantId();
        try {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(patient.getOrganizationId());
            return action.get();
        } finally {
            com.joprelys.backend.auth.security.TenantContext.setTenantId(originalTenantId);
        }
    }
}
