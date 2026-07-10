package com.joprelys.backend.cash.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.cash.api.CashMovementResponse;
import com.joprelys.backend.cash.api.CashSessionHistoryResponse;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CashSessionCloseoutReportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.FRANCE);

    private final CashRegisterService cashRegisterService;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;

    public CashSessionCloseoutReportService(CashRegisterService cashRegisterService,
                                            UserAccountRepository userAccountRepository,
                                            OrganizationRepository organizationRepository) {
        this.cashRegisterService = cashRegisterService;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        MONEY_FORMAT.setMaximumFractionDigits(0);
        MONEY_FORMAT.setMinimumFractionDigits(0);
    }

    @Transactional(readOnly = true)
    public byte[] generate(UUID sessionId) {
        CashRegisterSessionEntity session = cashRegisterService.getClosedSessionForReport(sessionId);
        CashSessionHistoryResponse history = cashRegisterService.getSessionHistory(sessionId);
        List<CashMovementResponse> movements = cashRegisterService.getSessionMovements(sessionId);
        OrganizationEntity organization = organizationRepository.findById(session.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable."));

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.BLACK);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

            addHeader(document, organization, history, titleFont, mutedFont);
            addIdentitySection(document, history, sectionFont, bodyFont);
            addSummarySection(document, history, sectionFont, bodyFont);
            addMovementSection(document, movements, sectionFont, bodyFont, tableHeaderFont);
            addSignatures(document, history, sectionFont, bodyFont);
            addFooter(document, history, mutedFont);
        } catch (DocumentException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de générer le bordereau de clôture.", exception);
        } finally {
            document.close();
        }

        return output.toByteArray();
    }

    private void addHeader(Document document,
                           OrganizationEntity organization,
                           CashSessionHistoryResponse history,
                           Font titleFont,
                           Font mutedFont) throws DocumentException {
        Paragraph clinic = new Paragraph(organization.getName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK));
        clinic.setAlignment(Element.ALIGN_CENTER);
        document.add(clinic);

        String address = safe(organization.getAddress()) + (organization.getCity() == null ? "" : " — " + organization.getCity());
        Paragraph contact = new Paragraph(address + " | Tél. " + safe(organization.getPhone()), mutedFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        document.add(contact);
        document.add(new Paragraph(" "));

        Paragraph title = new Paragraph("BORDEREAU DE CLÔTURE DE CAISSE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph number = new Paragraph("N° " + history.reportNumber(), mutedFont);
        number.setAlignment(Element.ALIGN_CENTER);
        document.add(number);
        document.add(new Paragraph(" "));
    }

    private void addIdentitySection(Document document,
                                    CashSessionHistoryResponse history,
                                    Font sectionFont,
                                    Font bodyFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{50f, 50f});
        table.setSpacingAfter(12f);

        addInfoCell(table, "Caisse", history.cashRegisterName(), sectionFont, bodyFont);
        addInfoCell(table, "Caissier", history.openedByName(), sectionFont, bodyFont);
        addInfoCell(table, "Ouverture", formatDate(history.openedAt()), sectionFont, bodyFont);
        addInfoCell(table, "Clôture", formatDate(history.closedAt()), sectionFont, bodyFont);
        document.add(table);
    }

    private void addSummarySection(Document document,
                                   CashSessionHistoryResponse history,
                                   Font sectionFont,
                                   Font bodyFont) throws DocumentException {
        document.add(new Paragraph("RÉCAPITULATIF FINANCIER", sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{68f, 32f});
        table.setSpacingBefore(6f);
        table.setSpacingAfter(14f);

        addAmountRow(table, "Fond de caisse initial", history.openingBalance(), bodyFont, false);
        addAmountRow(table, "Encaissements espèces", history.cashReceipts(), bodyFont, false);
        addAmountRow(table, "Encaissements chèques", history.chequeReceipts(), bodyFont, false);
        addAmountRow(table, "Encaissements virements", history.transferReceipts(), bodyFont, false);
        addAmountRow(table, "Dépenses espèces", history.cashExpenses(), bodyFont, false);
        addAmountRow(table, "Versements banque", history.bankDeposits(), bodyFont, false);
        addAmountRow(table, "Solde théorique espèces", history.expectedCash(), bodyFont, true);
        addAmountRow(table, "Montant physique déclaré", history.declaredBalance(), bodyFont, true);
        addAmountRow(table, "Écart de caisse", history.discrepancyAmount(), bodyFont, true);

        if (history.discrepancyReason() != null && !history.discrepancyReason().isBlank()) {
            PdfPCell label = cell("Justification de l'écart", bodyFont, false);
            PdfPCell value = cell(history.discrepancyReason(), bodyFont, false);
            table.addCell(label);
            table.addCell(value);
        }
        document.add(table);
    }

    private void addMovementSection(Document document,
                                    List<CashMovementResponse> movements,
                                    Font sectionFont,
                                    Font bodyFont,
                                    Font tableHeaderFont) throws DocumentException {
        document.add(new Paragraph("MOUVEMENTS DE LA SESSION", sectionFont));
        document.add(new Paragraph(" "));

        if (movements.isEmpty()) {
            document.add(new Paragraph("Aucun mouvement enregistré pendant cette session.", bodyFont));
            return;
        }

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{14f, 11f, 27f, 15f, 14f, 19f});
        addHeaderCell(table, "Date", tableHeaderFont);
        addHeaderCell(table, "Type", tableHeaderFont);
        addHeaderCell(table, "Description", tableHeaderFont);
        addHeaderCell(table, "Montant", tableHeaderFont);
        addHeaderCell(table, "Mode", tableHeaderFont);
        addHeaderCell(table, "Référence", tableHeaderFont);

        for (CashMovementResponse movement : movements) {
            table.addCell(cell(formatDate(movement.createdAt()), bodyFont, false));
            table.addCell(cell(movement.movementType(), bodyFont, false));
            table.addCell(cell(movement.description(), bodyFont, false));
            PdfPCell amountCell = cell(formatMoney(movement.amount()), bodyFont, true);
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);
            table.addCell(cell(movement.paymentMethod(), bodyFont, false));
            table.addCell(cell(safe(movement.referenceNumber()), bodyFont, false));
        }
        document.add(table);
    }

    private void addSignatures(Document document,
                               CashSessionHistoryResponse history,
                               Font sectionFont,
                               Font bodyFont) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{50f, 50f});
        table.setSpacingBefore(24f);

        PdfPCell cashierCell = signatureCell("Signature du caissier", history.openedByName(), sectionFont, bodyFont);
        PdfPCell managerCell = signatureCell("Visa du responsable", "Nom, date et signature", sectionFont, bodyFont);
        table.addCell(cashierCell);
        table.addCell(managerCell);
        document.add(table);
    }

    private void addFooter(Document document,
                           CashSessionHistoryResponse history,
                           Font mutedFont) throws DocumentException {
        document.add(new Paragraph(" "));
        Paragraph footer = new Paragraph(
                "Document généré par Joprelys HealthTech — Parce que chaque vie compte. Référence : "
                        + history.reportNumber(),
                mutedFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addInfoCell(PdfPTable table,
                             String label,
                             String value,
                             Font sectionFont,
                             Font bodyFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8f);
        cell.setBorderColor(new Color(220, 226, 232));
        cell.addElement(new Paragraph(label, sectionFont));
        cell.addElement(new Paragraph(safe(value), bodyFont));
        table.addCell(cell);
    }

    private void addAmountRow(PdfPTable table,
                              String label,
                              Double amount,
                              Font bodyFont,
                              boolean emphasized) {
        Font labelFont = emphasized
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK)
                : bodyFont;
        PdfPCell labelCell = cell(label, labelFont, emphasized);
        PdfPCell amountCell = cell(formatMoney(amount), labelFont, emphasized);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(amountCell);
    }

    private PdfPCell cell(String value, Font font, boolean emphasized) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(value), font));
        cell.setPadding(6f);
        cell.setBorderColor(new Color(225, 230, 235));
        if (emphasized) {
            cell.setBackgroundColor(new Color(244, 247, 249));
        }
        return cell;
    }

    private void addHeaderCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(6f);
        cell.setBackgroundColor(new Color(29, 78, 91));
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private PdfPCell signatureCell(String title,
                                   String subtitle,
                                   Font sectionFont,
                                   Font bodyFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setMinimumHeight(85f);
        cell.setBorderColor(new Color(210, 216, 222));
        cell.addElement(new Paragraph(title, sectionFont));
        cell.addElement(new Paragraph(safe(subtitle), bodyFont));
        return cell;
    }

    private String formatDate(java.time.Instant instant) {
        return instant == null ? "—" : DATE_TIME_FORMAT.format(instant);
    }

    private String formatMoney(Double amount) {
        return MONEY_FORMAT.format(amount == null ? 0d : amount) + " FCFA";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}