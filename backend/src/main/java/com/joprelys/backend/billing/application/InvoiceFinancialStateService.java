package com.joprelys.backend.billing.application;

import com.joprelys.backend.billing.api.InvoiceCollectionStatus;
import com.joprelys.backend.billing.api.InvoiceSettlementSummaryResponse;
import com.joprelys.backend.billing.api.SettlementPartyResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentEntity;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Centralise l'initialisation des créances et l'interprétation financière d'une facture. */
@Service
public class InvoiceFinancialStateService {

    private static final BigDecimal SETTLED_TOLERANCE = new BigDecimal("0.01");
    private static final String PATIENT = "PATIENT";
    private static final String INSURANCE = "INSURANCE";

    private final InvoiceRepository invoiceRepository;
    private final ReceivableRepository receivableRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceFinancialStateService(InvoiceRepository invoiceRepository,
                                        ReceivableRepository receivableRepository,
                                        PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.receivableRepository = receivableRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Crée uniquement les créances manquantes. Les paiements historiques sont utilisés
     * pour reconstruire prudemment une créance patient absente.
     */
    @Transactional
    public void initializeReceivables(InvoiceEntity invoice) {
        List<ReceivableEntity> existing = receivableRepository.findByInvoiceId(invoice.getId());
        Set<String> debtorTypes = existing.stream()
                .map(ReceivableEntity::getDebtorType)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        BigDecimal patientShare = amountOrZero(invoice.getPatientShare());
        if (patientShare.signum() > 0 && !debtorTypes.contains(PATIENT)) {
            ReceivableEntity patientReceivable = new ReceivableEntity(
                    invoice.getId(), PATIENT, invoice.getPatientId(), patientShare);
            patientReceivable.setOrganizationId(invoice.getOrganizationId());
            patientReceivable.setPaidAmount(inferHistoricalPatientPaid(invoice, patientShare));
            receivableRepository.save(patientReceivable);
        }

        BigDecimal insuranceShare = amountOrZero(invoice.getInsuranceShare());
        if (insuranceShare.signum() > 0
                && invoice.getInsuranceConvention() != null
                && !debtorTypes.contains(INSURANCE)) {
            ReceivableEntity insuranceReceivable = new ReceivableEntity(
                    invoice.getId(), INSURANCE, invoice.getInsuranceConvention().getId(), insuranceShare);
            insuranceReceivable.setOrganizationId(invoice.getOrganizationId());
            if (invoice.getStatus() == InvoiceStatus.SETTLED) {
                insuranceReceivable.setPaidAmount(insuranceShare);
            }
            receivableRepository.save(insuranceReceivable);
        }
    }

    @Transactional
    public InvoiceStatus synchronize(InvoiceEntity invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELLED
                || invoice.getStatus() == InvoiceStatus.PENDING
                || invoice.getStatus() == InvoiceStatus.PROFORMA) {
            return invoice.getStatus();
        }

        InvoiceCollectionStatus collectionStatus = collectionStatus(
                invoice, receivableRepository.findByInvoiceId(invoice.getId()));
        InvoiceStatus targetStatus = switch (collectionStatus) {
            case PATIENT_DUE -> InvoiceStatus.VALIDATED;
            case PATIENT_PARTIALLY_PAID -> InvoiceStatus.PARTIALLY_PAID;
            case INSURANCE_DUE -> InvoiceStatus.PAID;
            case SETTLED -> InvoiceStatus.SETTLED;
            case CANCELLED -> InvoiceStatus.CANCELLED;
            case NOT_YET_DUE -> invoice.getStatus();
        };

        if (invoice.getStatus() != targetStatus) {
            invoice.setStatus(targetStatus);
            invoiceRepository.save(invoice);
        }
        return targetStatus;
    }

    @Transactional(readOnly = true)
    public InvoiceSettlementSummaryResponse summarize(InvoiceEntity invoice) {
        List<ReceivableEntity> receivables = receivableRepository.findByInvoiceId(invoice.getId());
        SettlementPartyResponse patient = partySummary(invoice, receivables, PATIENT, invoice.getPatientShare());
        SettlementPartyResponse insurance = amountOrZero(invoice.getInsuranceShare()).compareTo(SETTLED_TOLERANCE) > 0
                ? partySummary(invoice, receivables, INSURANCE, invoice.getInsuranceShare())
                : null;
        return new InvoiceSettlementSummaryResponse(
                invoice.getId(), collectionStatus(invoice, patient, insurance), patient, insurance);
    }

    private InvoiceCollectionStatus collectionStatus(InvoiceEntity invoice,
                                                     List<ReceivableEntity> receivables) {
        SettlementPartyResponse patient = partySummary(invoice, receivables, PATIENT, invoice.getPatientShare());
        SettlementPartyResponse insurance = amountOrZero(invoice.getInsuranceShare()).compareTo(SETTLED_TOLERANCE) > 0
                ? partySummary(invoice, receivables, INSURANCE, invoice.getInsuranceShare())
                : null;
        return collectionStatus(invoice, patient, insurance);
    }

    private InvoiceCollectionStatus collectionStatus(InvoiceEntity invoice,
                                                     SettlementPartyResponse patient,
                                                     SettlementPartyResponse insurance) {
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return InvoiceCollectionStatus.CANCELLED;
        }
        if (invoice.getStatus() == InvoiceStatus.PENDING || invoice.getStatus() == InvoiceStatus.PROFORMA) {
            return InvoiceCollectionStatus.NOT_YET_DUE;
        }
        if (patient.remainingAmount().compareTo(SETTLED_TOLERANCE) > 0) {
            return patient.paidAmount().compareTo(SETTLED_TOLERANCE) > 0
                    ? InvoiceCollectionStatus.PATIENT_PARTIALLY_PAID
                    : InvoiceCollectionStatus.PATIENT_DUE;
        }
        if (insurance != null && insurance.remainingAmount().compareTo(SETTLED_TOLERANCE) > 0) {
            return InvoiceCollectionStatus.INSURANCE_DUE;
        }
        return InvoiceCollectionStatus.SETTLED;
    }

    private SettlementPartyResponse partySummary(InvoiceEntity invoice,
                                                  List<ReceivableEntity> receivables,
                                                  String debtorType,
                                                  BigDecimal invoiceShare) {
        BigDecimal share = amountOrZero(invoiceShare);
        return receivables.stream()
                .filter(receivable -> debtorType.equalsIgnoreCase(receivable.getDebtorType()))
                .findFirst()
                .map(receivable -> new SettlementPartyResponse(
                        receivable.getTotalAmount(),
                        receivable.getPaidAmount(),
                        receivable.getTotalAmount().subtract(receivable.getPaidAmount()).max(BigDecimal.ZERO),
                        receivable.getStatus()))
                .orElseGet(() -> {
                    BigDecimal inferredPaid = inferredPaidAmount(invoice, debtorType, share);
                    return new SettlementPartyResponse(
                            share,
                            inferredPaid,
                            share.subtract(inferredPaid).max(BigDecimal.ZERO),
                            fallbackPartyStatus(invoice, share, inferredPaid));
                });
    }

    private BigDecimal inferredPaidAmount(InvoiceEntity invoice, String debtorType, BigDecimal share) {
        if (share.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (PATIENT.equals(debtorType)) {
            return inferHistoricalPatientPaid(invoice, share);
        }
        return invoice.getStatus() == InvoiceStatus.SETTLED ? share : BigDecimal.ZERO;
    }

    private BigDecimal inferHistoricalPatientPaid(InvoiceEntity invoice, BigDecimal patientShare) {
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.SETTLED) {
            return patientShare;
        }
        BigDecimal payments = paymentRepository.findByInvoiceId(invoice.getId()).stream()
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return payments.min(patientShare).max(BigDecimal.ZERO);
    }

    private String fallbackPartyStatus(InvoiceEntity invoice, BigDecimal share, BigDecimal paid) {
        if (share.signum() <= 0 || invoice.getStatus() == InvoiceStatus.PENDING || invoice.getStatus() == InvoiceStatus.PROFORMA) {
            return "NOT_DUE";
        }
        if (paid.compareTo(share) >= 0) {
            return "PAID";
        }
        return paid.signum() > 0 ? "PARTIALLY_PAID" : "UNPAID";
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
