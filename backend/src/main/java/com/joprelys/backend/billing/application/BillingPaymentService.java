package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.PaymentRequest;
import com.joprelys.backend.billing.api.PaymentResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentEntity;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableRepository;
import com.joprelys.backend.cash.api.CashMovementRequest;
import com.joprelys.backend.cash.application.CashRegisterService;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Coordinates an invoice payment, its cash movement, receipt and patient receivable. */
@Service
public class BillingPaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReceivableRepository receivableRepository;
    private final CashRegisterService cashRegisterService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public BillingPaymentService(InvoiceRepository invoiceRepository,
                                 PaymentRepository paymentRepository,
                                 ReceivableRepository receivableRepository,
                                 CashRegisterService cashRegisterService,
                                 UserAccountRepository userAccountRepository,
                                 AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.receivableRepository = receivableRepository;
        this.cashRegisterService = cashRegisterService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PaymentResponse addPayment(UUID invoiceId, PaymentRequest request) {
        UserAccountEntity actor = getCurrentUser();
        CashRegisterSessionEntity session = cashRegisterService.findActiveSessionForUser(actor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Paiement sans session ouverte : veuillez d'abord ouvrir une session de caisse."));
        InvoiceEntity invoice = findPayableInvoice(invoiceId);
        double totalPaid = paymentRepository.findByInvoiceId(invoiceId).stream()
                .mapToDouble(PaymentEntity::getAmount)
                .sum();
        double remaining = invoice.getPatientShare() - totalPaid;
        if (request.amount() > remaining + 0.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant dépasse le solde restant à payer par le patient (" + remaining + " FCFA).");
        }

        PaymentEntity payment = new PaymentEntity(invoice, request.amount(), request.method(), request.reference(), actor.getId());
        payment.setOrganizationId(actor.getOrganizationId());
        payment.setCashSessionId(session.getId());
        PaymentEntity saved = paymentRepository.save(payment);
        settlePatientReceivables(invoiceId, request.amount());
        cashRegisterService.addMovement(new CashMovementRequest(
                "IN", request.amount(), "Règlement facture N° " + invoice.getInvoiceNumber(),
                request.method().name(), request.reference(), false));
        cashRegisterService.createReceiptForPayment(saved);
        updateInvoicePaymentStatus(invoice, totalPaid + request.amount());
        auditService.logSuccess(actor.getId(), actor.getOrganizationId(), invoice.getPatientId(), "BILLING", saved.getId(),
                "ADD_PAYMENT", "Enregistrement règlement de " + request.amount() + " FCFA via " + request.method()
                        + " pour la facture N° " + invoice.getInvoiceNumber() + ".");
        return PaymentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream().map(PaymentResponse::fromEntity).toList();
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
    }

    private InvoiceEntity findPayableInvoice(UUID invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La facture est déjà entièrement réglée.");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'enregistrer un règlement sur une facture annulée.");
        }
        return invoice;
    }

    private void settlePatientReceivables(UUID invoiceId, double amount) {
        double remainingPayment = amount;
        for (ReceivableEntity receivable : receivableRepository.findByInvoiceIdAndDebtorTypeIgnoreCase(invoiceId, "PATIENT")) {
            if (remainingPayment <= 0) {
                break;
            }
            double outstanding = receivable.getTotalAmount() - receivable.getPaidAmount();
            double appliedAmount = Math.min(outstanding, remainingPayment);
            receivable.setPaidAmount(receivable.getPaidAmount() + appliedAmount);
            receivableRepository.save(receivable);
            remainingPayment -= appliedAmount;
        }
    }

    private void updateInvoicePaymentStatus(InvoiceEntity invoice, double totalPaid) {
        invoice.setStatus(Math.abs(totalPaid - invoice.getPatientShare()) < 0.01
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);
    }
}
