package com.joprelys.backend.visit.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderItemEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.visit.infrastructure.persistence.VisitCareStage;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fin anticipée d'une visite (ADR-20261017 D-08) : patient parti sans être vu, erreur d'enregistrement, refus.
 *
 * <p>L'accueil n'annule qu'une visite sans donnée clinique ; au-delà, l'annulation reste un acte du médecin.
 * Les demandes d'examens non commencées et les factures en attente de la visite sont annulées avec elle.</p>
 */
@Service
public class VisitAbandonmentService {

    public static final Set<String> REASONS = Set.of("LEFT_WITHOUT_BEING_SEEN", "REGISTRATION_ERROR", "PATIENT_REFUSED");
    private static final Set<LabOrderStatus> NOT_STARTED = Set.of(
            LabOrderStatus.REQUESTED, LabOrderStatus.AWAITING_PAYMENT, LabOrderStatus.PAID);

    private final VisitRepository visitRepository;
    private final ConsultationRepository consultationRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final LabOrderRepository labOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public VisitAbandonmentService(
            VisitRepository visitRepository,
            ConsultationRepository consultationRepository,
            HospitalizationRepository hospitalizationRepository,
            LabOrderRepository labOrderRepository,
            InvoiceRepository invoiceRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.visitRepository = visitRepository;
        this.consultationRepository = consultationRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.labOrderRepository = labOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    /** Abandon par l'accueil, avant toute prise en charge clinique. */
    @Transactional
    public VisitEntity abandon(UUID visitId, String reason, String note, String actorEmail) {
        VisitEntity visit = requireActiveVisit(visitId);
        if (visit.getCareStage() == VisitCareStage.EN_CONSULTATION
                || consultationRepository.findByVisitId(visitId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le patient a déjà été pris en charge : seul le médecin peut annuler cette visite.");
        }
        return cancel(visit, reason, note, actorEmail, "VISIT_ABANDONED");
    }

    /** Annulation par un utilisateur habilité à gérer le cycle des visites. */
    @Transactional
    public VisitEntity cancelByManager(UUID visitId, String reason, String note, String actorEmail) {
        return cancel(requireActiveVisit(visitId), reason == null ? "REGISTRATION_ERROR" : reason, note, actorEmail, "VISIT_CANCELLED");
    }

    private VisitEntity cancel(VisitEntity visit, String reason, String note, String actorEmail, String action) {
        String normalizedReason = reason == null ? "" : reason.trim().toUpperCase(java.util.Locale.ROOT);
        if (!REASONS.contains(normalizedReason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motif d'annulation de visite invalide.");
        }
        if (hospitalizationRepository.existsByVisitIdAndStatus(visit.getId(), "EN_COURS")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette visite est liée à une hospitalisation en cours et ne peut pas être annulée.");
        }
        UserAccountEntity actor = userAccountRepository.findByEmail(actorEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
        Instant now = Instant.now();
        String normalizedNote = note == null || note.isBlank() ? null : note.trim();
        visit.cancel(normalizedReason, normalizedNote, actor.getId(), now);

        int cancelledExams = 0;
        for (LabOrderEntity order : labOrderRepository.findByVisitId(visit.getId())) {
            if (!NOT_STARTED.contains(order.getStatus())) {
                continue;
            }
            for (LabOrderItemEntity item : order.getItems()) {
                if (NOT_STARTED.contains(item.getStatus())) {
                    item.applyStatus(LabOrderStatus.CANCELLED, now);
                }
            }
            order.setStatus(LabOrderStatus.CANCELLED);
            cancelledExams++;
        }
        int cancelledInvoices = 0;
        for (var invoice : invoiceRepository.findAllByVisitId(visit.getId())) {
            if (invoice.getStatus() == InvoiceStatus.PENDING) {
                invoice.setStatus(InvoiceStatus.CANCELLED);
                cancelledInvoices++;
            }
        }
        VisitEntity saved = visitRepository.save(visit);
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), visit.getPatient().getId(), "VISIT",
                visit.getId(), action, normalizedReason + (normalizedNote == null ? "" : " — " + normalizedNote)
                        + " ; demandes d'examens annulées : " + cancelledExams
                        + " ; factures en attente annulées : " + cancelledInvoices);
        saved.getPatient().getFullName();
        return saved;
    }

    private VisitEntity requireActiveVisit(UUID visitId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seule une visite active peut être annulée.");
        }
        return visit;
    }
}
