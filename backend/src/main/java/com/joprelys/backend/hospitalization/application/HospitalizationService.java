package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import com.joprelys.backend.hospitalization.api.DischargeHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationNoteResponse;
import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.api.SurgicalConsentResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationNoteRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalConsentEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.SurgicalConsentRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.visit.application.DocumentNumberGenerator;
import com.joprelys.backend.visit.application.PdfGeneratorService;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus;
import com.joprelys.backend.visit.infrastructure.persistence.DocumentType;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HospitalizationService {

    private static final String ACTIVE_STATUS = "EN_COURS";
    private static final String TEMPORARY_PDF_PATH = "TEMP_PATH";

    private final HospitalizationRepository hospitalizationRepository;
    private final HospitalizationNoteRepository hospitalizationNoteRepository;
    private final PatientService patientService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final OrganizationRepository organizationRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final VisitRepository visitRepository;
    private final MedicalDocumentRepository medicalDocumentRepository;
    private final VerificationUrlProvider verificationUrlProvider;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final SurgicalConsentRepository surgicalConsentRepository;

    @Value("${joprelys.documents.storage-dir:./storage/documents}")
    private String storageDir;

    public HospitalizationService(
            HospitalizationRepository hospitalizationRepository,
            HospitalizationNoteRepository hospitalizationNoteRepository,
            PatientService patientService,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            OrganizationRepository organizationRepository,
            PdfGeneratorService pdfGeneratorService,
            QrCodeGeneratorService qrCodeGeneratorService,
            DocumentNumberGenerator documentNumberGenerator,
            VisitRepository visitRepository,
            MedicalDocumentRepository medicalDocumentRepository,
            VerificationUrlProvider verificationUrlProvider,
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
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.surgicalConsentRepository = surgicalConsentRepository;
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
        HospitalizationEntity hospitalization = requireHospitalization(id);
        patientService.getPatientById(hospitalization.getPatientId());
        return HospitalizationResponse.fromEntity(hospitalization);
    }

    @Transactional
    public HospitalizationNoteResponse addNote(UUID id, String noteContent) {
        HospitalizationEntity hospitalization = requireHospitalization(id);
        patientService.getPatientById(hospitalization.getPatientId());

        UserAccountEntity actor = currentUserOrNull();
        String authorName = actor == null ? "Système" : actor.getDisplayName();
        HospitalizationNoteEntity saved = hospitalizationNoteRepository.save(
                new HospitalizationNoteEntity(id, authorName, noteContent));

        audit(actor, hospitalization, "ADD_NOTE", "Ajout d'une note journalière d'évolution par : " + authorName);
        return HospitalizationNoteResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<HospitalizationNoteResponse> getNotes(UUID id) {
        HospitalizationEntity hospitalization = requireHospitalization(id);
        patientService.getPatientById(hospitalization.getPatientId());
        return hospitalizationNoteRepository.findByHospitalizationIdOrderByCreatedAtDesc(id).stream()
                .map(HospitalizationNoteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public HospitalizationResponse dischargePatient(UUID id, DischargeHospitalizationRequest request) {
        HospitalizationEntity hospitalization = requireHospitalization(id);
        if (!ACTIVE_STATUS.equals(hospitalization.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'hospitalisation est déjà clôturée.");
        }

        PatientEntity patient = patientService.getPatientById(hospitalization.getPatientId());
        OrganizationEntity organization = organizationRepository.findById(hospitalization.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinique introuvable."));

        boolean againstMedicalAdvice = Boolean.TRUE.equals(request.againstMedicalAdvice());
        String finalStatus = againstMedicalAdvice ? "SORTI_CONTRE_AVIS" : "SORTI";
        hospitalization.discharge(
                request.dischargeDiagnosis(),
                request.dischargeInstructions(),
                TEMPORARY_PDF_PATH,
                finalStatus);

        byte[] pdfBytes = generateDischargePdf(hospitalization, patient, organization);
        Path filePath = writeDocument("discharge-" + hospitalization.getId() + ".pdf", pdfBytes);
        MedicalDocumentEntity document = createDischargeDocument(hospitalization, filePath, pdfBytes);
        releaseBedAssignment(hospitalization.getId());

        hospitalization.discharge(
                request.dischargeDiagnosis(),
                request.dischargeInstructions(),
                filePath.toString(),
                finalStatus);
        hospitalization.setDocumentId(document.getId());
        HospitalizationEntity saved = hospitalizationRepository.save(hospitalization);

        UserAccountEntity actor = currentUserOrNull();
        String action = againstMedicalAdvice ? "DISCHARGE_AGAINST_ADVICE" : "DISCHARGE";
        String details = againstMedicalAdvice
                ? "Sortie contre avis médical du patient."
                : "Déclaration de sortie d'hospitalisation avec génération de la fiche de sortie PDF.";
        audit(actor, saved, action, details);
        return HospitalizationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public byte[] loadDischargePdf(UUID id) {
        HospitalizationEntity hospitalization = requireHospitalization(id);
        patientService.getPatientById(hospitalization.getPatientId());

        String pdfFilePath = hospitalization.getPdfFilePath();
        if (pdfFilePath == null || TEMPORARY_PDF_PATH.equals(pdfFilePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Fiche de sortie PDF non encore générée ou indisponible.");
        }

        Path configuredPath = Paths.get(pdfFilePath);
        Path fallbackPath = Paths.get(storageDir).resolve("discharge-" + hospitalization.getId() + ".pdf");
        Path existingPath = Files.exists(configuredPath) ? configuredPath : fallbackPath;
        if (!Files.exists(existingPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier PDF de sortie introuvable sur le disque.");
        }

        try {
            byte[] content = Files.readAllBytes(existingPath);
            audit(currentUserOrNull(), hospitalization, "DOWNLOAD_DISCHARGE_PDF",
                    "Téléchargement du PDF de sortie d'hospitalisation.");
            return content;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de lire la fiche de sortie sur le disque.",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] loadEntryPdf(UUID id) {
        HospitalizationEntity hospitalization = requireHospitalization(id);
        PatientEntity patient = patientService.getPatientById(hospitalization.getPatientId());
        OrganizationEntity organization = organizationRepository.findById(hospitalization.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinique introuvable."));

        String verificationUrl = verificationUrlProvider.getVerificationUrl(
                "hospitalization/" + hospitalization.getId());
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);
        byte[] pdfBytes = pdfGeneratorService.generateHospitalizationEntryPdf(
                hospitalization,
                patient,
                organization.getName(),
                organization.getAddress(),
                organization.getPhone(),
                qrCodeBytes);

        audit(currentUserOrNull(), hospitalization, "DOWNLOAD_ENTRY_PDF",
                "Téléchargement du billet d'entrée d'hospitalisation.");
        return pdfBytes;
    }

    @Transactional
    public SurgicalConsentResponse addConsent(
            UUID hospitalizationId,
            String consentType,
            boolean patientSignaturePresent,
            String witnessName,
            MultipartFile file) {
        HospitalizationEntity hospitalization = requireHospitalization(hospitalizationId);
        if (!ACTIVE_STATUS.equals(hospitalization.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'hospitalisation n'est pas active.");
        }

        UUID documentId = file == null || file.isEmpty()
                ? null
                : createConsentDocument(hospitalization, file).getId();

        SurgicalConsentEntity consent = new SurgicalConsentEntity(
                hospitalizationId,
                consentType,
                patientSignaturePresent,
                witnessName);
        consent.setDocumentId(documentId);
        SurgicalConsentEntity saved = surgicalConsentRepository.save(consent);

        audit(currentUserOrNull(), hospitalization, "ADD_SURGICAL_CONSENT",
                "Ajout d'un consentement opératoire de type : " + consentType);
        return SurgicalConsentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SurgicalConsentResponse> getConsents(UUID hospitalizationId) {
        requireHospitalization(hospitalizationId);
        return surgicalConsentRepository.findByHospitalizationId(hospitalizationId).stream()
                .map(SurgicalConsentResponse::fromEntity)
                .toList();
    }

    private byte[] generateDischargePdf(
            HospitalizationEntity hospitalization,
            PatientEntity patient,
            OrganizationEntity organization) {
        String verificationUrl = verificationUrlProvider.getVerificationUrl(
                "hospitalization/" + hospitalization.getId());
        byte[] qrCodeBytes = qrCodeGeneratorService.generateQrCode(verificationUrl, 200, 200);
        return pdfGeneratorService.generateHospitalizationDischargePdf(
                hospitalization,
                patient,
                organization.getName(),
                organization.getAddress(),
                organization.getPhone(),
                qrCodeBytes);
    }

    private MedicalDocumentEntity createDischargeDocument(
            HospitalizationEntity hospitalization,
            Path filePath,
            byte[] pdfBytes) {
        VisitEntity visit = requireVisit(hospitalization.getVisitId());
        MedicalDocumentEntity document = new MedicalDocumentEntity(
                visit,
                documentNumberGenerator.generateNextDocumentNumber(),
                filePath.toString(),
                DocumentType.FICHE_SORTIE);
        document.setHash(sha256(pdfBytes));
        setDocumentLinks(document);
        setDocumentAuthor(document, hospitalization.getResponsiblePractitionerId());
        applyNextVersion(document, visit);
        return medicalDocumentRepository.save(document);
    }

    private MedicalDocumentEntity createConsentDocument(
            HospitalizationEntity hospitalization,
            MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String extension = safeExtension(file.getOriginalFilename());
            Path filePath = writeDocument("consent-" + UUID.randomUUID() + extension, fileBytes);
            VisitEntity visit = requireVisit(hospitalization.getVisitId());

            MedicalDocumentEntity document = new MedicalDocumentEntity(
                    visit,
                    documentNumberGenerator.generateNextDocumentNumber(),
                    filePath.toString(),
                    DocumentType.CONSENTEMENT_SIGNE);
            document.setHash(sha256(fileBytes));
            document.setVersion(1);
            setDocumentLinks(document);
            setDocumentAuthor(document, null);
            return medicalDocumentRepository.save(document);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer le consentement sur le disque.",
                    exception);
        }
    }

    private void applyNextVersion(MedicalDocumentEntity document, VisitEntity visit) {
        List<MedicalDocumentEntity> previousDocuments =
                medicalDocumentRepository.findAllByVisitIdAndDocumentTypeOrderByVersionDesc(
                        visit.getId(), DocumentType.FICHE_SORTIE);
        if (previousDocuments.isEmpty()) {
            document.setVersion(1);
            return;
        }

        MedicalDocumentEntity previous = previousDocuments.getFirst();
        previous.setStatus(DocumentStatus.REMPLACE);
        medicalDocumentRepository.save(previous);
        document.setPreviousDocumentId(previous.getId());
        document.setVersion(previous.getVersion() + 1);
    }

    private void setDocumentLinks(MedicalDocumentEntity document) {
        document.setVerificationUrl(verificationUrlProvider.getVerificationUrl("verify/" + document.getId()));
        document.setQrCodeUrl("/api/public/documents/" + document.getId() + "/qr");
    }

    private void setDocumentAuthor(MedicalDocumentEntity document, UUID responsiblePractitionerId) {
        if (responsiblePractitionerId != null) {
            document.setAuthorUserId(responsiblePractitionerId);
            return;
        }
        UserAccountEntity actor = currentUserOrNull();
        if (actor != null) {
            document.setAuthorUserId(actor.getId());
        }
    }

    private void releaseBedAssignment(UUID hospitalizationId) {
        bedAssignmentRepository.findActiveByHospitalizationId(hospitalizationId).ifPresent(assignment -> {
            assignment.releaseAt(Instant.now());
            bedAssignmentRepository.save(assignment);

            BedEntity bed = assignment.getBed();
            bed.setStatus(BedStatus.CLEANING);
            bedRepository.save(bed);
        });
    }

    private Path writeDocument(String filename, byte[] content) {
        try {
            Path storagePath = Paths.get(storageDir);
            Files.createDirectories(storagePath);
            Path filePath = storagePath.resolve(filename);
            Files.write(filePath, content);
            return filePath;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer le document sur le disque.",
                    exception);
        }
    }

    private String safeExtension(String filename) {
        if (filename == null) {
            return ".pdf";
        }
        int separator = filename.lastIndexOf('.');
        if (separator < 0 || separator == filename.length() - 1) {
            return ".pdf";
        }
        String extension = filename.substring(separator + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? "." + extension : ".pdf";
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available on this JVM.", exception);
        }
    }

    private HospitalizationEntity requireHospitalization(UUID id) {
        return hospitalizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));
    }

    private VisitEntity requireVisit(UUID visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Visite associée à l'hospitalisation introuvable."));
    }

    private UserAccountEntity currentUserOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElse(null);
    }

    private void audit(
            UserAccountEntity actor,
            HospitalizationEntity hospitalization,
            String action,
            String details) {
        if (actor == null) {
            return;
        }
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                hospitalization.getPatientId(),
                "HOSPITALIZATION",
                hospitalization.getId(),
                action,
                details);
    }
}
