package com.joprelys.backend.accounting.application;

import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.cash.infrastructure.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountingExportService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final CashRegisterSessionRepository cashRegisterSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final InsuranceBordereauRepository insuranceBordereauRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyy").withZone(ZoneId.of("UTC"));

    public AccountingExportService(InvoiceRepository invoiceRepository,
                                   PaymentRepository paymentRepository,
                                   CashRegisterSessionRepository cashRegisterSessionRepository,
                                   CashMovementRepository cashMovementRepository,
                                   InsuranceBordereauRepository insuranceBordereauRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.cashRegisterSessionRepository = cashRegisterSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.insuranceBordereauRepository = insuranceBordereauRepository;
    }

    @Transactional(readOnly = true)
    public String generateSage100Export(Instant start, Instant end) {
        List<AccountingLine> lines = new ArrayList<>();

        // 1. Journal des Ventes (Factures VALIDATED, PAID, PARTIALLY_PAID)
        List<InvoiceStatus> salesStatuses = List.of(InvoiceStatus.VALIDATED, InvoiceStatus.PAID, InvoiceStatus.PARTIALLY_PAID);
        for (InvoiceStatus status : salesStatuses) {
            List<InvoiceEntity> invoices = invoiceRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(status, start, end);
            for (InvoiceEntity inv : invoices) {
                String dateStr = DATE_FORMATTER.format(inv.getCreatedAt());
                String refPiece = inv.getInvoiceNumber();
                String libelle = truncate("Facture " + refPiece, 30);

                // Part Patient
                if (inv.getPatientShare() != null && inv.getPatientShare().signum() > 0) {
                    lines.add(new AccountingLine("VT", dateStr, "41110000", inv.getPatientId().toString(), refPiece, libelle, inv.getPatientShare().doubleValue(), 0.0));
                    lines.add(new AccountingLine("VT", dateStr, "70610000", null, refPiece, libelle, 0.0, inv.getPatientShare().doubleValue()));
                }

                // Part Assurance
                if (inv.getInsuranceShare() != null && inv.getInsuranceShare().signum() > 0 && inv.getInsuranceConvention() != null) {
                    String assuranceId = inv.getInsuranceConvention().getId().toString();
                    lines.add(new AccountingLine("VT", dateStr, "41120000", assuranceId, refPiece, libelle, inv.getInsuranceShare().doubleValue(), 0.0));
                    lines.add(new AccountingLine("VT", dateStr, "70610000", null, refPiece, libelle, 0.0, inv.getInsuranceShare().doubleValue()));
                }
            }
        }

        // 2. Journal de Caisse (Paiements perçus)
        List<PaymentEntity> payments = paymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        for (PaymentEntity p : payments) {
            String dateStr = DATE_FORMATTER.format(p.getCreatedAt());
            String refPiece = p.getId().toString().substring(0, 8).toUpperCase();
            String libelle = truncate("Reglement Caisse " + refPiece, 30);

            if (p.getPaymentMethod() == PaymentMethod.CASH) {
                // Débit Caisse / Crédit Client Patient
                lines.add(new AccountingLine("CA", dateStr, "57110000", null, refPiece, libelle, p.getAmount().doubleValue(), 0.0));
                lines.add(new AccountingLine("CA", dateStr, "41110000", null, refPiece, libelle, 0.0, p.getAmount().doubleValue()));
            }
        }

        // 3. Journal de Caisse (Écarts de Clôture)
        List<CashRegisterSessionEntity> sessions = cashRegisterSessionRepository.findAllByOrderByOpenedAtDesc();
        for (CashRegisterSessionEntity sess : sessions) {
            if (sess.getClosedAt() != null && sess.getClosedAt().isAfter(start) && sess.getClosedAt().isBefore(end)) {
                Double diff = sess.getDiscrepancyAmount();
                if (diff != null && Math.abs(diff) > 0.01) {
                    String dateStr = DATE_FORMATTER.format(sess.getClosedAt());
                    String refPiece = sess.getId().toString().substring(0, 8).toUpperCase();
                    String libelle = truncate("Ecart caisse " + refPiece, 30);

                    if (diff < 0) {
                        // Déficit : Charge exceptionnelle 65600000 (Débit) / Caisse 57110000 (Crédit)
                        lines.add(new AccountingLine("CA", dateStr, "65600000", null, refPiece, libelle, Math.abs(diff), 0.0));
                        lines.add(new AccountingLine("CA", dateStr, "57110000", null, refPiece, libelle, 0.0, Math.abs(diff)));
                    } else {
                        // Excédent : Caisse 57110000 (Débit) / Produit exceptionnel 75600000 (Crédit)
                        lines.add(new AccountingLine("CA", dateStr, "57110000", null, refPiece, libelle, diff, 0.0));
                        lines.add(new AccountingLine("CA", dateStr, "75600000", null, refPiece, libelle, 0.0, diff));
                    }
                }
            }
        }

        // 4. Journal de Banque (Bordereaux Assurance payés & Virements)
        List<InsuranceBordereauEntity> bordereaux = insuranceBordereauRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        for (InsuranceBordereauEntity b : bordereaux) {
            if ("PAID".equals(b.getStatus()) && b.getUpdatedAt() != null) {
                String dateStr = DATE_FORMATTER.format(b.getUpdatedAt());
                String refPiece = b.getBordereauNumber();
                String libelle = truncate("Reglement Assurance " + refPiece, 30);
                Double paidAmount = b.getTotalAmount().doubleValue(); // On considère le total réglé pour cet export

                // Débit Banque 52110000 / Crédit Assurance 41120000
                lines.add(new AccountingLine("BQ", dateStr, "52110000", null, refPiece, libelle, paidAmount, 0.0));
                lines.add(new AccountingLine("BQ", dateStr, "41120000", b.getInsuranceConvention().getId().toString(), refPiece, libelle, 0.0, paidAmount));
            }
        }

        // Mouvements de caisse de type TRANSFER_TO_BANK (Virements internes)
        List<CashMovementEntity> movements = cashMovementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        for (CashMovementEntity m : movements) {
            if ("TRANSFER_TO_BANK".equals(m.getMovementType())) {
                String dateStr = DATE_FORMATTER.format(m.getCreatedAt());
                String refPiece = m.getId().toString().substring(0, 8).toUpperCase();
                String libelle = truncate("Versement Banque " + refPiece, 30);

                // Débit Virement interne 58500000 / Crédit Caisse 57110000 (dans le journal de caisse)
                lines.add(new AccountingLine("CA", dateStr, "58500000", null, refPiece, libelle, m.getAmount(), 0.0));
                lines.add(new AccountingLine("CA", dateStr, "57110000", null, refPiece, libelle, 0.0, m.getAmount()));

                // Débit Banque 52110000 / Crédit Virement interne 58500000 (dans le journal de banque)
                lines.add(new AccountingLine("BQ", dateStr, "52110000", null, refPiece, libelle, m.getAmount(), 0.0));
                lines.add(new AccountingLine("BQ", dateStr, "58500000", null, refPiece, libelle, 0.0, m.getAmount()));
            }
        }

        // 5. Générer le fichier CSV
        StringWriter sw = new StringWriter();
        sw.append("Journal;Date;CompteGeneral;CompteTiers;RefPiece;Libelle;Debit;Credit\n");
        for (AccountingLine line : lines) {
            sw.append(line.journal()).append(";")
              .append(line.date()).append(";")
              .append(line.compteGeneral()).append(";")
              .append(line.compteTiers() != null ? line.compteTiers() : "").append(";")
              .append(line.refPiece()).append(";")
              .append(line.libelle()).append(";")
              .append(String.format("%.0f", line.debit())).append(";")
              .append(String.format("%.0f", line.credit())).append("\n");
        }

        return sw.toString();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.substring(0, Math.min(text.length(), max));
    }

    private record AccountingLine(
        String journal,
        String date,
        String compteGeneral,
        String compteTiers,
        String refPiece,
        String libelle,
        Double debit,
        Double credit
    ) {}
}
