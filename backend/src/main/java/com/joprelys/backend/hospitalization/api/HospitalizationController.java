package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.application.CanonicalHospitalizationQueryService;
import com.joprelys.backend.hospitalization.application.HospitalizationAdmissionService;
import com.joprelys.backend.hospitalization.application.HospitalizationCareService;
import com.joprelys.backend.hospitalization.application.HospitalizationEntryDocumentService;
import com.joprelys.backend.hospitalization.application.HospitalizationService;
import com.joprelys.backend.hospitalization.application.OperatingReportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospitalizations")
public class HospitalizationController {

    private final HospitalizationAdmissionService hospitalizationAdmissionService;
    private final HospitalizationService hospitalizationService;
    private final HospitalizationEntryDocumentService hospitalizationEntryDocumentService;
    private final CanonicalHospitalizationQueryService canonicalHospitalizationQueryService;
    private final HospitalizationCareService hospitalizationCareService;
    private final OperatingReportService operatingReportService;

    public HospitalizationController(
            HospitalizationAdmissionService hospitalizationAdmissionService,
            HospitalizationService hospitalizationService,
            HospitalizationEntryDocumentService hospitalizationEntryDocumentService,
            CanonicalHospitalizationQueryService canonicalHospitalizationQueryService,
            HospitalizationCareService hospitalizationCareService,
            OperatingReportService operatingReportService) {
        this.hospitalizationAdmissionService = hospitalizationAdmissionService;
        this.hospitalizationService = hospitalizationService;
        this.hospitalizationEntryDocumentService = hospitalizationEntryDocumentService;
        this.canonicalHospitalizationQueryService = canonicalHospitalizationQueryService;
        this.hospitalizationCareService = hospitalizationCareService;
        this.operatingReportService = operatingReportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE')")
    public HospitalizationResponse admitPatient(@Valid @RequestBody CreateHospitalizationRequest request) {
        return hospitalizationAdmissionService.admitPatient(request);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<HospitalizationResponse> listHospitalizations(@PathVariable UUID patientId) {
        return canonicalHospitalizationQueryService.list(patientId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public HospitalizationResponse getDetails(@PathVariable UUID id) {
        return hospitalizationService.getHospitalizationDetails(id);
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE')")
    public HospitalizationNoteResponse addNote(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHospitalizationNoteRequest request) {
        return hospitalizationService.addNote(id, request.noteContent());
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<HospitalizationNoteResponse> getNotes(@PathVariable UUID id) {
        return hospitalizationService.getNotes(id);
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_DISCHARGE_DECIDE')")
    public HospitalizationResponse dischargePatient(
            @PathVariable UUID id,
            @Valid @RequestBody DischargeHospitalizationRequest request) {
        return hospitalizationService.dischargePatient(id, request);
    }

    @GetMapping("/{id}/entry-pdf")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public ResponseEntity<byte[]> downloadEntryPdf(@PathVariable UUID id) {
        return pdfResponse(hospitalizationEntryDocumentService.loadOrCreate(id), "billet-entree-" + id + ".pdf");
    }

    @PostMapping(value = "/{id}/consents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE')")
    public SurgicalConsentResponse addConsent(
            @PathVariable UUID id,
            @RequestParam("consentType") String consentType,
            @RequestParam("patientSignaturePresent") boolean patientSignaturePresent,
            @RequestParam(value = "witnessName", required = false) String witnessName,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file) {
        return hospitalizationService.addConsent(id, consentType, patientSignaturePresent, witnessName, file);
    }

    @GetMapping("/{id}/consents")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<SurgicalConsentResponse> getConsents(@PathVariable UUID id) {
        return hospitalizationService.getConsents(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        return pdfResponse(hospitalizationService.loadDischargePdf(id), "fiche-sortie-" + id + ".pdf");
    }

    @PostMapping("/{id}/daily-cares")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE')")
    public DailyCareResponse addDailyCare(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDailyCareRequest request) {
        return hospitalizationCareService.addDailyCare(id, request);
    }

    @GetMapping("/{id}/daily-cares")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<DailyCareResponse> getDailyCares(@PathVariable UUID id) {
        return hospitalizationCareService.getDailyCares(id);
    }

    @PostMapping("/{id}/medication-administrations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE')")
    public MedicationAdministrationResponse addMedicationAdministration(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMedicationAdministrationRequest request) {
        return hospitalizationCareService.addMedicationAdministration(id, request);
    }

    @GetMapping("/{id}/medication-administrations")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<MedicationAdministrationResponse> getMedicationAdministrations(@PathVariable UUID id) {
        return hospitalizationCareService.getMedicationAdministrations(id);
    }

    @PostMapping("/{id}/patient-consumptions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('HOSPITALIZATION_MANAGE')")
    public PatientConsumptionResponse addPatientConsumption(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePatientConsumptionRequest request) {
        return hospitalizationCareService.addPatientConsumption(id, request);
    }

    @GetMapping("/{id}/patient-consumptions")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<PatientConsumptionResponse> getPatientConsumptions(@PathVariable UUID id) {
        return hospitalizationCareService.getPatientConsumptions(id);
    }

    @PostMapping("/{id}/operating-reports")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public OperatingReportResponse createOperatingReport(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOperatingReportRequest request) {
        return operatingReportService.createOperatingReport(id, request);
    }

    @GetMapping("/{id}/operating-reports")
    @PreAuthorize("hasAnyAuthority('CLINICAL_READ', 'HOSPITALIZATION_READ')")
    public List<OperatingReportResponse> getOperatingReports(@PathVariable UUID id) {
        return operatingReportService.getOperatingReports(id);
    }

    @PostMapping("/operating-reports/{reportId}/validate")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public OperatingReportResponse validateOperatingReport(@PathVariable UUID reportId) {
        return operatingReportService.validateOperatingReport(reportId);
    }

    private static ResponseEntity<byte[]> pdfResponse(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
