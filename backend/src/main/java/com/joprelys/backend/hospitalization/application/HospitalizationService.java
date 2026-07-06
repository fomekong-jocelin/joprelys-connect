package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalization.api.CreateHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.DischargeHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationNoteResponse;
import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.application.PdfGeneratorService;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class HospitalizationService {

    private final HospitalizationRepository hospitalizationRepository;
    private final HospitalizationNoteRepository hospitalizationNoteRepository;
    private final PatientService patientService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final OrganizationRepository organizationRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final com.joprelys.backend.visit.application.DocumentNumberGenerator documentNumberGenerator;
    private final com.joprelys.backend.visit.infrastructure.persistence.VisitRepository visitRepository;
    private final com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository medicalDocumentRepository;

    @Value("${joprelys.documents.storage-dir:./storage/documents}")
    private String storageDir;

    @Value("${joprelys.documents.verification-base-url:http://localhost:4200/verify}")
    private String verificationBaseUrl;

    public HospitalizationService(HospitalizationRepository hospitalizationRepository,
                                  HospitalizationNoteRepository hospitalizationNoteRepository,
                                  PatientService patientService,
                                  UserAccountRepository userAccountRepository,
                                  AuditService auditService,
                                  OrganizationRepository organizationRepository,
                                  PdfGeneratorService pdfGeneratorService,
                                  QrCodeGeneratorService qrCodeGeneratorService,
                                  com.joprelys.backend.visit.application.DocumentNumberGenerator documentNumberGenerator,
                                  com.joprelys.backend.visit.infrastructure.persistence.VisitRepository visitRepository,
                                  com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository medicalDocumentRepository) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.hospitalizationNoteRepository = hospitalizationNoteRepository;
        this.patientService = patientService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.organizationRepository = organizationRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.documentNumberGenerator = documentNumberGenerator;
        this.visitRepository = visitRepository;
        this.medicalDocumentRepository = medicalDocumentRepository;
    }

    @Transactional
    public HospitalizationResponse admitPatient(CreateHospitalizationRequest request) {
        PatientEntity patient = patientService.getPatientById(request.patientId());

        // 1. Vérifier si le patient est déjà hospitalisé dans la clinique (status EN_COURS)
        hospitalizationRepository.findActiveByPatientId(request.patientId()).ifPresent(h -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le patient est déjà admis en hospitalisation active.");
        });

        // 2. Vérifier si le lit est déjà occupé
        hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber()).ifPresent(h -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le lit demandé est déjà occupé par un autre séjour.");
        });

        // 3. Générer le numéro unique
        Long seqVal = hospitalizationRepository.getNextHospitalizationNumberSequenceValue();
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String hospitalizationNumber = String.format("HOSP-%s-%06d", dateStr, seqVal);

        // 4. Créer l'hospitalisation
        HospitalizationEntity entity = new HospitalizationEntity(
                request.patientId(),
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber(),
                request.admissionReason(),
                hospitalizationNumber,
                request.visitId(),
                request.responsiblePractitionerId()
        );

        HospitalizationEntity saved = hospitalizationRepository.save(entity);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    request.patientId(),
                    "HOSPITALIZATION",
                    saved.getId(),
                    "ADMISSION",
                    "Admission en hospitalisation. Service : " + saved.getServiceName() + " | Chambre : " + saved.getRoomNumber() + " | Lit : " + saved.getBedNumber()
            );
        }

        return HospitalizationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<HospitalizationResponse> listHospitalizations(UUID patientId) {
        patientService.getPatientById(patientId);
        return hospitalizationRepository.findByPatientIdOrderByAdmittedAtDesc(patientId).stream()
                .map(HospitalizationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public HospitalizationResponse getHospitalizationDetails(UUID id) {
        HospitalizationEntity entity = hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));
        patientService.getPatientById(entity.getPatientId());
        return HospitalizationResponse.fromEntity(entity);
    }

    @Transactional
    public HospitalizationNoteResponse addNote(UUID id, String noteContent) {
        HospitalizationEntity entity = hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        patientService.getPatientById(entity.getPatientId());

        UserAccountEntity actor = getCurrentUser();
        String authorName = actor != null ? actor.getDisplayName() : "Système";

        HospitalizationNoteEntity note = new HospitalizationNoteEntity(id, authorName, noteContent);
        HospitalizationNoteEntity saved = hospitalizationNoteRepository.save(note);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    entity.getPatientId(),
                    "HOSPITALIZATION",
                    entity.getId(),
                    "ADD_NOTE",
                    "Ajout d'une note journalière d'évolution par : " + authorName
            );
        }

        return HospitalizationNoteResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<HospitalizationNoteResponse> getNotes(UUID id) {
        HospitalizationEntity entity = hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        patientService.getPatientById(entity.getPatientId());

        return hospitalizationNoteRepository.findByHospitalizationIdOrderByCreatedAtDesc(id).stream()
                .map(HospitalizationNoteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public HospitalizationResponse dischargePatient(UUID id, DischargeHospitalizationRequest request) {
        HospitalizationEntity entity = hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(entity.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'hospitalisation est déjà clôturée.");
        }

        PatientEntity patient = patientService.getPatientById(entity.getPatientId());
        UUID orgId = entity.getOrganizationId();

        OrganizationEntity organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        // 1. Déclarer la sortie de l'entité
        entity.discharge(request.dischargeDiagnosis(), request.dischargeInstructions(), "TEMP_PATH");

        // 2. Générer le QR Code de vérification publique pour le PDF de sortie
        String verificationUrl = verificationBaseUrl + "/hospitalization/" + entity.getId();
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);

        // 3. Générer le PDF
        byte[] pdfBytes = pdfGeneratorService.generateHospitalizationDischargePdf(
                entity,
                patient,
                organization.getName(),
                organization.getAddress(),
                organization.getPhone(),
                qrCodeBytes
        );

        // 4. Écrire le PDF sur disque
        try {
            Path storagePath = Paths.get(storageDir);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            Path filePath = storagePath.resolve("discharge-" + entity.getId() + ".pdf");
            Files.write(filePath, pdfBytes);

            // Calculer le hash SHA-256 du PDF
            String hash = "";
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = digest.digest(pdfBytes);
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                hash = hexString.toString();
            } catch (java.security.NoSuchAlgorithmException ignored) {}

            // Charger la visite
            com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = visitRepository.findById(entity.getVisitId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite associée à l'hospitalisation introuvable."));

            // Créer le document médical
            String docNum = documentNumberGenerator.generateNextDocumentNumber();
            com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity doc = new com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity(
                    visit,
                    docNum,
                    filePath.toString(),
                    com.joprelys.backend.visit.infrastructure.persistence.DocumentType.FICHE_SORTIE
            );
            doc.setHash(hash);

            // Renseigner les URLs et QR code
            String docVerificationUrl = verificationBaseUrl + "/verify/" + doc.getId();
            String qrCodeUrl = "/api/public/documents/" + doc.getId() + "/qr";
            doc.setVerificationUrl(docVerificationUrl);
            doc.setQrCodeUrl(qrCodeUrl);

            // Auteur
            if (entity.getResponsiblePractitionerId() != null) {
                doc.setAuthorUserId(entity.getResponsiblePractitionerId());
            } else {
                UserAccountEntity actor = getCurrentUser();
                if (actor != null) {
                    doc.setAuthorUserId(actor.getId());
                }
            }

            // Versionnement
            List<com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity> previousDocs = medicalDocumentRepository.findAllByVisitIdAndDocumentTypeOrderByVersionDesc(visit.getId(), com.joprelys.backend.visit.infrastructure.persistence.DocumentType.FICHE_SORTIE);
            if (!previousDocs.isEmpty()) {
                com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity previous = previousDocs.get(0);
                previous.setStatus(com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus.REMPLACE);
                medicalDocumentRepository.save(previous);
                doc.setPreviousDocumentId(previous.getId());
                doc.setVersion(previous.getVersion() + 1);
            } else {
                doc.setVersion(1);
            }

            com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity savedDoc = medicalDocumentRepository.save(doc);

            entity.discharge(request.dischargeDiagnosis(), request.dischargeInstructions(), filePath.toString());
            entity.setDocumentId(savedDoc.getId());
            HospitalizationEntity saved = hospitalizationRepository.save(entity);

            UserAccountEntity actor = getCurrentUser();
            if (actor != null) {
                auditService.logSuccess(
                        actor.getId(),
                        actor.getOrganizationId(),
                        saved.getPatientId(),
                        "HOSPITALIZATION",
                        saved.getId(),
                        "DISCHARGE",
                        "Déclaration de sortie d'hospitalisation avec génération de la fiche de sortie PDF."
                );
            }

            return HospitalizationResponse.fromEntity(saved);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store discharge PDF document on disk", e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] loadDischargePdf(UUID id) {
        HospitalizationEntity entity = hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        patientService.getPatientById(entity.getPatientId());

        if (entity.getPdfFilePath() == null || "TEMP_PATH".equals(entity.getPdfFilePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fiche de sortie PDF non encore générée ou indisponible.");
        }

        try {
            Path path = Paths.get(entity.getPdfFilePath());
            if (!Files.exists(path)) {
                // Essayer de chercher directement dans le dossier storageDir par nom
                Path fallbackPath = Paths.get(storageDir).resolve("discharge-" + entity.getId() + ".pdf");
                if (Files.exists(fallbackPath)) {
                    path = fallbackPath;
                } else {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier PDF de sortie introuvable sur le disque.");
                }
            }

            byte[] bytes = Files.readAllBytes(path);

            UserAccountEntity actor = getCurrentUser();
            if (actor != null) {
                auditService.logSuccess(
                        actor.getId(),
                        actor.getOrganizationId(),
                        entity.getPatientId(),
                        "HOSPITALIZATION",
                        entity.getId(),
                        "DOWNLOAD_DISCHARGE_PDF",
                        "Téléchargement du PDF de sortie d'hospitalisation."
                );
            }

            return bytes;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read discharge PDF file from disk", e);
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
