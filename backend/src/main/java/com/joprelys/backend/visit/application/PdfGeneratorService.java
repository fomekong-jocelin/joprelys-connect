package com.joprelys.backend.visit.application;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public byte[] generatePdf(
            VisitEntity visit,
            ConsultationEntity consultation,
            List<PrescriptionItemEntity> prescriptionItems,
            String clinicName,
            String clinicAddress,
            String clinicPhone,
            byte[] qrCodePngBytes) {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts definitions
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font fontSectionHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font fontBodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font fontTableHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font fontTableCell = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            // 1. En-tête avec Clinique à gauche et QR code à droite (via PdfPTable)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100f);
            headerTable.setWidths(new float[]{70f, 30f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(clinicName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK)));
            leftCell.addElement(new Paragraph(clinicAddress != null ? clinicAddress : "", fontMuted));
            leftCell.addElement(new Paragraph("Tél : " + (clinicPhone != null ? clinicPhone : ""), fontMuted));

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (qrCodePngBytes != null) {
                Image qrCodeImg = Image.getInstance(qrCodePngBytes);
                qrCodeImg.scaleAbsolute(70f, 70f);
                qrCodeImg.setAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(qrCodeImg);
            }

            headerTable.addCell(leftCell);
            headerTable.addCell(rightCell);
            document.add(headerTable);

            // Ligne de séparation
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            document.add(new Paragraph(" "));

            // 2. Titre du document
            Paragraph title = new Paragraph("COMPTE-RENDU DE CONSULTATION", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // 3. Informations Patient & Visite (via PdfPTable)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100f);
            infoTable.setWidths(new float[]{50f, 50f});

            PdfPCell visitCell = new PdfPCell();
            visitCell.setBorder(Rectangle.NO_BORDER);
            visitCell.addElement(new Paragraph("Informations de la Visite", fontSectionHeader));
            visitCell.addElement(new Paragraph("Numéro de visite : " + visit.getVisitNumber(), fontBody));
            visitCell.addElement(new Paragraph("Date : " + DATE_FORMATTER.format(visit.getCreatedAt()), fontBody));
            if (consultation != null && consultation.getDoctor() != null) {
                visitCell.addElement(new Paragraph("Praticien : " + consultation.getDoctor().getDisplayName(), fontBody));
            }

            PdfPCell patientCell = new PdfPCell();
            patientCell.setBorder(Rectangle.NO_BORDER);
            patientCell.addElement(new Paragraph("Informations du Patient", fontSectionHeader));
            patientCell.addElement(new Paragraph("Nom : " + visit.getPatient().getFullName(), fontBody));
            patientCell.addElement(new Paragraph("DPU : " + visit.getPatient().getGlobalPatientNumber(), fontBody));
            patientCell.addElement(new Paragraph("Tél : " + visit.getPatient().getPhone(), fontBody));

            infoTable.addCell(visitCell);
            infoTable.addCell(patientCell);
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 4. Motif & Constantes Vitales
            Paragraph sectionVitals = new Paragraph("1. Motif & Constantes Vitales", fontSectionHeader);
            document.add(sectionVitals);
            document.add(new Paragraph("Motif de visite : " + visit.getReason(), fontBody));
            document.add(new Paragraph(" "));

            VitalsEntity vitals = visit.getVitals();
            if (vitals != null) {
                PdfPTable vitalsTable = new PdfPTable(5);
                vitalsTable.setWidthPercentage(100f);
                vitalsTable.getDefaultCell().setBorder(Rectangle.BOTTOM);
                vitalsTable.getDefaultCell().setBorderWidth(0.5f);
                vitalsTable.getDefaultCell().setPadding(5f);

                vitalsTable.addCell(new Phrase("Température", fontTableHead));
                vitalsTable.addCell(new Phrase("Poids", fontTableHead));
                vitalsTable.addCell(new Phrase("Taille", fontTableHead));
                vitalsTable.addCell(new Phrase("IMC", fontTableHead));
                vitalsTable.addCell(new Phrase("Tension / Pouls", fontTableHead));

                String temp = vitals.getTemperature() != null ? vitals.getTemperature() + " °C" : "-";
                String weight = vitals.getWeight() != null ? vitals.getWeight() + " kg" : "-";
                String height = vitals.getHeight() != null ? vitals.getHeight() + " cm" : "-";
                String bmi = vitals.getBmi() != null ? vitals.getBmi().toString() : "-";
                String bpPulse = "-";
                if (vitals.getSystolic() != null && vitals.getDiastolic() != null) {
                    bpPulse = vitals.getSystolic() + "/" + vitals.getDiastolic() + " mmHg";
                }
                if (vitals.getPulse() != null) {
                    bpPulse += " (" + vitals.getPulse() + " bpm)";
                }

                vitalsTable.addCell(new Phrase(temp, fontTableCell));
                vitalsTable.addCell(new Phrase(weight, fontTableCell));
                vitalsTable.addCell(new Phrase(height, fontTableCell));
                vitalsTable.addCell(new Phrase(bmi, fontTableCell));
                vitalsTable.addCell(new Phrase(bpPulse, fontTableCell));

                document.add(vitalsTable);
            } else {
                document.add(new Paragraph("Aucune constante vitale enregistrée.", fontMuted));
            }
            document.add(new Paragraph(" "));

            // 5. Diagnostic / Consultation
            Paragraph sectionConsult = new Paragraph("2. Détails de la Consultation", fontSectionHeader);
            document.add(sectionConsult);

            if (consultation != null) {
                document.add(new Paragraph("Symptômes : " + consultation.getSymptoms(), fontBody));
                if (consultation.getClinicalExam() != null && !consultation.getClinicalExam().isBlank()) {
                    document.add(new Paragraph("Examen clinique : " + consultation.getClinicalExam(), fontBody));
                }
                document.add(new Paragraph("Diagnostic : " + consultation.getDiagnosis(), fontBody));
                if (consultation.getAdvice() != null && !consultation.getAdvice().isBlank()) {
                    document.add(new Paragraph("Conseils / Recommandations : " + consultation.getAdvice(), fontBody));
                }
            } else {
                document.add(new Paragraph("Aucun détail de consultation enregistré.", fontMuted));
            }
            document.add(new Paragraph(" "));

            // 6. Prescription
            if (prescriptionItems != null && !prescriptionItems.isEmpty()) {
                Paragraph sectionPrescription = new Paragraph("3. Prescription Médicale (Ordonnance)", fontSectionHeader);
                document.add(sectionPrescription);
                document.add(new Paragraph(" "));

                PdfPTable pTable = new PdfPTable(4);
                pTable.setWidthPercentage(100f);
                pTable.setWidths(new float[]{30f, 20f, 25f, 25f});
                pTable.getDefaultCell().setBorder(Rectangle.BOTTOM);
                pTable.getDefaultCell().setBorderWidth(0.5f);
                pTable.getDefaultCell().setPadding(5f);

                pTable.addCell(new Phrase("Médicament", fontTableHead));
                pTable.addCell(new Phrase("Dosage / Quantité", fontTableHead));
                pTable.addCell(new Phrase("Posologie / Durée", fontTableHead));
                pTable.addCell(new Phrase("Instructions", fontTableHead));

                for (PrescriptionItemEntity item : prescriptionItems) {
                    pTable.addCell(new Phrase(item.getDrugName(), fontTableCell));
                    String dosageQty = item.getDosage();
                    if (item.getQuantity() != null && !item.getQuantity().isBlank()) {
                        dosageQty += " (" + item.getQuantity() + ")";
                    }
                    pTable.addCell(new Phrase(dosageQty, fontTableCell));

                    String posoDur = item.getPosology() != null ? item.getPosology() : "";
                    if (item.getDuration() != null && !item.getDuration().isBlank()) {
                        posoDur += " pendant " + item.getDuration();
                    }
                    pTable.addCell(new Phrase(posoDur, fontTableCell));
                    pTable.addCell(new Phrase(item.getInstructions() != null ? item.getInstructions() : "", fontTableCell));
                }
                document.add(pTable);
            }

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    public byte[] generateHospitalizationDischargePdf(
            com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity hospitalization,
            com.joprelys.backend.patient.infrastructure.persistence.PatientEntity patient,
            String clinicName,
            String clinicAddress,
            String clinicPhone,
            byte[] qrCodePngBytes) {

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts definitions
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font fontSectionHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font fontBodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            // 1. En-tête
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100f);
            headerTable.setWidths(new float[]{70f, 30f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(clinicName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK)));
            leftCell.addElement(new Paragraph(clinicAddress != null ? clinicAddress : "", fontMuted));
            leftCell.addElement(new Paragraph("Tél : " + (clinicPhone != null ? clinicPhone : ""), fontMuted));

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (qrCodePngBytes != null) {
                Image qrCodeImg = Image.getInstance(qrCodePngBytes);
                qrCodeImg.scaleAbsolute(70f, 70f);
                qrCodeImg.setAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(qrCodeImg);
            }

            headerTable.addCell(leftCell);
            headerTable.addCell(rightCell);
            document.add(headerTable);

            // Separation line
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            document.add(new Paragraph(" "));

            // 2. Document Title
            Paragraph title = new Paragraph("FICHE DE SORTIE D'HOSPITALISATION", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // 3. Information Tables
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100f);
            infoTable.setWidths(new float[]{50f, 50f});

            PdfPCell stayCell = new PdfPCell();
            stayCell.setBorder(Rectangle.NO_BORDER);
            stayCell.addElement(new Paragraph("Détails du Séjour", fontSectionHeader));
            stayCell.addElement(new Paragraph("Service : " + hospitalization.getServiceName(), fontBody));
            stayCell.addElement(new Paragraph("Chambre : " + hospitalization.getRoomNumber() + " | Lit : " + hospitalization.getBedNumber(), fontBody));
            stayCell.addElement(new Paragraph("Admis le : " + DATE_FORMATTER.format(hospitalization.getAdmittedAt()), fontBody));
            if (hospitalization.getDischargedAt() != null) {
                stayCell.addElement(new Paragraph("Sorti le : " + DATE_FORMATTER.format(hospitalization.getDischargedAt()), fontBody));
            }

            PdfPCell patientCell = new PdfPCell();
            patientCell.setBorder(Rectangle.NO_BORDER);
            patientCell.addElement(new Paragraph("Informations du Patient", fontSectionHeader));
            patientCell.addElement(new Paragraph("Nom : " + patient.getFullName(), fontBody));
            patientCell.addElement(new Paragraph("DPU : " + patient.getGlobalPatientNumber(), fontBody));
            patientCell.addElement(new Paragraph("Tél : " + patient.getPhone(), fontBody));

            infoTable.addCell(stayCell);
            infoTable.addCell(patientCell);
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 4. Clinical Details
            document.add(new Paragraph("Motif d'hospitalisation :", fontSectionHeader));
            document.add(new Paragraph(hospitalization.getAdmissionReason(), fontBody));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Diagnostic de sortie :", fontSectionHeader));
            document.add(new Paragraph(hospitalization.getDischargeDiagnosis() != null ? hospitalization.getDischargeDiagnosis() : "Non renseigné", fontBody));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Consignes médicales & Prescriptions de sortie :", fontSectionHeader));
            document.add(new Paragraph(hospitalization.getDischargeInstructions() != null ? hospitalization.getDischargeInstructions() : "Non renseigné", fontBody));
            document.add(new Paragraph(" "));

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }
}
