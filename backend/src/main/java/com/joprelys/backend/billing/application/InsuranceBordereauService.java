package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class InsuranceBordereauService {

    private final InsuranceBordereauRepository bordereauRepository;
    private final InvoiceRepository invoiceRepository;
    private final InsuranceConventionRepository conventionRepository;
    private final ReceivableRepository receivableRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public InsuranceBordereauService(InsuranceBordereauRepository bordereauRepository,
                                     InvoiceRepository invoiceRepository,
                                     InsuranceConventionRepository conventionRepository,
                                     ReceivableRepository receivableRepository,
                                     UserAccountRepository userAccountRepository,
                                     AuditService auditService) {
        this.bordereauRepository = bordereauRepository;
        this.invoiceRepository = invoiceRepository;
        this.conventionRepository = conventionRepository;
        this.receivableRepository = receivableRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public InsuranceBordereauEntity generateBordereau(UUID conventionId, LocalDate startDate, LocalDate endDate) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;

        InsuranceConventionEntity convention = conventionRepository.findById(conventionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention d'assurance introuvable"));

        // Convert LocalDate parameters to Instant for invoice validatedAt field queries
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        // Include the entire end date until 23:59:59.999
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);

        // Find eligible invoices for this convention that are not yet billed in a statement (bordereau)
        List<InvoiceEntity> invoices = invoiceRepository.findInvoicesForBordereau(
                conventionId,
                List.of(InvoiceStatus.VALIDATED, InvoiceStatus.PENDING, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.PAID),
                startInstant,
                endInstant,
                orgId
        );

        if (invoices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune facture éligible (VALIDATED, PENDING, PARTIALLY_PAID) avec part assurance n'est disponible pour cette convention sur la période sélectionnée.");
        }

        // Generate robust sequence-based bordereau number (format: BORD-yyyyMMdd-XXXXXX)
        Long seqVal = bordereauRepository.getNextBordereauNumberSequenceValue();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String bordereauNumber = String.format("BORD-%s-%06d", dateStr, seqVal);

        // Calculate insurance total share sum
        double totalAmount = invoices.stream()
                .mapToDouble(InvoiceEntity::getInsuranceShare)
                .sum();

        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity(
                bordereauNumber,
                convention,
                startDate,
                endDate,
                totalAmount
        );
        bordereau.setOrganizationId(orgId);
        InsuranceBordereauEntity savedBordereau = bordereauRepository.save(bordereau);

        // Link invoices to the new bordereau statement
        for (InvoiceEntity invoice : invoices) {
            invoice.setInsuranceBordereauId(savedBordereau.getId());
            invoiceRepository.save(invoice);
        }

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    orgId,
                    null,
                    "BILLING",
                    savedBordereau.getId(),
                    "GENERATE_BORDEREAU",
                    "Génération du bordereau N° " + savedBordereau.getBordereauNumber() +
                            " pour la convention " + convention.getName() + " d'un montant total de " + totalAmount + " FCFA."
            );
        }

        return savedBordereau;
    }

    @Transactional(readOnly = true)
    public List<InsuranceBordereauEntity> listBordereaux() {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;
        return bordereauRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    @Transactional(readOnly = true)
    public InsuranceBordereauEntity getBordereau(UUID id) {
        return bordereauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bordereau introuvable"));
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> getBordereauInvoices(UUID bordereauId) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;
        return invoiceRepository.findByInsuranceBordereauIdAndOrganizationId(bordereauId, orgId);
    }

    @Transactional
    public InsuranceBordereauEntity markAsSent(UUID id) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;

        InsuranceBordereauEntity bordereau = bordereauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bordereau introuvable"));

        if (bordereau.getStatus() != InsuranceBordereauStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le bordereau doit être au statut DRAFT pour être marqué comme envoyé.");
        }

        bordereau.setStatus(InsuranceBordereauStatus.SENT);
        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    orgId,
                    null,
                    "BILLING",
                    saved.getId(),
                    "SEND_BORDEREAU",
                    "Bordereau N° " + saved.getBordereauNumber() + " marqué comme envoyé physiquement à l'assurance."
            );
        }

        return saved;
    }

    @Transactional
    public InsuranceBordereauEntity recordPayment(UUID id, Double amount, String referenceNumber) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;

        InsuranceBordereauEntity bordereau = bordereauRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bordereau introuvable"));

        if (bordereau.getStatus() != InsuranceBordereauStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le bordereau doit être au statut SENT pour enregistrer un règlement.");
        }

        if (Math.abs(bordereau.getTotalAmount() - amount) > 0.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant réglé (" + amount + " FCFA) ne correspond pas au montant du bordereau (" + bordereau.getTotalAmount() + " FCFA).");
        }

        bordereau.setStatus(InsuranceBordereauStatus.PAID);
        InsuranceBordereauEntity saved = bordereauRepository.save(bordereau);

        // Fetch all invoices associated with this bordereau statement
        List<InvoiceEntity> invoices = getBordereauInvoices(id);
        for (InvoiceEntity invoice : invoices) {
            // Settle corresponding insurance receivables for the invoices
            List<ReceivableEntity> receivables = receivableRepository.findByInvoiceIdAndDebtorTypeIgnoreCase(
                    invoice.getId(), "INSURANCE");
            for (ReceivableEntity receivable : receivables) {
                receivable.setPaidAmount(receivable.getTotalAmount());
                receivableRepository.save(receivable);
            }
        }

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    orgId,
                    null,
                    "BILLING",
                    saved.getId(),
                    "PAY_BORDEREAU",
                    "Règlement enregistré pour le bordereau N° " + saved.getBordereauNumber() +
                            " d'un montant de " + amount + " FCFA. Référence: " + referenceNumber
            );
        }

        return saved;
    }
}
