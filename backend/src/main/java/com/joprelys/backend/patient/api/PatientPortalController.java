package com.joprelys.backend.patient.api;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.application.DocumentService;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
public class PatientPortalController {

    private final PatientRepository patientRepository;
    private final ConsultationRepository consultationRepository;
    private final MedicalDocumentRepository medicalDocumentRepository;
    private final DocumentService documentService;

    public PatientPortalController(
            PatientRepository patientRepository,
            ConsultationRepository consultationRepository,
            MedicalDocumentRepository medicalDocumentRepository,
            DocumentService documentService) {
        this.patientRepository = patientRepository;
        this.consultationRepository = consultationRepository;
        this.medicalDocumentRepository = medicalDocumentRepository;
        this.documentService = documentService;
    }

    @GetMapping("/me")
    public PatientPortalMeResponse getMe(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }

        PatientEntity patient = patientRepository.findByGlobalPatientNumber(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier patient introuvable."));

        List<PatientPortalMeResponse.PatientPortalConsultation> consultations = consultationRepository
                .findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(c -> {
                    MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitId(c.getVisit().getId()).orElse(null);
                    LocalDate visitDate = LocalDate.ofInstant(c.getVisit().getCreatedAt(), ZoneId.systemDefault());

                    return new PatientPortalMeResponse.PatientPortalConsultation(
                            c.getVisit().getId(),
                            c.getVisit().getVisitNumber(),
                            visitDate,
                            c.getDoctor().getDisplayName(),
                            c.getVisit().getOrientation(), // Orienté vers le service clinique comme nom de clinique/service
                            c.getDiagnosis(),
                            doc != null ? doc.getId() : null,
                            doc != null ? doc.getStatus() : null
                    );
                })
                .toList();

        return new PatientPortalMeResponse(
                patient.getId(),
                patient.getGlobalPatientNumber(),
                patient.getLocalPatientNumber(),
                patient.getFullName(),
                patient.getGender(),
                patient.getBirthDate(),
                patient.getPhone(),
                patient.getCity(),
                patient.getDistrict(),
                patient.getAddress(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                patient.getAllergies(),
                patient.getMedicalHistory(),
                consultations
        );
    }

    @GetMapping("/visits/{visitId}/document")
    public ResponseEntity<byte[]> downloadOwnDocument(@PathVariable UUID visitId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
        }

        PatientEntity patient = patientRepository.findByGlobalPatientNumber(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier patient introuvable."));

        MedicalDocumentEntity doc = medicalDocumentRepository.findByVisitId(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable pour cette visite."));

        // Vérification de sécurité de l'accès au document
        if (!doc.getVisit().getPatient().getId().equals(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à accéder à ce document.");
        }

        byte[] pdfBytes = documentService.loadDocumentFile(doc);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getDocumentNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
