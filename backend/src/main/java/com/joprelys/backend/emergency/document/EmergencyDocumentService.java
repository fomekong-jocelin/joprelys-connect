package com.joprelys.backend.emergency.document;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.medicolegal.api.EmergencyMedicoLegalResponse;
import com.joprelys.backend.emergency.medicolegal.application.EmergencyMedicoLegalService;
import com.joprelys.backend.visit.application.DocumentNumberGenerator;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmergencyDocumentService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final List<DocumentType> BUNDLE_TYPES = List.of(
            DocumentType.FICHE_URGENCE,
            DocumentType.FEUILLE_REANIMATION,
            DocumentType.CONSTAT_INCAPACITE_URGENCE,
            DocumentType.FICHE_TIERS_URGENCE,
            DocumentType.INVENTAIRE_EFFETS_URGENCE);

    private final EmergencyRepository emergencyRepository;
    private final EmergencyMedicoLegalService medicoLegalService;
    private final VisitRepository visitRepository;
    private final OrganizationRepository organizationRepository;
    private final MedicalDocumentRepository documentRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final VerificationUrlProvider verificationUrlProvider;
    private final EmergencyDocumentPdfService pdfService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    @Value("${joprelys.documents.storage-dir:./storage/documents}")
    private String storageDir;

    public EmergencyDocumentService(
            EmergencyRepository emergencyRepository,
            EmergencyMedicoLegalService medicoLegalService,
            VisitRepository visitRepository,
            OrganizationRepository organizationRepository,
            MedicalDocumentRepository documentRepository,
            DocumentNumberGenerator documentNumberGenerator,
            QrCodeGeneratorService qrCodeGeneratorService,
            VerificationUrlProvider verificationUrlProvider,
            EmergencyDocumentPdfService pdfService,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.emergencyRepository = emergencyRepository;
        this.medicoLegalService = medicoLegalService;
        this.visitRepository = visitRepository;
        this.organizationRepository = organizationRepository;
        this.documentRepository = documentRepository;
        this.documentNumberGenerator = documentNumberGenerator;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.verificationUrlProvider = verificationUrlProvider;
        this.pdfService = pdfService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<EmergencyDocumentResponse> ensureBundle(UUID emergencyId) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(emergencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urgence introuvable."));
        if (emergency.getVisitId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'urgence doit être associée à une visite avant la génération documentaire.");
        }
        VisitEntity visit = visitRepository.findById(emergency.getVisitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite d'urgence introuvable."));
        OrganizationEntity organization = organizationRepository.findById(emergency.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clinique introuvable."));
        EmergencyMedicoLegalResponse medicoLegal = medicoLegalService.getDossier(emergencyId);

        return BUNDLE_TYPES.stream()
                .map(type -> ensureDocument(type, emergency, visit, organization, medicoLegal))
                .map(EmergencyDocumentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmergencyDocumentResponse> list(UUID emergencyId) {
        EmergencyEntity emergency = emergencyRepository.findByIdWithPatientAndLogs(emergencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Urgence introuvable."));
        if (emergency.getVisitId() == null) {
            return List.of();
        }
        return BUNDLE_TYPES.stream()
                .flatMap(type -> documentRepository
                        .findAllByVisitIdAndDocumentTypeOrderByVersionDesc(emergency.getVisitId(), type)
                        .stream()
                        .limit(1))
                .map(EmergencyDocumentResponse::fromEntity)
                .toList();
    }

    private MedicalDocumentEntity ensureDocument(
            DocumentType type,
            EmergencyEntity emergency,
            VisitEntity visit,
            OrganizationEntity organization,
            EmergencyMedicoLegalResponse medicoLegal) {
        List<MedicalDocumentEntity> existing = documentRepository
                .findAllByVisitIdAndDocumentTypeOrderByVersionDesc(visit.getId(), type);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        MedicalDocumentEntity document = new MedicalDocumentEntity(
                visit,
                documentNumberGenerator.generateNextDocumentNumber(),
                "TEMP_PATH",
                type);
        UserAccountEntity actor = currentUser();
        if (actor != null) {
            document.setAuthorUserId(actor.getId());
        }
        document.setVersion(1);
        String verificationUrl = verificationUrlProvider.getVerificationUrl("verify/" + document.getId());
        document.setVerificationUrl(verificationUrl);
        document.setQrCodeUrl("/api/public/documents/" + document.getId() + "/qr");
        byte[] qrCode = qrCodeGeneratorService.generateQrCode(verificationUrl, 180, 180);
        byte[] pdf = pdfService.generate(
                title(type),
                emergency.getPatient(),
                emergency,
                organization,
                qrCode,
                sections(type, emergency, medicoLegal));
        document.setHash(sha256(pdf));
        document.setFilePath(writeFile(document.getDocumentNumber() + ".pdf", pdf).toAbsolutePath().toString());
        MedicalDocumentEntity saved = documentRepository.save(document);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    emergency.getPatient().getId(),
                    "EMERGENCY_DOCUMENT",
                    saved.getId(),
                    "GENERATE_" + type,
                    "Génération du document d'urgence " + saved.getDocumentNumber());
        }
        return saved;
    }

    private List<EmergencyDocumentPdfService.Section> sections(
            DocumentType type,
            EmergencyEntity emergency,
            EmergencyMedicoLegalResponse medicoLegal) {
        return switch (type) {
            case FICHE_URGENCE -> List.of(
                    new EmergencyDocumentPdfService.Section(
                            "Accueil et triage",
                            "Mode d'arrivée : " + value(emergency.getArrivalMode())
                                    + "\nMotif principal : " + value(emergency.getChiefComplaint())
                                    + "\nNiveau de triage : " + value(emergency.getTriageLevel())
                                    + "\nÉtat hémodynamique : " + value(emergency.getHemodynamicStatus())),
                    new EmergencyDocumentPdfService.Section(
                            "Constantes initiales",
                            "TA : " + value(emergency.getInitialBpSystolic()) + "/"
                                    + value(emergency.getInitialBpDiastolic())
                                    + "\nFréquence cardiaque : " + value(emergency.getInitialHr())
                                    + "\nTempérature : " + value(emergency.getInitialTemp())),
                    new EmergencyDocumentPdfService.Section(
                            "Orientation",
                            "Stabilisation : " + format(emergency.getStabilizedAt())
                                    + "\nOrientation : " + value(emergency.getOrientation())));
            case FEUILLE_REANIMATION -> List.of(new EmergencyDocumentPdfService.Section(
                    "Actes et administrations",
                    emergency.getResuscitationLogs().isEmpty()
                            ? "Aucun acte de réanimation enregistré."
                            : emergency.getResuscitationLogs().stream()
                                    .map(item -> format(item.getAdministeredAt()) + " — "
                                            + item.getActionType() + " — " + item.getDescription()
                                            + quantity(item.getQuantity(), item.getUnit()))
                                    .reduce((left, right) -> left + "\n" + right)
                                    .orElse("Aucun acte de réanimation enregistré.")));
            case CONSTAT_INCAPACITE_URGENCE -> List.of(
                    new EmergencyDocumentPdfService.Section(
                            "Capacité et conscience",
                            medicoLegal.capacityHistory().isEmpty()
                                    ? "Aucune évaluation de capacité enregistrée."
                                    : medicoLegal.capacityHistory().stream()
                                            .map(item -> format(item.effectiveAt()) + " — " + item.status()
                                                    + " — " + value(item.consciousnessLevel())
                                                    + " — " + value(item.clinicalReason()))
                                            .reduce((left, right) -> left + "\n" + right)
                                            .orElse("Aucune évaluation de capacité enregistrée.")),
                    new EmergencyDocumentPdfService.Section(
                            "Base légale d'urgence",
                            medicoLegal.legalBases().isEmpty()
                                    ? "Aucune base légale enregistrée."
                                    : medicoLegal.legalBases().stream()
                                            .map(item -> format(item.startsAt()) + " — " + item.basisType()
                                                    + " — " + value(item.justification())
                                                    + " — Actes couverts : " + item.coveredActs())
                                            .reduce((left, right) -> left + "\n" + right)
                                            .orElse("Aucune base légale enregistrée.")));
            case FICHE_TIERS_URGENCE -> List.of(
                    new EmergencyDocumentPdfService.Section(
                            "Déclarants et accompagnants",
                            medicoLegal.thirdParties().isEmpty()
                                    ? "Aucun tiers enregistré."
                                    : medicoLegal.thirdParties().stream()
                                            .map(item -> value(item.fullName()) + " — " + item.qualities()
                                                    + " — " + value(item.relationshipToPatient())
                                                    + " — " + value(item.phone()))
                                            .reduce((left, right) -> left + "\n" + right)
                                            .orElse("Aucun tiers enregistré.")),
                    new EmergencyDocumentPdfService.Section(
                            "Déclarations d'identité",
                            medicoLegal.identityStatements().isEmpty()
                                    ? "Aucune déclaration enregistrée."
                                    : medicoLegal.identityStatements().stream()
                                            .map(item -> format(item.declaredAt()) + " — " + item.fieldName()
                                                    + " : " + item.value() + " — Confiance : " + item.confidenceLevel())
                                            .reduce((left, right) -> left + "\n" + right)
                                            .orElse("Aucune déclaration enregistrée.")));
            case INVENTAIRE_EFFETS_URGENCE -> List.of(new EmergencyDocumentPdfService.Section(
                    "Inventaire et chaîne de possession",
                    medicoLegal.belongings().isEmpty()
                            ? "Aucun effet personnel enregistré."
                            : medicoLegal.belongings().stream()
                                    .map(item -> item.category() + " — " + item.description()
                                            + " — Qté : " + item.quantity()
                                            + " — Scellé : " + value(item.sealNumber())
                                            + " — Statut : " + item.custodyStatus()
                                            + transferHistory(item))
                                    .reduce((left, right) -> left + "\n\n" + right)
                                    .orElse("Aucun effet personnel enregistré.")));
            default -> throw new IllegalArgumentException("Type de document d'urgence non pris en charge : " + type);
        };
    }

    private String transferHistory(EmergencyMedicoLegalResponse.Belonging item) {
        if (item.transfers().isEmpty()) {
            return "";
        }
        return item.transfers().stream()
                .map(transfer -> "\n  " + format(transfer.occurredAt()) + " — " + transfer.action()
                        + " — Destinataire : " + value(transfer.recipientName()))
                .reduce("", String::concat);
    }

    private String title(DocumentType type) {
        return switch (type) {
            case FICHE_URGENCE -> "FICHE D'URGENCE";
            case FEUILLE_REANIMATION -> "FEUILLE DE RÉANIMATION";
            case CONSTAT_INCAPACITE_URGENCE -> "CONSTAT D'INCAPACITÉ ET BASE D'URGENCE";
            case FICHE_TIERS_URGENCE -> "FICHE DU DÉCLARANT / ACCOMPAGNANT";
            case INVENTAIRE_EFFETS_URGENCE -> "INVENTAIRE ET REÇU DES EFFETS PERSONNELS";
            default -> type.name();
        };
    }

    private Path writeFile(String fileName, byte[] content) {
        try {
            Path directory = Paths.get(storageDir);
            Files.createDirectories(directory);
            Path path = directory.resolve(fileName);
            Files.write(path, content);
            return path;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer le document d'urgence.",
                    exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible.", exception);
        }
    }

    private UserAccountEntity currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElse(null);
    }

    private static String value(Object value) {
        return value == null || value.toString().isBlank() ? "Non renseigné" : value.toString();
    }

    private static String format(java.time.Instant instant) {
        return instant == null ? "Non renseigné" : DATE_TIME.format(instant);
    }

    private static String quantity(Object quantity, String unit) {
        return quantity == null ? "" : " — " + quantity + " " + value(unit);
    }
}
