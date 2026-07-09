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
import com.joprelys.backend.hospitalization.api.CreateSurgicalConsentRequest;
import com.joprelys.backend.hospitalization.api.SurgicalConsentResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalConsentEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalConsentRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.application.PdfGeneratorService;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import com.joprelys.backend.spatial.infrastructure.persistence.*;

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
    private final VerificationUrlProvider verificationUrlProvider;
    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final SurgicalConsentRepository surgicalConsentRepository;

    @Value("${joprelys.documents.storage-dir:./storage/documents}")
    private String storageDir;

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
                                  com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository medicalDocumentRepository,
                                  VerificationUrlProvider verificationUrlProvider,
                                  WardRepository wardRepository,
                                  RoomRepository roomRepository,
                                  BedRepository bedRepository,
                                  BedAssignmentRepository bedAssignmentRepository,
                                  SurgicalConsentRepository surgicalConsentRepository) {
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
        this.verificationUrlProvider = verificationUrlProvider;
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.surgicalConsentRepository = surgicalConsentRepository;
    }

    @Transactional
    public HospitalizationResponse admitPatient(CreateHospitalizationRequest request) {
        PatientEntity patient = patientService.getPatientById(request.patientId());

        // 1. Vérifier si le patient est déjà hospitalisé dans la clinique (status EN_COURS)
        hospitalizationRepository.findActiveByPatientId(request.patientId()).ifPresent(h -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le patient est déjà admis en hospitalisation active.");
        });

        // 2. Vérifier si le lit est déjà occupé via les hospitalisations existantes
        hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber()).ifPresent(h -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le lit demandé est déjà occupé par un autre séjour.");
        });

        // 3. Vérifier/Mettre à jour le lit dans les tables de gestion spatiale (Flyway V44)
        java.util.Optional<BedEntity> bedOpt = bedRepository.findByWardRoomAndBedNumber(
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber()
        );

        BedEntity bed;
        if (bedOpt.isPresent()) {
            bed = bedOpt.get();
            if (bed.getStatus() != BedStatus.FREE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Le lit demandé n'est pas libre.");
            }
            bed.setStatus(BedStatus.OCCUPIED);
            bed = bedRepository.save(bed);
        } else {
            // Auto-provisioning pour préserver la compatibilité ascendante avec les tests existants
            WardEntity ward = wardRepository.findAll().stream()
                    .filter(w -> w.getName().equalsIgnoreCase(request.serviceName()))
                    .findFirst()
                    .orElseGet(() -> {
                        WardEntity newWard = new WardEntity(request.serviceName());
                        newWard.setOrganizationId(patient.getOrganizationId());
                        return wardRepository.save(newWard);
                    });

            RoomEntity room = roomRepository.findByWardId(ward.getId()).stream()
                    .filter(r -> r.getRoomNumber().equalsIgnoreCase(request.roomNumber()))
                    .findFirst()
                    .orElseGet(() -> {
                        RoomEntity newRoom = new RoomEntity(ward, request.roomNumber(), 2, "STANDARD");
                        newRoom.setOrganizationId(patient.getOrganizationId());
                        return roomRepository.save(newRoom);
                    });

            BedEntity newBed = new BedEntity(room, request.bedNumber());
            newBed.setStatus(BedStatus.OCCUPIED);
            newBed.setOrganizationId(patient.getOrganizationId());
            bed = bedRepository.save(newBed);
        }

        // 4. Générer le numéro unique
        Long seqVal = hospitalizationRepository.getNextHospitalizationNumberSequenceValue();
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String hospitalizationNumber = String.format("HOSP-%s-%06d", dateStr, seqVal);

        // 5. Créer l'hospitalisation
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

        // 6. Enregistrer l'assignation spatiale
        BedAssignmentEntity assignment = new BedAssignmentEntity(saved.getId(), bed);
        assignment.setOrganizationId(saved.getOrganizationId());
        bedAssignmentRepository.save(assignment);

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

        boolean againstMedicalAdvice = request.againstMedicalAdvice() != null && request.againstMedicalAdvice();
        String finalStatus = againstMedicalAdvice ? "SORTI_CONTRE_AVIS" : "SORTI";

        // 1. Déclarer la sortie de l'entité
        entity.discharge(request.dischargeDiagnosis(), request.dischargeInstructions(), "TEMP_PATH", finalStatus);

        // 2. Générer le QR Code de vérification publique pour le PDF de sortie
        String verificationUrl = verificationUrlProvider.getVerificationUrl("hospitalization/" + entity.getId());
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
            String docVerificationUrl = verificationUrlProvider.getVerificationUrl("verify/" + doc.getId());
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

            // Libérer l'assignation spatiale du lit (Flyway V44)
            bedAssignmentRepository.findActiveByHospitalizationId(id).ifPresent(assignment -> {
                assignment.setReleasedAt(java.time.Instant.now());
                bedAssignmentRepository.save(assignment);

                BedEntity bed = assignment.getBed();
                bed.setStatus(BedStatus.CLEANING);
                bedRepository.save(bed);
            });

            entity.discharge(request.dischargeDiagnosis(), request.dischargeInstructions(), filePath.toString(), finalStatus);
            entity.setDocumentId(savedDoc.getId());
            HospitalizationEntity saved = hospitalizationRepository.save(entity);

            UserAccountEntity actor = getCurrentUser();
            if (actor != null) {
                String action = againstMedicalAdvice ? "DISCHARGE_AGAINST_ADVICE" : "DISCHARGE";
                String details = againstMedicalAdvice ? "Sortie contre avis médical du patient." : "Déclaration de sortie d'hospitalisation avec génération de la fiche de sortie PDF.";
                auditService.logSuccess(
                        actor.getId(),
                        actor.getOrganizationId(),
                        saved.getPatientId(),
                        "HOSPITALIZATION",
                        saved.getId(),
                        action,
                        details
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

    @Transactional(readOnly = true)
    public byte[] loadEntryPdf(UUID id) {
        HospitalizationEntity entity = hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        PatientEntity patient = patientService.getPatientById(entity.getPatientId());
        UUID orgId = entity.getOrganizationId();

        OrganizationEntity organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        // Générer le QR Code de vérification publique pour le PDF d'entrée
        String verificationUrl = verificationUrlProvider.getVerificationUrl("hospitalization/" + entity.getId());
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);

        // Générer le PDF
        byte[] pdfBytes = pdfGeneratorService.generateHospitalizationEntryPdf(
                entity,
                patient,
                organization.getName(),
                organization.getAddress(),
                organization.getPhone(),
                qrCodeBytes
        );

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    entity.getPatientId(),
                    "HOSPITALIZATION",
                    entity.getId(),
                    "DOWNLOAD_ENTRY_PDF",
                    "Téléchargement du billet d'entrée d'hospitalisation."
            );
        }

        return pdfBytes;
    }

    @Transactional
    public SurgicalConsentResponse addConsent(UUID hospitalizationId, String consentType, boolean patientSignaturePresent, String witnessName, MultipartFile file) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hospitalization.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'hospitalisation n'est pas active.");
        }

        UUID documentId = null;
        if (file != null && !file.isEmpty()) {
            try {
                byte[] fileBytes = file.getBytes();
                Path storagePath = Paths.get(storageDir);
                if (!Files.exists(storagePath)) {
                    Files.createDirectories(storagePath);
                }
                String extension = ".pdf";
                if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
                    extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                }
                Path filePath = storagePath.resolve("consent-" + UUID.randomUUID() + extension);
                Files.write(filePath, fileBytes);

                // Calculer le hash SHA-256 du fichier
                String hash = "";
                try {
                    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                    byte[] hashBytes = digest.digest(fileBytes);
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : hashBytes) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }
                    hash = hexString.toString();
                } catch (java.security.NoSuchAlgorithmException ignored) {}

                // Charger la visite
                com.joprelys.backend.visit.infrastructure.persistence.VisitEntity visit = visitRepository.findById(hospitalization.getVisitId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite associée à l'hospitalisation introuvable."));

                // Créer le document médical
                String docNum = documentNumberGenerator.generateNextDocumentNumber();
                com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity doc = new com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity(
                        visit,
                        docNum,
                        filePath.toString(),
                        com.joprelys.backend.visit.infrastructure.persistence.DocumentType.CONSENTEMENT_SIGNE
                );
                doc.setHash(hash);
                doc.setVersion(1);

                // Renseigner les URLs et QR code
                String docVerificationUrl = verificationUrlProvider.getVerificationUrl("verify/" + doc.getId());
                String qrCodeUrl = "/api/public/documents/" + doc.getId() + "/qr";
                doc.setVerificationUrl(docVerificationUrl);
                doc.setQrCodeUrl(qrCodeUrl);

                UserAccountEntity actor = getCurrentUser();
                if (actor != null) {
                    doc.setAuthorUserId(actor.getId());
                }

                com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity savedDoc = medicalDocumentRepository.save(doc);
                documentId = savedDoc.getId();

            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store consent file on disk", e);
            }
        }

        SurgicalConsentEntity consent = new SurgicalConsentEntity(
                hospitalizationId,
                consentType,
                patientSignaturePresent,
                witnessName
        );
        if (documentId != null) {
            consent.setDocumentId(documentId);
        }

        SurgicalConsentEntity saved = surgicalConsentRepository.save(consent);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hospitalization.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "ADD_SURGICAL_CONSENT",
                    "Ajout d'un consentement opératoire de type : " + consentType
            );
        }

        return SurgicalConsentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SurgicalConsentResponse> getConsents(UUID hospitalizationId) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        return surgicalConsentRepository.findByHospitalizationId(hospitalizationId).stream()
                .map(SurgicalConsentResponse::fromEntity)
                .toList();
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
