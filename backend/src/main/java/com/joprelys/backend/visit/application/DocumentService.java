package com.joprelys.backend.visit.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    private final MedicalDocumentRepository medicalDocumentRepository;
    private final OrganizationRepository organizationRepository;
    private final ConsultationRepository consultationRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final PdfGeneratorService pdfGeneratorService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final AuditService auditService;
    private final UserAccountRepository userAccountRepository;

    @Value("${joprelys.documents.storage-dir:./storage/documents}")
    private String storageDir;

    @Value("${joprelys.documents.verification-base-url:http://localhost:4200/verify}")
    private String verificationBaseUrl;

    public DocumentService(
            MedicalDocumentRepository medicalDocumentRepository,
            OrganizationRepository organizationRepository,
            ConsultationRepository consultationRepository,
            PrescriptionRepository prescriptionRepository,
            DocumentNumberGenerator documentNumberGenerator,
            QrCodeGeneratorService qrCodeGeneratorService,
            PdfGeneratorService pdfGeneratorService,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            AuditService auditService,
            UserAccountRepository userAccountRepository) {
        this.medicalDocumentRepository = medicalDocumentRepository;
        this.organizationRepository = organizationRepository;
        this.consultationRepository = consultationRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.documentNumberGenerator = documentNumberGenerator;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.pdfGeneratorService = pdfGeneratorService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditService = auditService;
        this.userAccountRepository = userAccountRepository;
    }

    public java.util.Optional<com.joprelys.backend.visit.api.DocumentVerificationResponse> verifyDocument(UUID documentId) {
        String sql = "SELECT d.document_number, d.status, d.created_at, " +
                     "       o.name as clinic_name, " +
                     "       p.full_name as patient_name, " +
                     "       u.display_name as doctor_name " +
                     "FROM medical_documents d " +
                     "JOIN visits v ON d.visit_id = v.id " +
                     "JOIN patients p ON v.patient_id = p.id " +
                     "JOIN organizations o ON d.organization_id = o.id " +
                     "LEFT JOIN consultations c ON c.visit_id = v.id " +
                     "LEFT JOIN users u ON c.doctor_id = u.id " +
                     "WHERE d.id = ?";

        try {
            return java.util.Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new com.joprelys.backend.visit.api.DocumentVerificationResponse(
                    rs.getString("document_number"),
                    rs.getString("status"),
                    rs.getString("clinic_name"),
                    rs.getString("doctor_name") != null ? rs.getString("doctor_name") : "Non spécifié",
                    rs.getString("patient_name"),
                    rs.getTimestamp("created_at").toInstant()
            ), documentId));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return java.util.Optional.empty();
        }
    }

    @Transactional
    public MedicalDocumentEntity generateAndSaveDocument(VisitEntity visit) {
        // 1. Résoudre l'organisation de la visite
        UUID orgId = visit.getOrganizationId();
        OrganizationEntity organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        // 2. Résoudre la consultation de la visite
        ConsultationEntity consultation = consultationRepository.findByVisitId(visit.getId())
                .orElse(null);

        // 3. Résoudre la prescription de la consultation (si présente)
        List<PrescriptionItemEntity> prescriptionItems = List.of();
        if (consultation != null) {
            Optional<PrescriptionEntity> prescriptionOpt = prescriptionRepository.findByConsultationId(consultation.getId());
            if (prescriptionOpt.isPresent()) {
                prescriptionItems = prescriptionOpt.get().getItems();
            }
        }

        // 4. Générer le numéro de document
        String documentNumber = documentNumberGenerator.generateNextDocumentNumber();

        // 5. Instancier l'entité temporairement pour avoir son ID généré (UUID opaque)
        MedicalDocumentEntity doc = new MedicalDocumentEntity(visit, documentNumber, "TEMP_PATH");
        UUID documentUuid = doc.getId();

        // 6. Générer l'URL de vérification publique et le QR code
        String verificationUrl = verificationBaseUrl + "/" + documentUuid;
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);

        // 7. Générer le PDF
        byte[] pdfBytes = pdfGeneratorService.generatePdf(
                visit,
                consultation,
                prescriptionItems,
                organization.getName(),
                organization.getAddress(),
                organization.getPhone(),
                qrCodeBytes
        );

        // 8. Écrire le fichier PDF sur disque
        try {
            Path storagePath = Paths.get(storageDir);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            Path filePath = storagePath.resolve(documentNumber + ".pdf");
            Files.write(filePath, pdfBytes);

            // 9. Mettre à jour le chemin réel et sauvegarder l'entité
            doc.setFilePath(filePath.toAbsolutePath().toString());
            var savedDoc = medicalDocumentRepository.save(doc);

            var actor = getCurrentUser();
            if (actor != null) {
                auditService.logSuccess(
                        actor.getId(),
                        actor.getOrganizationId(),
                        visit.getPatient().getId(),
                        "MEDICAL_DOCUMENT",
                        savedDoc.getId(),
                        "GENERATE_DOCUMENT",
                        "Génération du document médical n° : " + savedDoc.getDocumentNumber()
                );
            }

            return savedDoc;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store PDF document on disk", e);
        }
    }

    public byte[] loadDocumentFile(MedicalDocumentEntity doc) {
        try {
            Path path = Paths.get(doc.getFilePath());
            if (!Files.exists(path)) {
                // Stratégie de repli : chercher le fichier par son nom dans le dossier storageDir configuré
                Path fileName = path.getFileName();
                if (fileName != null) {
                    Path fallbackPath = Paths.get(storageDir).resolve(fileName);
                    if (Files.exists(fallbackPath)) {
                        path = fallbackPath;
                    } else {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier du document médical introuvable sur le disque : " + doc.getFilePath());
                    }
                } else {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier du document médical introuvable sur le disque : " + doc.getFilePath());
                }
            }
            byte[] bytes = Files.readAllBytes(path);

            var actor = getCurrentUser();
            if (actor != null) {
                auditService.logSuccess(
                        actor.getId(),
                        actor.getOrganizationId(),
                        doc.getVisit().getPatient().getId(),
                        "MEDICAL_DOCUMENT",
                        doc.getId(),
                        "DOWNLOAD_DOCUMENT",
                        "Téléchargement du document médical n° : " + doc.getDocumentNumber()
                );
            }

            return bytes;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read document file from disk", e);
        }
    }

    // -------------------------------------------------------------------------
    // STORY-0603 — Révocation et annulation de documents médicaux
    // -------------------------------------------------------------------------

    /**
     * Révoque un document (statut : REVOQUE).
     * Action réservée aux rôles MEDECIN et ADMIN_CLINIQUE.
     * Un document déjà révoqué ou annulé ne peut pas être révoqué une seconde fois.
     *
     * @param documentId UUID opaque du document médical
     * @param reason     Motif de révocation (obligatoire pour l'audit)
     * @param actorId    UUID de l'utilisateur qui effectue la révocation
     * @return DocumentStatusResponse avec les métadonnées de traçabilité
     */
    @Transactional
    public com.joprelys.backend.visit.api.DocumentStatusResponse revokeDocument(
            UUID documentId, String reason, UUID actorId) {

        // JOIN FETCH visit + patient pour éviter LazyInitializationException lors de l'audit
        MedicalDocumentEntity doc = medicalDocumentRepository.findByIdWithVisitAndPatient(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document introuvable : " + documentId));

        if (!"VALID".equals(doc.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce document ne peut pas être révoqué car son statut actuel est : " + doc.getStatus());
        }

        doc.revoke(actorId, reason, "REVOQUE");
        medicalDocumentRepository.save(doc);

        var user = userAccountRepository.findById(actorId).orElse(null);
        UUID orgId = user != null ? user.getOrganizationId() : doc.getOrganizationId();
        auditService.logSuccess(
                actorId,
                orgId,
                doc.getVisit().getPatient().getId(),
                "MEDICAL_DOCUMENT",
                doc.getId(),
                "REVOKE_DOCUMENT",
                "Révocation du document médical n° " + doc.getDocumentNumber() + ". Motif: " + reason
        );

        return new com.joprelys.backend.visit.api.DocumentStatusResponse(
                doc.getId(),
                doc.getDocumentNumber(),
                doc.getStatus(),
                doc.getRevokedAt(),
                doc.getRevokedByUserId(),
                doc.getRevocationReason());
    }

    /**
     * Annule un document (statut : ANNULE).
     * Action réservée aux rôles ADMIN_CLINIQUE et MEDECIN.
     * Différence sémantique avec la révocation :
     *   REVOQUE = document invalide mais historiquement référençable
     *   ANNULE  = document considéré comme n'ayant jamais dû exister
     *
     * @param documentId UUID opaque du document médical
     * @param reason     Motif d'annulation (obligatoire pour l'audit)
     * @param actorId    UUID de l'utilisateur qui effectue l'annulation
     * @return DocumentStatusResponse avec les métadonnées de traçabilité
     */
    @Transactional
    public com.joprelys.backend.visit.api.DocumentStatusResponse cancelDocument(
            UUID documentId, String reason, UUID actorId) {

        // JOIN FETCH visit + patient pour éviter LazyInitializationException lors de l'audit
        MedicalDocumentEntity doc = medicalDocumentRepository.findByIdWithVisitAndPatient(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Document introuvable : " + documentId));

        if ("ANNULE".equals(doc.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ce document est déjà annulé.");
        }

        doc.revoke(actorId, reason, "ANNULE");
        medicalDocumentRepository.save(doc);

        var user = userAccountRepository.findById(actorId).orElse(null);
        UUID orgId = user != null ? user.getOrganizationId() : doc.getOrganizationId();
        auditService.logSuccess(
                actorId,
                orgId,
                doc.getVisit().getPatient().getId(),
                "MEDICAL_DOCUMENT",
                doc.getId(),
                "CANCEL_DOCUMENT",
                "Annulation du document médical n° " + doc.getDocumentNumber() + ". Motif: " + reason
        );

        return new com.joprelys.backend.visit.api.DocumentStatusResponse(
                doc.getId(),
                doc.getDocumentNumber(),
                doc.getStatus(),
                doc.getRevokedAt(),
                doc.getRevokedByUserId(),
                doc.getRevocationReason());
    }

    @Transactional
    public MedicalDocumentEntity generatePrescriptionDocument(UUID prescriptionId, UUID actorUserId) {
        // 1. Charger la prescription
        PrescriptionEntity prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable."));

        // 2. Charger la visite et la consultation
        ConsultationEntity consultation = prescription.getConsultation();
        VisitEntity visit = consultation.getVisit();

        // 3. Charger l'organisation
        OrganizationEntity organization = organizationRepository.findById(visit.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation introuvable."));

        // 4. Charger le médecin prescripteur
        String doctorName = "Médecin clinicien";
        if (actorUserId != null) {
            doctorName = userAccountRepository.findById(actorUserId)
                    .map(u -> u.getDisplayName())
                    .orElse("Médecin clinicien");
        }

        // 5. Générer le numéro de document
        String documentNumber = documentNumberGenerator.generateNextDocumentNumber();

        // 6. Instancier l'entité temporairement (document_type = ORDONNANCE)
        MedicalDocumentEntity doc = new MedicalDocumentEntity(visit, documentNumber, "TEMP_PATH", "ORDONNANCE");
        UUID documentUuid = doc.getId();

        // 7. Générer l'URL de vérification publique et le QR code
        String verificationUrl = verificationBaseUrl + "/" + documentUuid;
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);

        // 8. Générer le PDF
        byte[] pdfBytes = pdfGeneratorService.generatePrescriptionPdf(
                visit,
                consultation,
                prescription,
                organization.getName(),
                organization.getAddress(),
                organization.getPhone(),
                doctorName,
                qrCodeBytes
        );

        // 9. Écrire le fichier PDF sur disque
        try {
            Path storagePath = Paths.get(storageDir);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            Path filePath = storagePath.resolve(documentNumber + ".pdf");
            Files.write(filePath, pdfBytes);

            // 10. Mettre à jour le chemin réel et sauvegarder l'entité
            doc.setFilePath(filePath.toAbsolutePath().toString());
            var savedDoc = medicalDocumentRepository.save(doc);

            // 11. Mettre à jour la prescription avec le documentId et issuedAt
            prescription.setDocumentId(savedDoc.getId());
            prescription.setIssuedAt(Instant.now());
            prescriptionRepository.save(prescription);

            // 12. Log success
            if (actorUserId != null) {
                auditService.logSuccess(
                        actorUserId,
                        visit.getOrganizationId(),
                        visit.getPatient().getId(),
                        "PRESCRIPTION",
                        prescription.getId(),
                        "GENERATE_PRESCRIPTION_DOCUMENT",
                        "Génération réussie de l'ordonnance PDF n° " + documentNumber + " pour la visite " + visit.getId()
                );
            }

            return savedDoc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save prescription document", e);
        }
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
