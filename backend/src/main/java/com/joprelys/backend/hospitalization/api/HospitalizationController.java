package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.application.HospitalizationService;
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

    public HospitalizationController(HospitalizationService hospitalizationService) {
        this.hospitalizationService = hospitalizationService;
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

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        byte[] pdfBytes = hospitalizationService.loadDischargePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "fiche-sortie-" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
