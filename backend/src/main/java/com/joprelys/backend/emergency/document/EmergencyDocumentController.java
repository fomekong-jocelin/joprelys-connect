package com.joprelys.backend.emergency.document;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EmergencyDocumentController {

    private final EmergencyDocumentService emergencyDocumentService;
    private final CanonicalPatientDocumentService canonicalPatientDocumentService;

    public EmergencyDocumentController(
            EmergencyDocumentService emergencyDocumentService,
            CanonicalPatientDocumentService canonicalPatientDocumentService) {
        this.emergencyDocumentService = emergencyDocumentService;
        this.canonicalPatientDocumentService = canonicalPatientDocumentService;
    }

    @PostMapping("/emergencies/{emergencyId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DOCUMENT_MANAGE')")
    public List<EmergencyDocumentResponse> generate(@PathVariable UUID emergencyId) {
        return emergencyDocumentService.ensureBundle(emergencyId);
    }

    @GetMapping("/emergencies/{emergencyId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public List<EmergencyDocumentResponse> listEmergencyDocuments(@PathVariable UUID emergencyId) {
        return emergencyDocumentService.list(emergencyId);
    }

    @GetMapping("/patients/{patientId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public List<EmergencyDocumentResponse> listCanonicalPatientDocuments(@PathVariable UUID patientId) {
        return canonicalPatientDocumentService.list(patientId);
    }
}
