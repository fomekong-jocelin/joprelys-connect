package com.joprelys.backend.emergency.document;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmergencyDocumentPdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public byte[] generate(
            String title,
            PatientEntity patient,
            EmergencyEntity emergency,
            OrganizationEntity organization,
            byte[] qrCode,
            List<Section> sections) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.BLACK);
        Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
        Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY);

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{75, 25});

            PdfPCell clinic = new PdfPCell();
            clinic.setBorder(Rectangle.NO_BORDER);
            clinic.addElement(new Paragraph(value(organization.getName()), headingFont));
            clinic.addElement(new Paragraph(value(organization.getAddress()), mutedFont));
            clinic.addElement(new Paragraph("Tél : " + value(organization.getPhone()), mutedFont));
            header.addCell(clinic);

            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (qrCode != null) {
                Image image = Image.getInstance(qrCode);
                image.scaleAbsolute(65, 65);
                image.setAlignment(Element.ALIGN_RIGHT);
                qrCell.addElement(image);
            }
            header.addCell(qrCell);
            document.add(header);

            Paragraph documentTitle = new Paragraph(title, titleFont);
            documentTitle.setAlignment(Element.ALIGN_CENTER);
            documentTitle.setSpacingBefore(12);
            documentTitle.setSpacingAfter(12);
            document.add(documentTitle);

            PdfPTable context = new PdfPTable(2);
            context.setWidthPercentage(100);
            context.setWidths(new float[]{50, 50});
            context.addCell(infoCell(
                    "Patient",
                    "Nom affiché : " + value(patient.getDisplayName())
                            + "\nDPU : " + value(patient.getGlobalPatientNumber())
                            + "\nIdentifiant provisoire : " + value(patient.getTemporaryPatientNumber()),
                    headingFont,
                    bodyFont));
            context.addCell(infoCell(
                    "Urgence",
                    "Identifiant : " + emergency.getId()
                            + "\nArrivée : " + format(emergency.getCreatedAt())
                            + "\nOrientation : " + value(emergency.getOrientation()),
                    headingFont,
                    bodyFont));
            document.add(context);

            for (Section section : sections) {
                Paragraph heading = new Paragraph(section.title(), headingFont);
                heading.setSpacingBefore(12);
                heading.setSpacingAfter(4);
                document.add(heading);
                Paragraph content = new Paragraph(value(section.content()), bodyFont);
                content.setLeading(14);
                document.add(content);
            }

            Paragraph footer = new Paragraph(
                    "Document généré par Joprelys Connect. Le numéro, le hash et la version sont conservés lors du rapprochement d'identité.",
                    mutedFont);
            footer.setSpacingBefore(18);
            document.add(footer);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            if (document.isOpen()) {
                document.close();
            }
            throw new IllegalStateException("Impossible de générer le document d'urgence.", exception);
        }
    }

    private PdfPCell infoCell(String title, String content, Font heading, Font body) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.addElement(new Paragraph(title, heading));
        cell.addElement(new Paragraph(content, body));
        return cell;
    }

    private static String value(Object value) {
        return value == null || value.toString().isBlank() ? "Non renseigné" : value.toString();
    }

    private static String format(java.time.Instant instant) {
        return instant == null ? "Non renseigné" : DATE_TIME.format(instant);
    }

    public record Section(String title, String content) {
    }
}
