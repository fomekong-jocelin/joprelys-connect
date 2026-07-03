package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.application.PatientMedicalInfoService;
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

    public PatientMedicalInfoController(PatientMedicalInfoService patientMedicalInfoService) {
        this.patientMedicalInfoService = patientMedicalInfoService;
    }

    @GetMapping("/allergies")
    public List<PatientAllergyResponse> getAllergies(@PathVariable UUID patientId) {
        return patientMedicalInfoService.listAllergies(patientId);
    }

    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientAllergyResponse addAllergy(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientAllergyRequest request) {
        return patientMedicalInfoService.addAllergy(patientId, request);
    }

    @PutMapping("/allergies/{allergyId}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientAllergyResponse updateAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @Valid @RequestBody CreatePatientAllergyRequest request) {
        return patientMedicalInfoService.updateAllergy(patientId, allergyId, request);
    }

    @GetMapping("/medical-history")
    public List<PatientMedicalHistoryResponse> getMedicalHistory(@PathVariable UUID patientId) {
        return patientMedicalInfoService.listMedicalHistory(patientId);
    }

    @PostMapping("/medical-history")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientMedicalHistoryResponse addMedicalHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreatePatientMedicalHistoryRequest request) {
        return patientMedicalInfoService.addMedicalHistory(patientId, request);
    }

    @PutMapping("/medical-history/{historyId}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientMedicalHistoryResponse updateMedicalHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID historyId,
            @Valid @RequestBody CreatePatientMedicalHistoryRequest request) {
        return patientMedicalInfoService.updateMedicalHistory(patientId, historyId, request);
    }
}
