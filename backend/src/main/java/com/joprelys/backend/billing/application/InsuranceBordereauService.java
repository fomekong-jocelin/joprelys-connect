package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class InsuranceBordereauService {

    private static final int MONEY_SCALE = 4;

    private final InsuranceBordereauRepository bordereauRepository;
    private final InvoiceRepository invoiceRepository;
    private final InsuranceConventionRepository conventionRepository;
    private final ReceivableRepository receivableRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final InvoiceFinancialStateService financialStateService;

    public InsuranceBordereauService(InsuranceBordereauRepository bordereauRepository,
                                     InvoiceRepository invoiceRepository,
                                     InsuranceConventionRepository conventionRepository,
                                     ReceivableRepository receivableRepository,
                                     UserAccountRepository userAccountRepository,
                                     AuditService auditService,
                                     InvoiceFinancialStateService financialStateService) {
        this.bordereauRepository = bordereauRepository;
        this.invoiceRepository = invoiceRepository;
        this.conventionRepository = conventionRepository;
        this.receivableRepository = receivableRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.financialStateService = financialStateService;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur authentifié introuvable"));
    }

    private InsuranceBordereauEntity getTenantBordereau(UUID id, UUID organizationId) {
        return bordereauRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bordereau introuvable"));
    }

    @Transactional
    public InsuranceBordereauEntity generateBordereau(UUID conventionId, LocalDate startDate, LocalDate endDate) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor.getOrganizationId();

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de début doit précéder la date de fin");
        }

        InsuranceConventionEntity convention = conventionRepository.findById(conventionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention d'assurance introuvable"));

        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        List<InvoiceEntity> invoices = invoiceRepository.findInvoicesForBordereau(
                conventionId,
                List.of(InvoiceStatus.VALIDATED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.PAID),
                startInstant,
                endInstant,
                orgId
        );

        if (invoices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune facture validée avec part assurance n'est disponible pour cette convention sur la période sélectionnée.");
        }

        Long seqVal = bordereauRepository.getNextBordereauNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String bordereauNumber = String.format("BORD-%s-%06d", dateStr, seqVal);

        BigDecimal totalAmount = invoices.stream()
                .map(InvoiceEntity::getInsuranceShare)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity(
                bordereauNumber,
                convention,
                startDate,
                endDate,
                totalAmount
        );
        bordereau.setOrganizationId(orgId);
        InsuranceBordereauEntity savedBordereau = bordereauRepository.save(bordereau);

        for (InvoiceEntity invoice : invoices) {
            invoice.setInsuranceBordereauId(savedBordereau.getId());
            invoiceRepository.save(invoice);
        }

        audit(actor, savedBordereau, "GENERATE_BORDEREAU",
                "Génération du bordereau N° " + savedBordereau.getBordereauNumber()
                        + " pour la convention " + convention.getName()
                        + " d'un montant total de " + totalAmount + " FCFA.");
        return savedBordereau;
    }

    @Transactional(readOnly = true)
    public List<InsuranceBordereauEntity> listBordereaux() {
        UserAccountEntity actor = getCurrentUser();
        return bordereauRepository.findByOrganizationIdOrderByCreatedAtDesc(actor.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public InsuranceBordereauEntity getBordereau(UUID id) {
        UserAccountEntity actor = getCurrentUser();
        return getTenantBordereau(id, actor.getOrganizationId());
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> getBordereauInvoices(UUID bordereauId) {
        UserAccountEntity actor = getCurrentUser();
        getTenantBordereau(bordereauId, actor.getOrganizationId());
        return invoiceRepository.findByInsuranceBordereauIdAndOrganizationId(bordereauId, actor.getOrganizationId());
    }

    @Transactional
    public InsuranceBordereauEntity markAsSent(UUID id) {
        UserAccountEntity actor = getCurrentUser();
        InsuranceBordereauEntity bordereau = getTenantBordereau(id, actor.getOrganizationId());
        requireStatus(bordereau, InsuranceBordereauStatus.DRAFT,
                "Le bordereau doit être au statut DRAFT pour être envoyé.");

        bordereau.setStatus(InsuranceBordereauStatus.SENT);
        bordereau.setSentAt(Instant.now());
        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);
        audit(actor, saved, "SEND_BORDEREAU", "Bordereau N° " + saved.getBordereauNumber() + " marqué comme envoyé.");
        return saved;
    }

    @Transactional
    public InsuranceBordereauEntity markAsReceived(UUID id, String insurerReference) {
        UserAccountEntity actor = getCurrentUser();
        InsuranceBordereauEntity bordereau = getTenantBordereau(id, actor.getOrganizationId());
        requireStatus(bordereau, InsuranceBordereauStatus.SENT,
                "Le bordereau doit être au statut SENT pour enregistrer sa réception.");

        String reference = requireText(insurerReference, "La référence assureur est obligatoire.");
        bordereau.setInsurerReference(reference);
        bordereau.setReceivedAt(Instant.now());
        bordereau.setStatus(InsuranceBordereauStatus.RECEIVED);
        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);
        audit(actor, saved, "RECEIVE_BORDEREAU", "Réception du bordereau N° " + saved.getBordereauNumber() + ", référence " + reference + ".");
        return saved;
    }

    @Transactional
    public InsuranceBordereauEntity accept(UUID id, BigDecimal acceptedAmount, String insurerReference) {
        UserAccountEntity actor = getCurrentUser();
        InsuranceBordereauEntity bordereau = getTenantBordereau(id, actor.getOrganizationId());
        requireStatus(bordereau, InsuranceBordereauStatus.RECEIVED,
                "Le bordereau doit être au statut RECEIVED pour être accepté.");

        BigDecimal normalizedAmount = requirePositive(acceptedAmount, "Le montant accepté doit être strictement positif.");
        if (normalizedAmount.compareTo(bordereau.getTotalAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant accepté ne peut pas dépasser le montant réclamé.");
        }

        bordereau.setAcceptedAmount(normalizedAmount);
        if (insurerReference != null && !insurerReference.isBlank()) {
            bordereau.setInsurerReference(insurerReference.trim());
        }
        bordereau.setAcceptedAt(Instant.now());
        bordereau.setStatus(InsuranceBordereauStatus.ACCEPTED);
        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);
        audit(actor, saved, "ACCEPT_BORDEREAU",
                "Bordereau N° " + saved.getBordereauNumber() + " accepté pour " + normalizedAmount + " FCFA.");
        return saved;
    }

    @Transactional
    public InsuranceBordereauEntity reject(UUID id, String rejectionReason, String insurerReference) {
        UserAccountEntity actor = getCurrentUser();
        InsuranceBordereauEntity bordereau = getTenantBordereau(id, actor.getOrganizationId());
        requireStatus(bordereau, InsuranceBordereauStatus.RECEIVED,
                "Le bordereau doit être au statut RECEIVED pour être rejeté.");

        String reason = requireText(rejectionReason, "Le motif de rejet est obligatoire.");
        bordereau.setRejectionReason(reason);
        if (insurerReference != null && !insurerReference.isBlank()) {
            bordereau.setInsurerReference(insurerReference.trim());
        }
        bordereau.setRejectedAt(Instant.now());
        bordereau.setStatus(InsuranceBordereauStatus.REJECTED);
        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);
        audit(actor, saved, "REJECT_BORDEREAU", "Bordereau N° " + saved.getBordereauNumber() + " rejeté : " + reason);
        return saved;
    }

    @Transactional
    public InsuranceBordereauEntity recordPayment(UUID id, BigDecimal amount, String referenceNumber) {
        UserAccountEntity actor = getCurrentUser();
        InsuranceBordereauEntity bordereau = getTenantBordereau(id, actor.getOrganizationId());
        if (bordereau.getStatus() != InsuranceBordereauStatus.ACCEPTED
                && bordereau.getStatus() != InsuranceBordereauStatus.PARTIALLY_PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le bordereau doit être accepté avant l'enregistrement d'un règlement.");
        }

        BigDecimal normalizedAmount = requirePositive(amount, "Le montant réglé doit être strictement positif.");
        BigDecimal remaining = bordereau.getRemainingAmount();
        if (normalizedAmount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant réglé dépasse le reste à recouvrer de " + remaining + " FCFA.");
        }

        String reference = requireText(referenceNumber, "La référence du règlement est obligatoire.");
        applyPaymentToInsuranceReceivables(id, actor.getOrganizationId(), normalizedAmount);

        BigDecimal cumulativePaid = bordereau.getPaidAmount().add(normalizedAmount)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        bordereau.setPaidAmount(cumulativePaid);
        bordereau.setPaymentReference(reference);
        if (cumulativePaid.compareTo(bordereau.getTotalAmount()) >= 0) {
            bordereau.setStatus(InsuranceBordereauStatus.SETTLED);
            bordereau.setSettledAt(Instant.now());
        } else {
            bordereau.setStatus(InsuranceBordereauStatus.PARTIALLY_PAID);
        }

        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);
        audit(actor, saved, "PAY_BORDEREAU",
                "Règlement de " + normalizedAmount + " FCFA enregistré pour le bordereau N° "
                        + saved.getBordereauNumber() + ". Référence : " + reference);
        return saved;
    }

    private void applyPaymentToInsuranceReceivables(UUID bordereauId, UUID organizationId, BigDecimal amount) {
        BigDecimal remainingToAllocate = amount;
        List<InvoiceEntity> invoices = invoiceRepository
                .findByInsuranceBordereauIdAndOrganizationId(bordereauId, organizationId)
                .stream()
                .sorted(Comparator.comparing(InvoiceEntity::getCreatedAt))
                .toList();

        for (InvoiceEntity invoice : invoices) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            financialStateService.initializeReceivables(invoice);
            List<ReceivableEntity> receivables = receivableRepository.findByInvoiceIdAndDebtorTypeIgnoreCase(
                    invoice.getId(), "INSURANCE");
            for (ReceivableEntity receivable : receivables) {
                BigDecimal receivableRemaining = receivable.getTotalAmount().subtract(receivable.getPaidAmount());
                if (receivableRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal allocated = remainingToAllocate.min(receivableRemaining);
                receivable.setPaidAmount(receivable.getPaidAmount().add(allocated));
                receivableRepository.save(receivable);
                remainingToAllocate = remainingToAllocate.subtract(allocated);
            }
            financialStateService.synchronize(invoice);
        }

        if (remainingToAllocate.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le règlement ne peut pas être entièrement affecté aux créances assurance du bordereau.");
        }
    }

    private static BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private static void requireStatus(InsuranceBordereauEntity bordereau,
                                      InsuranceBordereauStatus required,
                                      String message) {
        if (bordereau.getStatus() != required) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private void audit(UserAccountEntity actor,
                       InsuranceBordereauEntity bordereau,
                       String action,
                       String description) {
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                null,
                "BILLING",
                bordereau.getId(),
                action,
                description
        );
    }
}
