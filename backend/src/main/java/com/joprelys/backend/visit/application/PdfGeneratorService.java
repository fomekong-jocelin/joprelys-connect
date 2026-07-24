package com.joprelys.backend.visit.application;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VitalsEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.file.FileStorageService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final FileStorageService fileStorageService;
    private final OrganizationRepository organizationRepository;
    private final PatientRepository patientRepository;

    public PdfGeneratorService(
            FileStorageService fileStorageService,
            OrganizationRepository organizationRepository,
            PatientRepository patientRepository) {
        this.fileStorageService = fileStorageService;
        this.organizationRepository = organizationRepository;
        this.patientRepository = patientRepository;
    }

    private Image getScaledImage(String path, float maxWidth, float maxHeight) {
        if (path == null || path.isBlank()) return null;
        try {
            byte[] bytes = fileStorageService.loadFile(path);
            if (bytes != null) {
                Image image = Image.getInstance(bytes);
                image.scaleToFit(maxWidth, maxHeight);
                return image;
            }
        } catch (Exception e) {
            System.err.println("Failed to load PDF image asset: " + path + ". Error: " + e.getMessage());
        }
        return null;
    }

    private PdfPTable createHeaderTable(String logoPath, String clinicName, String clinicAddress, String clinicPhone, byte[] qrCodePngBytes, Font fontMuted) {
        Image logoImg = getScaledImage(logoPath, 70f, 70f);

        PdfPTable headerTable;
        if (logoImg != null) {
            headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100f);
            try {
                headerTable.setWidths(new float[]{15f, 55f, 30f});
            } catch (Exception e) {
                // Ignore
            }

            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.addElement(logoImg);
            headerTable.addCell(logoCell);
        } else {
            headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100f);
            try {
                headerTable.setWidths(new float[]{70f, 30f});
            } catch (Exception e) {
                // Ignore
            }
        }

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph(clinicName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK)));
        leftCell.addElement(new Paragraph(clinicAddress != null ? clinicAddress : "", fontMuted));
        leftCell.addElement(new Paragraph("Tél : " + (clinicPhone != null ? clinicPhone : ""), fontMuted));
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (qrCodePngBytes != null) {
            try {
                Image qrCodeImg = Image.getInstance(qrCodePngBytes);
                qrCodeImg.scaleAbsolute(70f, 70f);
                qrCodeImg.setAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(qrCodeImg);
            } catch (Exception e) {
                // Ignore
            }
        }
        headerTable.addCell(rightCell);
        
        return headerTable;
    }

    private void addSignaturesAndStamp(Document document, UserAccountEntity doctor, Font fontSectionHeader, Font fontBody) throws DocumentException {
        if (doctor == null) return;

        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100f);
        try {
            sigTable.setWidths(new float[]{50f, 50f});
        } catch (Exception e) {
            // Ignore
        }
        sigTable.setSpacingBefore(20f);

        PdfPCell sigCell = new PdfPCell();
        sigCell.setBorder(Rectangle.NO_BORDER);
        sigCell.addElement(new Paragraph("Signature du Praticien", fontSectionHeader));
        sigCell.addElement(new Paragraph(doctor.getDisplayName(), fontBody));
        if (doctor.getRegistrationNumber() != null && !doctor.getRegistrationNumber().isBlank()) {
            sigCell.addElement(new Paragraph("N° Ordre : " + doctor.getRegistrationNumber(), fontBody));
        }
        Image sigImg = getScaledImage(doctor.getSignaturePath(), 120f, 60f);
        if (sigImg != null) {
            sigCell.addElement(new Paragraph(" "));
            sigCell.addElement(sigImg);
        }

        PdfPCell stampCell = new PdfPCell();
        stampCell.setBorder(Rectangle.NO_BORDER);
        stampCell.addElement(new Paragraph("Cachet Professionnel", fontSectionHeader));
        Image stampImg = getScaledImage(doctor.getStampPath(), 120f, 80f);
        if (stampImg != null) {
            stampCell.addElement(new Paragraph(" "));
            stampCell.addElement(stampImg);
        }

        sigTable.addCell(sigCell);
        sigTable.addCell(stampCell);
        document.add(sigTable);
    }

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
            String logoPath = null;
            try {
                OrganizationEntity org = organizationRepository.findById(visit.getOrganizationId()).orElse(null);
                logoPath = (org != null) ? org.getLogoPath() : null;
            } catch (Exception e) {
                // Ignore
            }
            PdfPTable headerTable = createHeaderTable(logoPath, clinicName, clinicAddress, clinicPhone, qrCodePngBytes, fontMuted);
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

            UserAccountEntity doctor = (consultation != null) ? consultation.getDoctor() : null;
            addSignaturesAndStamp(document, doctor, fontSectionHeader, fontBody);

            addFooterMention(document, fontMuted);

            document.close();
        } catch (Exception e) {
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
            String logoPath = null;
            try {
                OrganizationEntity org = organizationRepository.findById(hospitalization.getOrganizationId()).orElse(null);
                logoPath = (org != null) ? org.getLogoPath() : null;
            } catch (Exception e) {
                // Ignore
            }
            PdfPTable headerTable = createHeaderTable(logoPath, clinicName, clinicAddress, clinicPhone, qrCodePngBytes, fontMuted);
            document.add(headerTable);

            // Separation line
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            document.add(new Paragraph(" "));

            // 2. Document Title
            String titleStr = "FICHE DE SORTIE D'HOSPITALISATION";
            if ("SORTI_CONTRE_AVIS".equals(hospitalization.getStatus())) {
                titleStr = "FICHE DE SORTIE CONTRE AVIS MÉDICAL";
            }
            Paragraph title = new Paragraph(titleStr, fontTitle);
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
            stayCell.addElement(new Paragraph("Espace : " + hospitalization.getSpaceName() + " | Lit : " + hospitalization.getBedNumber(), fontBody));
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

            addFooterMention(document, fontMuted);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    public byte[] generateHospitalizationEntryPdf(
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
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

            // 1. En-tête
            String logoPath = null;
            try {
                OrganizationEntity org = organizationRepository.findById(hospitalization.getOrganizationId()).orElse(null);
                logoPath = (org != null) ? org.getLogoPath() : null;
            } catch (Exception e) {
                // Ignore
            }
            PdfPTable headerTable = createHeaderTable(logoPath, clinicName, clinicAddress, clinicPhone, qrCodePngBytes, fontMuted);
            document.add(headerTable);

            // Separation line
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            document.add(new Paragraph(" "));

            // 2. Document Title
            Paragraph title = new Paragraph("BILLET D'ENTRÉE D'HOSPITALISATION", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // 3. Information Tables
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100f);
            infoTable.setWidths(new float[]{50f, 50f});

            PdfPCell stayCell = new PdfPCell();
            stayCell.setBorder(Rectangle.NO_BORDER);
            stayCell.addElement(new Paragraph("Détails d'Admission", fontSectionHeader));
            stayCell.addElement(new Paragraph("Numéro d'hospitalisation : " + hospitalization.getHospitalizationNumber(), fontBody));
            stayCell.addElement(new Paragraph("Service : " + hospitalization.getServiceName(), fontBody));
            stayCell.addElement(new Paragraph("Espace : " + hospitalization.getSpaceName() + " | Lit : " + hospitalization.getBedNumber(), fontBody));
            stayCell.addElement(new Paragraph("Admis le : " + DATE_FORMATTER.format(hospitalization.getAdmittedAt()), fontBody));

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

            addFooterMention(document, fontMuted);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return baos.toByteArray();
    }

    public byte[] generatePatientSummaryPdf(
            com.joprelys.backend.patient.api.MedicalSummaryResponse summary,
            VitalsEntity vitals,
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
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font fontTableHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font fontTableCell = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

            // 1. En-tête avec Clinique à gauche et QR code à droite (via PdfPTable)
            String logoPath = null;
            try {
                java.util.Optional<PatientEntity> patientOpt = patientRepository.findByGlobalPatientNumber(summary.globalPatientNumber());
                UUID orgId = patientOpt.map(PatientEntity::getOrganizationId).orElse(null);
                OrganizationEntity org = (orgId != null) ? organizationRepository.findById(orgId).orElse(null) : null;
                logoPath = (org != null) ? org.getLogoPath() : null;
            } catch (Exception e) {
                // Ignore
            }
            PdfPTable headerTable = createHeaderTable(logoPath, clinicName, clinicAddress, clinicPhone, qrCodePngBytes, fontMuted);
            document.add(headerTable);

            // Separation line
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            
            // Add a small spacing
            Paragraph pSpacing = new Paragraph(" ");
            pSpacing.setLeading(10f);
            document.add(pSpacing);

            // 2. Document Title
            Paragraph title = new Paragraph("SYNTHÈSE MÉDICALE DU PATIENT", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(pSpacing);

            // 3. Section 1 : Identité & Informations Personnelles
            Paragraph sect1 = new Paragraph("1. Identité & Informations Personnelles", fontSectionHeader);
            document.add(sect1);
            document.add(pSpacing);

            PdfPTable identityTable = new PdfPTable(2);
            identityTable.setWidthPercentage(100f);
            identityTable.setWidths(new float[]{50f, 50f});

            PdfPCell col1 = new PdfPCell();
            col1.setBorder(Rectangle.NO_BORDER);
            col1.addElement(new Paragraph("Nom complet : " + summary.fullName(), fontBody));
            col1.addElement(new Paragraph("Date de naissance : " + (summary.birthDate() != null ? summary.birthDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""), fontBody));
            col1.addElement(new Paragraph("Genre : " + summary.gender(), fontBody));
            if (summary.bloodGroup() != null && !summary.bloodGroup().isBlank()) {
                col1.addElement(new Paragraph("Groupe sanguin : " + summary.bloodGroup(), fontBody));
            }
            col1.addElement(new Paragraph("DPU (N° Patient Unique) : " + summary.globalPatientNumber(), fontBody));

            PdfPCell col2 = new PdfPCell();
            col2.setBorder(Rectangle.NO_BORDER);
            col2.addElement(new Paragraph("Téléphone : ", fontBody)); // Optional phone or default details
            document.add(new Paragraph(" ")); // Just placeholder separation

            identityTable.addCell(col1);
            identityTable.addCell(col2);
            document.add(identityTable);
            document.add(pSpacing);

            // 4. Section 2 : Constantes Vitales Récentes
            String vitalsTitleStr = "2. Constantes Vitales Récentes";
            if (vitals != null && vitals.getCreatedAt() != null) {
                vitalsTitleStr += " (Mesurées le " + DATE_FORMATTER.format(vitals.getCreatedAt()) + ")";
            }
            Paragraph sect2 = new Paragraph(vitalsTitleStr, fontSectionHeader);
            document.add(sect2);
            document.add(pSpacing);

            if (vitals != null) {
                PdfPTable vitalsTable = new PdfPTable(4);
                vitalsTable.setWidthPercentage(100f);
                vitalsTable.setWidths(new float[]{25f, 25f, 25f, 25f});
                vitalsTable.getDefaultCell().setBorder(Rectangle.BOX);
                vitalsTable.getDefaultCell().setBorderWidth(0.5f);
                vitalsTable.getDefaultCell().setPadding(5f);

                vitalsTable.addCell(new Phrase("Température : " + (vitals.getTemperature() != null ? vitals.getTemperature() + " °C" : "-"), fontTableCell));
                
                String bp = "-";
                if (vitals.getSystolic() != null && vitals.getDiastolic() != null) {
                    bp = vitals.getSystolic() + "/" + vitals.getDiastolic() + " mmHg";
                }
                vitalsTable.addCell(new Phrase("Pression Artérielle : " + bp, fontTableCell));
                vitalsTable.addCell(new Phrase("Fréquence Cardiaque : " + (vitals.getPulse() != null ? vitals.getPulse() + " bpm" : "-"), fontTableCell));
                vitalsTable.addCell(new Phrase("SpO2 : " + (vitals.getSpo2() != null ? vitals.getSpo2() + " %" : "-"), fontTableCell));

                vitalsTable.addCell(new Phrase("Glycémie : " + (vitals.getGlycemia() != null ? vitals.getGlycemia() + " g/L" : "-"), fontTableCell));
                vitalsTable.addCell(new Phrase("Fréquence Resp. : " + (vitals.getRespiratoryRate() != null ? vitals.getRespiratoryRate() + " cpm" : "-"), fontTableCell));
                
                String wtHt = "-";
                if (vitals.getWeight() != null && vitals.getHeight() != null) {
                    wtHt = vitals.getWeight() + " kg / " + vitals.getHeight() + " cm";
                } else if (vitals.getWeight() != null) {
                    wtHt = vitals.getWeight() + " kg";
                } else if (vitals.getHeight() != null) {
                    wtHt = vitals.getHeight() + " cm";
                }
                vitalsTable.addCell(new Phrase("Poids / Taille : " + wtHt, fontTableCell));
                vitalsTable.addCell(new Phrase("IMC : " + (vitals.getBmi() != null ? vitals.getBmi().toString() : "-"), fontTableCell));

                document.add(vitalsTable);
            } else {
                document.add(new Paragraph("Aucune constante vitale récente enregistrée.", fontMuted));
            }
            document.add(pSpacing);

            // 5. Section 3 : Allergies & Intolérances Actives
            Paragraph sect3 = new Paragraph("3. Allergies & Intolérances Actives", fontSectionHeader);
            document.add(sect3);
            document.add(pSpacing);
            
            if (summary.allergies() != null && !summary.allergies().isEmpty()) {
                PdfPTable allergiesTable = new PdfPTable(4);
                allergiesTable.setWidthPercentage(100f);
                allergiesTable.setWidths(new float[]{30f, 20f, 30f, 20f});
                allergiesTable.getDefaultCell().setBorder(Rectangle.BOX);
                allergiesTable.getDefaultCell().setBorderWidth(0.5f);
                allergiesTable.getDefaultCell().setPadding(5f);

                allergiesTable.addCell(new Phrase("Substance", fontTableHead));
                allergiesTable.addCell(new Phrase("Sévérité", fontTableHead));
                allergiesTable.addCell(new Phrase("Réaction", fontTableHead));
                allergiesTable.addCell(new Phrase("Date Découverte", fontTableHead));

                for (var a : summary.allergies()) {
                    allergiesTable.addCell(new Phrase(a.substance(), fontTableCell));
                    allergiesTable.addCell(new Phrase(a.severity(), fontTableCell));
                    allergiesTable.addCell(new Phrase(a.reaction() != null ? a.reaction() : "-", fontTableCell));
                    allergiesTable.addCell(new Phrase(a.discoveredAt() != null ? a.discoveredAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", fontTableCell));
                }
                document.add(allergiesTable);
            } else {
                document.add(new Paragraph("Aucune allergie active signalée.", fontMuted));
            }
            document.add(pSpacing);

            // 6. Section 4 : Antécédents Médicaux (Importants ou En cours)
            Paragraph sect4 = new Paragraph("4. Antécédents Médicaux Principaux", fontSectionHeader);
            document.add(sect4);
            document.add(pSpacing);

            if (summary.medicalHistory() != null && !summary.medicalHistory().isEmpty()) {
                PdfPTable historyTable = new PdfPTable(4);
                historyTable.setWidthPercentage(100f);
                historyTable.setWidths(new float[]{20f, 40f, 20f, 20f});
                historyTable.getDefaultCell().setBorder(Rectangle.BOX);
                historyTable.getDefaultCell().setBorderWidth(0.5f);
                historyTable.getDefaultCell().setPadding(5f);

                historyTable.addCell(new Phrase("Catégorie", fontTableHead));
                historyTable.addCell(new Phrase("Description", fontTableHead));
                historyTable.addCell(new Phrase("Date de Début", fontTableHead));
                historyTable.addCell(new Phrase("Important / En cours", fontTableHead));

                for (var h : summary.medicalHistory()) {
                    historyTable.addCell(new Phrase(h.category(), fontTableCell));
                    historyTable.addCell(new Phrase(h.description(), fontTableCell));
                    historyTable.addCell(new Phrase(h.onsetDate() != null ? h.onsetDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-", fontTableCell));
                    
                    String statusStr = (h.important() ? "⚠️ Important" : "") + 
                                       (h.isOngoing() ? (h.important() ? " / " : "") + "En cours" : "");
                    historyTable.addCell(new Phrase(statusStr, fontTableCell));
                }
                document.add(historyTable);
            } else {
                document.add(new Paragraph("Aucun antécédent médical principal signalé.", fontMuted));
            }
            document.add(pSpacing);

            // 7. Section 5 : Traitements en cours
            Paragraph sect5 = new Paragraph("5. Traitements Actuels en cours", fontSectionHeader);
            document.add(sect5);
            document.add(pSpacing);

            if (summary.activePrescriptions() != null && !summary.activePrescriptions().isEmpty()) {
                PdfPTable treatmentsTable = new PdfPTable(4);
                treatmentsTable.setWidthPercentage(100f);
                treatmentsTable.setWidths(new float[]{30f, 30f, 20f, 20f});
                treatmentsTable.getDefaultCell().setBorder(Rectangle.BOX);
                treatmentsTable.getDefaultCell().setBorderWidth(0.5f);
                treatmentsTable.getDefaultCell().setPadding(5f);

                treatmentsTable.addCell(new Phrase("Médicament", fontTableHead));
                treatmentsTable.addCell(new Phrase("Dosage / Instructions", fontTableHead));
                treatmentsTable.addCell(new Phrase("Posologie", fontTableHead));
                treatmentsTable.addCell(new Phrase("Durée", fontTableHead));

                for (var p : summary.activePrescriptions()) {
                    for (var item : p.items()) {
                        treatmentsTable.addCell(new Phrase(item.drugName(), fontTableCell));
                        treatmentsTable.addCell(new Phrase((item.dosage() != null ? item.dosage() : "") + 
                                                           (item.instructions() != null ? " (" + item.instructions() + ")" : ""), fontTableCell));
                        treatmentsTable.addCell(new Phrase(item.posology() != null ? item.posology() : "-", fontTableCell));
                        treatmentsTable.addCell(new Phrase(item.duration() != null ? item.duration() : "-", fontTableCell));
                    }
                }
                document.add(treatmentsTable);
            } else {
                document.add(new Paragraph("Aucun traitement médicamenteux actif en cours.", fontMuted));
            }
            document.add(pSpacing);

            // 8. Section 6 : Dernières Visites (3 dernières)
            Paragraph sect6 = new Paragraph("6. Historique des Dernières Visites", fontSectionHeader);
            document.add(sect6);
            document.add(pSpacing);

            if (summary.recentVisits() != null && !summary.recentVisits().isEmpty()) {
                PdfPTable visitsTable = new PdfPTable(4);
                visitsTable.setWidthPercentage(100f);
                visitsTable.setWidths(new float[]{25f, 25f, 35f, 15f});
                visitsTable.getDefaultCell().setBorder(Rectangle.BOX);
                visitsTable.getDefaultCell().setBorderWidth(0.5f);
                visitsTable.getDefaultCell().setPadding(5f);

                visitsTable.addCell(new Phrase("Date", fontTableHead));
                visitsTable.addCell(new Phrase("N° Visite", fontTableHead));
                visitsTable.addCell(new Phrase("Motif de visite", fontTableHead));
                visitsTable.addCell(new Phrase("Service", fontTableHead));

                for (var v : summary.recentVisits()) {
                    visitsTable.addCell(new Phrase(DATE_FORMATTER.format(v.createdAt()), fontTableCell));
                    visitsTable.addCell(new Phrase(v.visitNumber(), fontTableCell));
                    visitsTable.addCell(new Phrase(v.reason(), fontTableCell));
                    visitsTable.addCell(new Phrase(v.service() != null ? v.service() : "-", fontTableCell));
                }
                document.add(visitsTable);
            } else {
                document.add(new Paragraph("Aucune visite enregistrée.", fontMuted));
            }
            document.add(pSpacing);

            // 9. Section 7 : Derniers Diagnostics & Conclusions (3 derniers)
            Paragraph sect7 = new Paragraph("7. Derniers Diagnostics & Conclusions", fontSectionHeader);
            document.add(sect7);
            document.add(pSpacing);

            if (summary.recentDiagnostics() != null && !summary.recentDiagnostics().isEmpty()) {
                PdfPTable diagTable = new PdfPTable(4);
                diagTable.setWidthPercentage(100f);
                diagTable.setWidths(new float[]{20f, 30f, 30f, 20f});
                diagTable.getDefaultCell().setBorder(Rectangle.BOX);
                diagTable.getDefaultCell().setBorderWidth(0.5f);
                diagTable.getDefaultCell().setPadding(5f);

                diagTable.addCell(new Phrase("Date", fontTableHead));
                diagTable.addCell(new Phrase("Diagnostic Suspecté", fontTableHead));
                diagTable.addCell(new Phrase("Diagnostic Final / Conclusion", fontTableHead));
                diagTable.addCell(new Phrase("Médecin", fontTableHead));

                for (var d : summary.recentDiagnostics()) {
                    diagTable.addCell(new Phrase(DATE_FORMATTER.format(d.createdAt()), fontTableCell));
                    diagTable.addCell(new Phrase(d.suspectedDiagnosis() != null ? d.suspectedDiagnosis() : "-", fontTableCell));
                    
                    String mainDiag = d.diagnosis();
                    if (d.finalDiagnosis() != null && !d.finalDiagnosis().isBlank()) {
                        mainDiag = d.finalDiagnosis();
                    }
                    if (d.conclusion() != null && !d.conclusion().isBlank()) {
                        mainDiag += " (" + d.conclusion() + ")";
                    }
                    diagTable.addCell(new Phrase(mainDiag, fontTableCell));
                    diagTable.addCell(new Phrase(d.doctorName(), fontTableCell));
                }
                document.add(diagTable);
            } else {
                document.add(new Paragraph("Aucun diagnostic enregistré.", fontMuted));
            }
            document.add(pSpacing);

            // 10. Section 8 : Résultats Biologiques Critiques
            Paragraph sect8 = new Paragraph("8. Résultats Biologiques Critiques récents", fontSectionHeader);
            document.add(sect8);
            document.add(pSpacing);

            if (summary.criticalResults() != null && !summary.criticalResults().isEmpty()) {
                PdfPTable critTable = new PdfPTable(5);
                critTable.setWidthPercentage(100f);
                critTable.setWidths(new float[]{20f, 20f, 30f, 15f, 15f});
                critTable.getDefaultCell().setBorder(Rectangle.BOX);
                critTable.getDefaultCell().setBorderWidth(0.5f);
                critTable.getDefaultCell().setPadding(5f);

                critTable.addCell(new Phrase("Date validation", fontTableHead));
                critTable.addCell(new Phrase("N° Résultat", fontTableHead));
                critTable.addCell(new Phrase("Analyse / Paramètre", fontTableHead));
                critTable.addCell(new Phrase("Valeur", fontTableHead));
                critTable.addCell(new Phrase("Interprétation", fontTableHead));

                for (var r : summary.criticalResults()) {
                    critTable.addCell(new Phrase(r.validatedAt() != null ? DATE_FORMATTER.format(r.validatedAt()) : "-", fontTableCell));
                    critTable.addCell(new Phrase(r.resultNumber(), fontTableCell));
                    critTable.addCell(new Phrase(r.analyteName(), fontTableCell));
                    critTable.addCell(new Phrase(r.value() + " " + (r.unit() != null ? r.unit() : ""), fontTableCell));
                    critTable.addCell(new Phrase(r.interpretation(), fontTableCell));
                }
                document.add(critTable);
            } else {
                document.add(new Paragraph("Aucun résultat biologique critique récent.", fontMuted));
            }

            addFooterMention(document, fontMuted);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate patient summary PDF", e);
        }

        return baos.toByteArray();
    }

    public byte[] generatePrescriptionPdf(
            VisitEntity visit,
            ConsultationEntity consultation,
            PrescriptionEntity prescription,
            String clinicName,
            String clinicAddress,
            String clinicPhone,
            String doctorName,
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
            String logoPath = null;
            try {
                OrganizationEntity org = organizationRepository.findById(visit.getOrganizationId()).orElse(null);
                logoPath = (org != null) ? org.getLogoPath() : null;
            } catch (Exception e) {
                // Ignore
            }
            PdfPTable headerTable = createHeaderTable(logoPath, clinicName, clinicAddress, clinicPhone, qrCodePngBytes, fontMuted);
            document.add(headerTable);

            // Ligne de séparation
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            document.add(new Paragraph(" "));

            // 2. Titre du document
            Paragraph title = new Paragraph("ORDONNANCE MÉDICALE", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // 3. Informations Patient & Ordonnance (via PdfPTable)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100f);
            infoTable.setWidths(new float[]{50f, 50f});

            PdfPCell patientCell = new PdfPCell();
            patientCell.setBorder(Rectangle.BOX);
            patientCell.setBorderWidth(0.5f);
            patientCell.setPadding(8f);
            patientCell.addElement(new Paragraph("PATIENT", fontSectionHeader));
            patientCell.addElement(new Paragraph("Nom complet : " + (visit != null && visit.getPatient() != null ? visit.getPatient().getFullName() : "Inconnu"), fontBodyBold));
            patientCell.addElement(new Paragraph("DPU : " + (visit != null && visit.getPatient() != null ? visit.getPatient().getGlobalPatientNumber() : "-"), fontBody));
            if (visit != null && visit.getPatient() != null && visit.getPatient().getBirthDate() != null) {
                patientCell.addElement(new Paragraph("Date de naissance : " + visit.getPatient().getBirthDate().toString(), fontBody));
            }

            PdfPCell prescCell = new PdfPCell();
            prescCell.setBorder(Rectangle.BOX);
            prescCell.setBorderWidth(0.5f);
            prescCell.setPadding(8f);
            prescCell.addElement(new Paragraph("PRESCRIPTION", fontSectionHeader));
            prescCell.addElement(new Paragraph("N° Ordonnance : " + (prescription.getPrescriptionNumber() != null ? prescription.getPrescriptionNumber() : "-"), fontBodyBold));
            prescCell.addElement(new Paragraph("Date : " + (prescription.getIssuedAt() != null ? DATE_FORMATTER.format(prescription.getIssuedAt()) : DATE_FORMATTER.format(Instant.now())), fontBody));
            prescCell.addElement(new Paragraph("Médecin prescripteur : " + (doctorName != null ? doctorName : "Médecin clinicien"), fontBody));
            if (prescription.getExpiresAt() != null) {
                prescCell.addElement(new Paragraph("Expire le : " + DATE_FORMATTER.format(prescription.getExpiresAt()), fontBodyBold));
            }

            infoTable.addCell(patientCell);
            infoTable.addCell(prescCell);
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 4. Liste des médicaments prescrits (Tableau)
            Paragraph sectItems = new Paragraph("Médicaments prescrits", fontSectionHeader);
            document.add(sectItems);
            document.add(new Paragraph(" "));

            PdfPTable itemsTable = new PdfPTable(7);
            itemsTable.setWidthPercentage(100f);
            itemsTable.setWidths(new float[]{25f, 12f, 12f, 15f, 10f, 10f, 16f});
            itemsTable.getDefaultCell().setBorder(Rectangle.BOX);
            itemsTable.getDefaultCell().setBorderWidth(0.5f);
            itemsTable.getDefaultCell().setPadding(5f);

            itemsTable.addCell(new Phrase("Médicament", fontTableHead));
            itemsTable.addCell(new Phrase("Dosage", fontTableHead));
            itemsTable.addCell(new Phrase("Forme", fontTableHead));
            itemsTable.addCell(new Phrase("Posologie", fontTableHead));
            itemsTable.addCell(new Phrase("Durée", fontTableHead));
            itemsTable.addCell(new Phrase("Qté", fontTableHead));
            itemsTable.addCell(new Phrase("Substitution", fontTableHead));

            for (PrescriptionItemEntity item : prescription.getItems()) {
                itemsTable.addCell(new Phrase(item.getDrugName(), fontTableCell));
                itemsTable.addCell(new Phrase(item.getDosage(), fontTableCell));
                itemsTable.addCell(new Phrase(item.getForm() != null ? item.getForm() : "-", fontTableCell));
                itemsTable.addCell(new Phrase(item.getPosology() != null ? item.getPosology() : "-", fontTableCell));
                itemsTable.addCell(new Phrase(item.getDuration() != null ? item.getDuration() : "-", fontTableCell));
                itemsTable.addCell(new Phrase(item.getQuantity() != null ? item.getQuantity() : "-", fontTableCell));
                itemsTable.addCell(new Phrase(item.isSubstitutionAllowed() ? "Autorisée" : "Interdite", fontTableCell));
            }
            document.add(itemsTable);
            document.add(new Paragraph(" "));

            // Instructions additionnelles
            for (PrescriptionItemEntity item : prescription.getItems()) {
                if (item.getInstructions() != null && !item.getInstructions().isBlank()) {
                    Paragraph inst = new Paragraph("Instructions pour " + item.getDrugName() + " : " + item.getInstructions(), fontMuted);
                    document.add(inst);
                }
            }

            document.add(new Paragraph(" "));
            Paragraph pinText = new Paragraph("Code de vérification sécurisé (PIN) : " + (prescription.getPinCode() != null ? prescription.getPinCode() : "-"), fontBodyBold);
            document.add(pinText);

            UserAccountEntity doctor = (prescription != null && prescription.getConsultation() != null)
                    ? prescription.getConsultation().getDoctor() : null;
            addSignaturesAndStamp(document, doctor, fontSectionHeader, fontBody);

            addFooterMention(document, fontMuted);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate prescription PDF", e);
        }

        return baos.toByteArray();
    }

    public byte[] generateInvoicePdf(
            com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity invoice,
            PatientEntity patient,
            String clinicName,
            String clinicAddress,
            String clinicPhone,
            byte[] qrCodePngBytes,
            String cashierName,
            BigDecimal totalPaid) {
        Document document = new Document(PageSize.A4, 36f, 36f, 36f, 36f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Font fontSectionHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font fontBodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font fontTableHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font fontTableCell = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            // 1. Header
            String logoPath = null;
            try {
                OrganizationEntity org = organizationRepository.findById(invoice.getOrganizationId()).orElse(null);
                logoPath = (org != null) ? org.getLogoPath() : null;
            } catch (Exception ignored) {}

            PdfPTable headerTable = createHeaderTable(logoPath, clinicName, clinicAddress, clinicPhone, qrCodePngBytes, fontMuted);
            document.add(headerTable);

            // Separator
            Paragraph separator = new Paragraph("______________________________________________________________________________",
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.LIGHT_GRAY));
            separator.setAlignment(Element.ALIGN_CENTER);
            document.add(separator);
            document.add(new Paragraph(" "));

            // 2. Title
            Paragraph title = new Paragraph("FACTURE DE SOINS MÉDICAUX", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // 3. Info Table (Patient / Invoice Details)
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100f);
            infoTable.setWidths(new float[]{50f, 50f});

            PdfPCell patientCell = new PdfPCell();
            patientCell.setBorder(Rectangle.BOX);
            patientCell.setBorderWidth(0.5f);
            patientCell.setPadding(8f);
            patientCell.addElement(new Paragraph("PATIENT", fontSectionHeader));
            patientCell.addElement(new Paragraph("Nom complet : " + patient.getFullName(), fontBodyBold));
            patientCell.addElement(new Paragraph("DPU : " + patient.getGlobalPatientNumber(), fontBody));
            if (patient.getPhone() != null) {
                patientCell.addElement(new Paragraph("Tél : " + patient.getPhone(), fontBody));
            }

            PdfPCell invoiceCell = new PdfPCell();
            invoiceCell.setBorder(Rectangle.BOX);
            invoiceCell.setBorderWidth(0.5f);
            invoiceCell.setPadding(8f);
            invoiceCell.addElement(new Paragraph("FACTURE", fontSectionHeader));
            invoiceCell.addElement(new Paragraph("N° Facture : " + invoice.getInvoiceNumber(), fontBodyBold));
            invoiceCell.addElement(new Paragraph("Date : " + DATE_FORMATTER.format(invoice.getCreatedAt()), fontBody));
            invoiceCell.addElement(new Paragraph("Statut : " + invoice.getStatus().name(), fontBodyBold));
            if (invoice.getInsuranceConvention() != null) {
                String coveragePercentage = invoice.getInsuranceConvention().getCoveragePercentage().movePointRight(2).stripTrailingZeros().toPlainString();
                invoiceCell.addElement(new Paragraph("Convention : " + invoice.getInsuranceConvention().getName() + " (" + coveragePercentage + "%)", fontBody));
            }

            infoTable.addCell(patientCell);
            infoTable.addCell(invoiceCell);
            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 4. Invoice Items Table
            Paragraph sectItems = new Paragraph("Prestations & Actes facturés", fontSectionHeader);
            document.add(sectItems);
            document.add(new Paragraph(" "));

            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100f);
            itemsTable.setWidths(new float[]{45f, 15f, 10f, 10f, 20f});
            itemsTable.getDefaultCell().setBorder(Rectangle.BOX);
            itemsTable.getDefaultCell().setBorderWidth(0.5f);
            itemsTable.getDefaultCell().setPadding(5f);

            itemsTable.addCell(new Phrase("Libellé Prestation", fontTableHead));
            itemsTable.addCell(new Phrase("Type", fontTableHead));
            itemsTable.addCell(new Phrase("Prix Unitaire", fontTableHead));
            itemsTable.addCell(new Phrase("Qté / Coeff", fontTableHead));
            itemsTable.addCell(new Phrase("Total (FCFA)", fontTableHead));

            for (com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity item : invoice.getItems()) {
                itemsTable.addCell(new Phrase(item.getLabel(), fontTableCell));
                itemsTable.addCell(new Phrase(item.getItemType().name(), fontTableCell));
                itemsTable.addCell(new Phrase(String.format("%,.0f", item.getUnitPrice()), fontTableCell));

                String qtyOrCoeff = String.format("%,.1f", item.getQuantity());
                if (item.getCoefficient() != null && item.getCoefficient().compareTo(BigDecimal.ONE) != 0) {
                    qtyOrCoeff += " x " + String.format("%,.1f", item.getCoefficient());
                }
                itemsTable.addCell(new Phrase(qtyOrCoeff, fontTableCell));

                itemsTable.addCell(new Phrase(String.format("%,.0f", item.getTotalItemAmount()), fontTableCell));
            }
            document.add(itemsTable);
            document.add(new Paragraph(" "));

            // 5. Totals & Répartition Table
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100f);
            totalsTable.setWidths(new float[]{65f, 35f});

            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);
            totalsTable.addCell(emptyCell);

            PdfPCell costCell = new PdfPCell();
            costCell.setBorder(Rectangle.BOX);
            costCell.setBorderWidth(0.5f);
            costCell.setPadding(8f);

            costCell.addElement(new Paragraph(String.format("Montant Total : %,.0f FCFA", invoice.getTotalAmount()), fontBodyBold));
            if (invoice.getInsuranceConvention() != null) {
                costCell.addElement(new Paragraph(String.format("Part Assurance : %,.0f FCFA", invoice.getInsuranceShare()), fontBody));
                costCell.addElement(new Paragraph(String.format("Part Patient (Ticket Mod.) : %,.0f FCFA", invoice.getPatientShare()), fontBodyBold));
            }
            costCell.addElement(new Paragraph(String.format("Règlements reçus : %,.0f FCFA", totalPaid), fontBody));

            BigDecimal balance = invoice.getPatientShare().subtract(totalPaid).max(BigDecimal.ZERO);
            costCell.addElement(new Paragraph(String.format("Solde Dû : %,.0f FCFA", balance), fontBodyBold));

            totalsTable.addCell(costCell);
            document.add(totalsTable);
            document.add(new Paragraph(" "));

            // Signatures
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100f);
            sigTable.setWidths(new float[]{50f, 50f});
            sigTable.setSpacingBefore(15f);

            PdfPCell cashierCell = new PdfPCell();
            cashierCell.setBorder(Rectangle.NO_BORDER);
            cashierCell.addElement(new Paragraph("Signature Caissier", fontSectionHeader));
            cashierCell.addElement(new Paragraph(cashierName != null ? cashierName : "Caisse centrale", fontBody));
            sigTable.addCell(cashierCell);

            PdfPCell stampCell = new PdfPCell();
            stampCell.setBorder(Rectangle.NO_BORDER);
            stampCell.addElement(new Paragraph("Cachet de l'Établissement", fontSectionHeader));
            sigTable.addCell(stampCell);

            document.add(sigTable);

            addFooterMention(document, fontMuted);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }

        return baos.toByteArray();
    }

    private void addFooterMention(Document document, Font font) throws DocumentException {
        Paragraph footerParagraph = new Paragraph("Propulsé par Joprelys HealthTech — Document généré électroniquement", font);
        footerParagraph.setAlignment(Element.ALIGN_CENTER);
        footerParagraph.setSpacingBefore(15f);
        document.add(footerParagraph);
    }
}
