package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.CreateCreditNoteRequest;
import com.joprelys.backend.billing.api.CreditNoteResponse;
import com.joprelys.backend.billing.api.RefundCreditNoteRequest;
import com.joprelys.backend.billing.infrastructure.persistence.CreditNoteEntity;
import com.joprelys.backend.billing.infrastructure.persistence.CreditNoteRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentEntity;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentMethod;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableRepository;
import com.joprelys.backend.cash.api.CashMovementRequest;
import com.joprelys.backend.cash.application.CashRegisterService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Avoirs (ADR-20261017 D-17) : émis par la facturation, approuvés par la DAF au-delà du seuil de l'établissement
 * (jamais par leur émetteur), remboursés en caisse dans la limite des sommes réellement encaissées.
 */
@Service
public class CreditNoteService {

    private static final String CREDIT_NOTE_PREFIX = "AV-";
    private static final Set<PaymentMethod> REFUND_METHODS = Set.of(PaymentMethod.CASH, PaymentMethod.CHECK, PaymentMethod.BANK_TRANSFER);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReceivableRepository receivableRepository;
    private final PaymentRepository paymentRepository;
    private final OrganizationRepository organizationRepository;
    private final InvoiceFinancialStateService financialStateService;
    private final CashRegisterService cashRegisterService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public CreditNoteService(CreditNoteRepository creditNoteRepository, InvoiceRepository invoiceRepository,
                             ReceivableRepository receivableRepository, PaymentRepository paymentRepository,
                             OrganizationRepository organizationRepository, InvoiceFinancialStateService financialStateService,
                             CashRegisterService cashRegisterService, UserAccountRepository userAccountRepository,
                             AuditService auditService) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.receivableRepository = receivableRepository;
        this.paymentRepository = paymentRepository;
        this.organizationRepository = organizationRepository;
        this.financialStateService = financialStateService;
        this.cashRegisterService = cashRegisterService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CreditNoteResponse issue(UUID invoiceId, CreateCreditNoteRequest request) {
        UserAccountEntity actor = currentUser();
        InvoiceEntity invoice = invoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.PENDING
                || invoice.getStatus() == InvoiceStatus.PROFORMA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un avoir ne s'émet que sur une facture validée ; une facture en attente s'annule ou se recalcule.");
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de l'avoir est obligatoire.");
        }
        BigDecimal amount = BigDecimal.valueOf(request.amount());
        if (amount.compareTo(TOLERANCE) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le montant de l'avoir doit être positif.");
        }
        BigDecimal alreadyCredited = creditNoteRepository.findByInvoiceId(invoiceId).stream()
                .filter(note -> !CreditNoteEntity.CANCELLED.equals(note.getStatus()))
                .map(note -> BigDecimal.valueOf(note.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.add(alreadyCredited).compareTo(invoice.getTotalAmount().add(TOLERANCE)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les avoirs dépasseraient le montant de la facture (" + invoice.getTotalAmount() + " FCFA, déjà "
                            + alreadyCredited + " FCFA).");
        }
        BigDecimal threshold = organizationRepository.findById(invoice.getOrganizationId())
                .map(organization -> organization.getCreditNoteApprovalThreshold())
                .orElse(new BigDecimal("50000"));
        boolean requiresApproval = amount.compareTo(threshold) > 0;

        String number = String.format("%s%s-%06d", CREDIT_NOTE_PREFIX,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                creditNoteRepository.getNextCreditNoteNumberSequenceValue());
        CreditNoteEntity creditNote = new CreditNoteEntity(invoiceId, number, request.amount(), reason);
        creditNote.markIssued(actor.getId(), requiresApproval);
        creditNoteRepository.save(creditNote);
        if (!requiresApproval) {
            applyToInvoice(invoice, amount);
        }
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), invoice.getPatientId(), "BILLING", creditNote.getId(),
                requiresApproval ? "ISSUE_CREDIT_NOTE_PENDING_APPROVAL" : "ISSUE_CREDIT_NOTE",
                "Avoir " + number + " de " + amount + " FCFA sur la facture " + invoice.getInvoiceNumber() + " : " + reason);
        return CreditNoteResponse.fromEntity(creditNote);
    }

    @Transactional
    public CreditNoteResponse approve(UUID creditNoteId) {
        UserAccountEntity actor = currentUser();
        CreditNoteEntity creditNote = pending(creditNoteId);
        if (actor.getId().equals(creditNote.getIssuedBy())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un avoir ne peut pas être approuvé par son émetteur.");
        }
        creditNote.approve(actor.getId(), Instant.now());
        creditNoteRepository.save(creditNote);
        InvoiceEntity invoice = invoice(creditNote.getInvoiceId());
        applyToInvoice(invoice, BigDecimal.valueOf(creditNote.getAmount()));
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), invoice.getPatientId(), "BILLING", creditNote.getId(),
                "APPROVE_CREDIT_NOTE", "Avoir " + creditNote.getCreditNoteNumber() + " approuvé.");
        return CreditNoteResponse.fromEntity(creditNote);
    }

    @Transactional
    public CreditNoteResponse reject(UUID creditNoteId) {
        UserAccountEntity actor = currentUser();
        CreditNoteEntity creditNote = pending(creditNoteId);
        creditNote.reject(actor.getId(), Instant.now());
        creditNoteRepository.save(creditNote);
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), null, "BILLING", creditNote.getId(),
                "REJECT_CREDIT_NOTE", "Avoir " + creditNote.getCreditNoteNumber() + " refusé.");
        return CreditNoteResponse.fromEntity(creditNote);
    }

    /** Remboursement en caisse d'un avoir actif, dans la session ouverte du caissier. */
    @Transactional
    public CreditNoteResponse refund(UUID creditNoteId, RefundCreditNoteRequest request) {
        UserAccountEntity actor = currentUser();
        CreditNoteEntity creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avoir introuvable"));
        if (!CreditNoteEntity.ACTIVE.equals(creditNote.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seul un avoir actif peut être remboursé.");
        }
        if (request.method() == null || !REFUND_METHODS.contains(request.method())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode de remboursement non pris en charge.");
        }
        BigDecimal amount = request.amount();
        BigDecimal refundableOnNote = BigDecimal.valueOf(creditNote.getAmount() - creditNote.getRefundedAmount());
        InvoiceEntity invoice = invoice(creditNote.getInvoiceId());
        BigDecimal collected = paymentRepository.findByInvoiceId(invoice.getId()).stream()
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alreadyRefunded = creditNoteRepository.findByInvoiceId(invoice.getId()).stream()
                .map(note -> BigDecimal.valueOf(note.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundable = refundableOnNote.min(collected.subtract(alreadyRefunded)).max(BigDecimal.ZERO);
        if (amount.compareTo(refundable.add(TOLERANCE)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Remboursement limité à " + refundable + " FCFA (avoir restant et sommes encaissées).");
        }
        cashRegisterService.findActiveSessionForUser(actor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Remboursement sans session ouverte : veuillez d'abord ouvrir une session de caisse."));
        // L'avoir a déjà été validé (et approuvé par la DAF au-delà du seuil) : il vaut double visa.
        cashRegisterService.addMovement(new CashMovementRequest("OUT", amount.doubleValue(),
                "Remboursement avoir N° " + creditNote.getCreditNoteNumber() + " (facture " + invoice.getInvoiceNumber() + ")",
                request.method().name(), request.reference(), true));
        creditNote.recordRefund(amount.doubleValue());
        creditNoteRepository.save(creditNote);
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), invoice.getPatientId(), "BILLING", creditNote.getId(),
                "REFUND_CREDIT_NOTE", "Remboursement de " + amount + " FCFA (" + request.method() + ") sur l'avoir "
                        + creditNote.getCreditNoteNumber() + ".");
        return CreditNoteResponse.fromEntity(creditNote);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteResponse> listByInvoice(UUID invoiceId) {
        return creditNoteRepository.findByInvoiceId(invoiceId).stream().map(CreditNoteResponse::fromEntity).toList();
    }

    private void applyToInvoice(InvoiceEntity invoice, BigDecimal creditAmount) {
        BigDecimal remaining = creditAmount;
        for (ReceivableEntity receivable : receivableRepository.findByInvoiceId(invoice.getId())) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal reducible = receivable.getTotalAmount().subtract(receivable.getPaidAmount()).max(BigDecimal.ZERO);
            BigDecimal reduction = remaining.min(reducible);
            receivable.setPaidAmount(receivable.getPaidAmount().add(reduction));
            remaining = remaining.subtract(reduction);
            receivableRepository.save(receivable);
        }
        financialStateService.synchronize(invoice);
    }

    private CreditNoteEntity pending(UUID creditNoteId) {
        CreditNoteEntity creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avoir introuvable"));
        if (!CreditNoteEntity.PENDING_APPROVAL.equals(creditNote.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet avoir n'est pas en attente d'approbation.");
        }
        return creditNote;
    }

    private InvoiceEntity invoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
    }

    private UserAccountEntity currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
    }
}
