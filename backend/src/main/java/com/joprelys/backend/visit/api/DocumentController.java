package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.application.DocumentService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final MedicalDocumentRepository medicalDocumentRepository;

    public DocumentController(
            DocumentService documentService,
            MedicalDocumentRepository medicalDocumentRepository) {
        this.documentService = documentService;
        this.medicalDocumentRepository = medicalDocumentRepository;
    }

    @GetMapping("/api/visits/{visitId}/document")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'ADMIN_CLINIQUE', 'PHARMACIEN')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID visitId) {
        MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitId(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable pour cette visite."));

        byte[] pdfBytes = documentService.loadDocumentFile(doc);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/api/public/documents/{id}/verify")
    public DocumentVerificationResponse verifyAnonymously(@PathVariable UUID id) {
        return documentService.verifyDocument(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable."));
    }
}
