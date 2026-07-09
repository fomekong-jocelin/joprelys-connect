package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.application.HospitalizationService;
import com.joprelys.backend.hospitalization.application.HospitalizationCareService;
import com.joprelys.backend.hospitalization.application.OperatingReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospitalizations")
@PreAuthorize("hasAnyRole('AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN', 'ADMIN_CLINIQUE')")
public class HospitalizationController {

    private final HospitalizationService hospitalizationService;
    private final HospitalizationCareService hospitalizationCareService;
    private final OperatingReportService operatingReportService;

    public HospitalizationController(HospitalizationService hospitalizationService,
                                   HospitalizationCareService hospitalizationCareService,
                                   OperatingReportService operatingReportService) {
        this.hospitalizationService = hospitalizationService;
        this.hospitalizationCareService = hospitalizationCareService;
        this.operatingReportService = operatingReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public HospitalizationResponse admitPatient(@Valid @RequestBody CreateHospitalizationRequest request) {
        return hospitalizationService.admitPatient(request);
    }

    @GetMapping("/patient/{patientId}")
    public List<HospitalizationResponse> listHospitalizations(@PathVariable UUID patientId) {
        return hospitalizationService.listHospitalizations(patientId);
    }

    @GetMapping("/{id}")
    public HospitalizationResponse getDetails(@PathVariable UUID id) {
        return hospitalizationService.getHospitalizationDetails(id);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public HospitalizationNoteResponse addNote(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHospitalizationNoteRequest request) {
        return hospitalizationService.addNote(id, request.noteContent());
    }

    @GetMapping("/{id}/notes")
    public List<HospitalizationNoteResponse> getNotes(@PathVariable UUID id) {
        return hospitalizationService.getNotes(id);
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public HospitalizationResponse dischargePatient(
            @PathVariable UUID id,
            @Valid @RequestBody DischargeHospitalizationRequest request) {
        return hospitalizationService.dischargePatient(id, request);
    }

    @GetMapping("/{id}/entry-pdf")
    public ResponseEntity<byte[]> downloadEntryPdf(@PathVariable UUID id) {
        byte[] pdfBytes = hospitalizationService.loadEntryPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "billet-entree-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/{id}/consents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public SurgicalConsentResponse addConsent(
            @PathVariable UUID id,
            @RequestParam("consentType") String consentType,
            @RequestParam("patientSignaturePresent") boolean patientSignaturePresent,
            @RequestParam(value = "witnessName", required = false) String witnessName,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file) {
        return hospitalizationService.addConsent(id, consentType, patientSignaturePresent, witnessName, file);
    }

    @GetMapping("/{id}/consents")
    public List<SurgicalConsentResponse> getConsents(@PathVariable UUID id) {
        return hospitalizationService.getConsents(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        byte[] pdfBytes = hospitalizationService.loadDischargePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "fiche-sortie-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/daily-cares")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public DailyCareResponse addDailyCare(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDailyCareRequest request) {
        return hospitalizationCareService.addDailyCare(id, request);
    }

    @GetMapping("/{id}/daily-cares")
    public List<DailyCareResponse> getDailyCares(@PathVariable UUID id) {
        return hospitalizationCareService.getDailyCares(id);
    }

    @PostMapping("/{id}/medication-administrations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public MedicationAdministrationResponse addMedicationAdministration(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMedicationAdministrationRequest request) {
        return hospitalizationCareService.addMedicationAdministration(id, request);
    }

    @GetMapping("/{id}/medication-administrations")
    public List<MedicationAdministrationResponse> getMedicationAdministrations(@PathVariable UUID id) {
        return hospitalizationCareService.getMedicationAdministrations(id);
    }

    @PostMapping("/{id}/patient-consumptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN_CLINIQUE')")
    public PatientConsumptionResponse addPatientConsumption(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePatientConsumptionRequest request) {
        return hospitalizationCareService.addPatientConsumption(id, request);
    }

    @GetMapping("/{id}/patient-consumptions")
    public List<PatientConsumptionResponse> getPatientConsumptions(@PathVariable UUID id) {
        return hospitalizationCareService.getPatientConsumptions(id);
    }

    @PostMapping("/{id}/operating-reports")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
    public OperatingReportResponse createOperatingReport(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOperatingReportRequest request) {
        return operatingReportService.createOperatingReport(id, request);
    }

    @GetMapping("/{id}/operating-reports")
    public List<OperatingReportResponse> getOperatingReports(@PathVariable UUID id) {
        return operatingReportService.getOperatingReports(id);
    }

    @PostMapping("/operating-reports/{reportId}/validate")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE')")
    public OperatingReportResponse validateOperatingReport(@PathVariable UUID reportId) {
        return operatingReportService.validateOperatingReport(reportId);
    }
}
