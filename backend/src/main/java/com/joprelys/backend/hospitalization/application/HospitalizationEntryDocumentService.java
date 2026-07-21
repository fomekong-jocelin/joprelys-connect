package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.common.application.VerificationUrlProvider;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.visit.application.DocumentNumberGenerator;
import com.joprelys.backend.visit.application.DocumentService;
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
public class HospitalizationEntryDocumentService {

    private final HospitalizationService hospitalizationService;
    private final HospitalizationRepository hospitalizationRepository;
    private final VisitRepository visitRepository;
    private final MedicalDocumentRepository documentRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final DocumentService documentService;
    private final VerificationUrlProvider verificationUrlProvider;
    private final UserAccountRepository userAccountRepository;

    @Value("${joprelys.documents.storage-dir:./storage/documents}")
    private String storageDir;

    public HospitalizationEntryDocumentService(
            HospitalizationService hospitalizationService,
            HospitalizationRepository hospitalizationRepository,
            VisitRepository visitRepository,
            MedicalDocumentRepository documentRepository,
            DocumentNumberGenerator documentNumberGenerator,
            DocumentService documentService,
            VerificationUrlProvider verificationUrlProvider,
            UserAccountRepository userAccountRepository) {
        this.hospitalizationService = hospitalizationService;
        this.hospitalizationRepository = hospitalizationRepository;
        this.visitRepository = visitRepository;
        this.documentRepository = documentRepository;
        this.documentNumberGenerator = documentNumberGenerator;
        this.documentService = documentService;
        this.verificationUrlProvider = verificationUrlProvider;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public byte[] loadOrCreate(UUID hospitalizationId) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable."));
        VisitEntity visit = visitRepository.findById(hospitalization.getVisitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite associée introuvable."));
        List<MedicalDocumentEntity> existing = documentRepository
                .findAllByVisitIdAndDocumentTypeOrderByVersionDesc(
                        visit.getId(),
                        DocumentType.FICHE_HOSPITALISATION);
        if (!existing.isEmpty()) {
            return documentService.loadDocumentFile(existing.getFirst());
        }

        byte[] pdf = hospitalizationService.loadEntryPdf(hospitalizationId);
        MedicalDocumentEntity document = new MedicalDocumentEntity(
                visit,
                documentNumberGenerator.generateNextDocumentNumber(),
                "TEMP_PATH",
                DocumentType.FICHE_HOSPITALISATION);
        document.setVersion(1);
        document.setHash(sha256(pdf));
        document.setVerificationUrl(verificationUrlProvider.getVerificationUrl("verify/" + document.getId()));
        document.setQrCodeUrl("/api/public/documents/" + document.getId() + "/qr");
        currentUserId().ifPresent(document::setAuthorUserId);
        document.setFilePath(write(document.getDocumentNumber() + ".pdf", pdf).toAbsolutePath().toString());
        MedicalDocumentEntity saved = documentRepository.save(document);
        hospitalization.setDocumentId(saved.getId());
        hospitalizationRepository.save(hospitalization);
        return pdf;
    }

    private Path write(String fileName, byte[] content) {
        try {
            Path directory = Paths.get(storageDir);
            Files.createDirectories(directory);
            Path file = directory.resolve(fileName);
            Files.write(file, content);
            return file;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'enregistrer le billet d'entrée.",
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

    private java.util.Optional<UUID> currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return java.util.Optional.empty();
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .map(user -> user.getId());
    }
}
