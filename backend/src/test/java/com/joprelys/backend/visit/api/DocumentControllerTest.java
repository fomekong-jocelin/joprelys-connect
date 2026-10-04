package com.joprelys.backend.visit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.io.File;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicalDocumentRepository medicalDocumentRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserAccountEntity userMedecinA;
    private UserAccountEntity userAdminJoprelys;
    private UserAccountEntity userPharmacienA;

    private String tokenMedecinA;
    private String tokenAdminJoprelys;
    private String tokenPharmacienA;

    private PatientEntity patientA;
    private PatientEntity patientB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create Organizations
        orgA = new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala");
        orgB = new OrganizationEntity("Clinique B", "contactb@joprelys.local", "456", "Street B", "Yaoundé");
        orgA = organizationRepository.save(orgA);
        orgB = organizationRepository.save(orgB);

        // Create Users
        userMedecinA = new UserAccountEntity("medecin.a@joprelys.local", "Médecin A", "MEDECIN", "passhash");
        userMedecinA.setOrganizationId(orgA.getId());
        userMedecinA = userAccountRepository.save(userMedecinA);

        userPharmacienA = new UserAccountEntity("pharmacien.a@joprelys.local", "Pharmacien A", "PHARMACIEN", "passhash");
        userPharmacienA.setOrganizationId(orgA.getId());
        userPharmacienA = userAccountRepository.save(userPharmacienA);

        userAdminJoprelys = new UserAccountEntity("admin.jop@joprelys.local", "Admin Joprelys", "ADMIN_JOPRELYS", "passhash");
        userAdminJoprelys = userAccountRepository.save(userAdminJoprelys);

        // Generate Tokens
        tokenMedecinA = jwtService.createToken(userMedecinA).value();
        tokenPharmacienA = jwtService.createToken(userPharmacienA).value();
        tokenAdminJoprelys = jwtService.createToken(userAdminJoprelys).value();

        // Create Patients
        TenantContext.setTenantId(orgA.getId());
        patientA = new PatientEntity("DPU-A", "PAT-A", "Patient A", "MASCULIN", LocalDate.of(1990, 1, 1), "+123", "Douala", "", "", "", "", "", "");
        patientA = patientRepository.save(patientA);

        TenantContext.setTenantId(orgB.getId());
        patientB = new PatientEntity("DPU-B", "PAT-B", "Patient B", "MASCULIN", LocalDate.of(1992, 2, 2), "+456", "Yaounde", "", "", "", "", "", "");
        patientB = patientRepository.save(patientB);

        TenantContext.clear();
    }

    @Test
    void givenMedecin_whenCloseVisit_thenDocumentGeneratedAndSaved() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        // 1. Create a visit
        VisitEntity visit = new VisitEntity(patientA, "VIS-001", "Fièvre", "Orientation");
        visit = visitRepository.save(visit);

        // 2. Create a consultation
        ConsultationEntity consultation = new ConsultationEntity(
                visit, userMedecinA, "CONS-001", "Fièvre élevée", "Examen clinique normal", "Grippe suspectée", "Repos", "Contrôle dans 3 jours"
        );
        consultation = consultationRepository.save(consultation);

        // 3. Create a prescription
        PrescriptionEntity prescription = new PrescriptionEntity(consultation);
        prescription = prescriptionRepository.save(prescription);

        PrescriptionItemEntity item = new PrescriptionItemEntity(
                prescription, "Paracétamol", "1000mg", "1 comprimé 3 fois par jour", "5 jours", "1 boite", "Prendre pendant les repas", 1
        );
        prescription.getItems().add(item);
        prescriptionRepository.save(prescription);
        TenantContext.clear();

        // 4. Close the visit via endpoint
        mockMvc.perform(post("/api/visits/" + visit.getId() + "/close")
                .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk());

        // 5. Assert the medical document is created
        TenantContext.setTenantId(orgA.getId());
        var docOpt = medicalDocumentRepository.findByVisitId(visit.getId());
        assertTrue(docOpt.isPresent());
        MedicalDocumentEntity doc = docOpt.get();
        assertNotNull(doc.getFilePath());
        assertTrue(doc.getDocumentNumber().startsWith("DOC-"));

        // Check physical file exists
        File file = new File(doc.getFilePath());
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        // Cleanup physical file
        if (file.exists()) {
            file.delete();
        }
        TenantContext.clear();
    }

    @Test
    void doctorCanUploadSaveAndRenderSignatureAsPng() throws Exception {
        var image = new java.awt.image.BufferedImage(17, 7, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var encoded = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", encoded);
        String upload = mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "signature.jpg", "image/jpeg", encoded.toByteArray()))
                        .param("type", "signature").header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String path = com.jayway.jsonpath.JsonPath.read(upload, "$.filePath");
        assertTrue(path.startsWith("uploads/signature/"));
        assertTrue(path.endsWith(".png"));
        byte[] stored = mockMvc.perform(get("/api/public/files/view").param("path", path))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(17, javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(stored)).getWidth());
        mockMvc.perform(put("/api/profile").header("Authorization", "Bearer " + tokenMedecinA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Dr. Alpha\",\"registrationNumber\":\"ORD-TEST\",\"signaturePath\":\"" + path + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.signaturePath").value(path));
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.signaturePath").value(path));
        TenantContext.setTenantId(orgA.getId());
        VisitEntity visit;
        try {
            visit = visitRepository.save(new VisitEntity(patientA, "VIS-SIGN-PNG", "Consultation", "Médecine"));
            consultationRepository.save(new ConsultationEntity(visit, userMedecinA, "CONS-SIGN-PNG",
                    "Observation", "Examen", "Diagnostic", "Conclusion", "Conseils"));
        } finally { TenantContext.clear(); }
        mockMvc.perform(post("/api/visits/" + visit.getId() + "/close")
                .header("Authorization", "Bearer " + tokenMedecinA)).andExpect(status().isOk());
        byte[] pdf = mockMvc.perform(get("/api/visits/" + visit.getId() + "/document")
                        .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertTrue(hasImage(pdf, 17, 7), "La signature téléversée doit être intégrée au PDF médical.");
    }

    @Test
    void nonDoctorCannotAttachMedicalSignatureToOwnProfile() throws Exception {
        mockMvc.perform(put("/api/profile").header("Authorization", "Bearer " + tokenPharmacienA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Pharmacien Alpha\",\"signaturePath\":\"uploads/signature/test.png\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanUploadPhysicianSignatureAssetsWithoutMedicalSigningRights() throws Exception {
        var image = new java.awt.image.BufferedImage(17, 7, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var encoded = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", encoded);
        mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "signature.png", "image/png", encoded.toByteArray()))
                        .param("type", "signature").header("Authorization", "Bearer " + tokenAdminJoprelys))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filePath").value(org.hamcrest.Matchers.endsWith(".png")));
    }

    @Test
    void signatureHeaderAloneIsNotAcceptedAsAnImage() throws Exception {
        mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "signature.png", "image/png",
                                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10, 0, 0, 0, 0}))
                        .param("type", "signature").header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isBadRequest());
    }

    private boolean hasImage(byte[] pdf, int width, int height) throws java.io.IOException {
        var reader = new com.lowagie.text.pdf.PdfReader(pdf);
        try {
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                var resources = reader.getPageN(page).getAsDict(com.lowagie.text.pdf.PdfName.RESOURCES);
                var objects = resources.getAsDict(com.lowagie.text.pdf.PdfName.XOBJECT);
                if (objects == null) continue;
                for (var key : objects.getKeys()) {
                    var object = com.lowagie.text.pdf.PdfReader.getPdfObject(objects.get(key));
                    if (object instanceof com.lowagie.text.pdf.PdfDictionary dictionary
                            && com.lowagie.text.pdf.PdfName.IMAGE.equals(dictionary.getAsName(com.lowagie.text.pdf.PdfName.SUBTYPE))
                            && dictionary.getAsNumber(com.lowagie.text.pdf.PdfName.WIDTH).intValue() == width
                            && dictionary.getAsNumber(com.lowagie.text.pdf.PdfName.HEIGHT).intValue() == height) return true;
                }
            }
            return false;
        } finally { reader.close(); }
    }

    @Test
    void givenValidDocument_whenVerifyAnonymously_thenSuccess() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        VisitEntity visit = new VisitEntity(patientA, "VIS-001", "Fièvre", "Orientation");
        visit = visitRepository.save(visit);

        ConsultationEntity consultation = new ConsultationEntity(
                visit, userMedecinA, "CONS-001", "Fièvre élevée", "Examen clinique normal", "Grippe suspectée", "Repos", "Contrôle"
        );
        consultationRepository.save(consultation);

        MedicalDocumentEntity doc = new MedicalDocumentEntity(visit, "DOC-20260702-000001", "some/path/doc.pdf");
        doc = medicalDocumentRepository.save(doc);
        TenantContext.clear();

        // Verify without authentication
        mockMvc.perform(get("/api/public/documents/" + doc.getId() + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("DOC-20260702-000001"))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.clinicName").value("Clinique A"))
                .andExpect(jsonPath("$.doctorName").value("Médecin A"))
                .andExpect(jsonPath("$.patientName").value("Patient A"))
                .andExpect(jsonPath("$.symptoms").doesNotExist()) // Verify medical details are hidden
                .andExpect(jsonPath("$.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.prescription").doesNotExist());
    }

    @Test
    void givenValidVisit_whenDownloadDocument_thenSuccess() throws Exception {
        TenantContext.setTenantId(orgA.getId());
        VisitEntity visit = new VisitEntity(patientA, "VIS-001", "Fièvre", "Orientation");
        visit = visitRepository.save(visit);

        // Write a temp file to satisfy service reading
        File tempFile = File.createTempFile("joprelys-test", ".pdf");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), "Mock PDF content");

        MedicalDocumentEntity doc = new MedicalDocumentEntity(visit, "DOC-20260702-000002", tempFile.getAbsolutePath());
        medicalDocumentRepository.save(doc);
        TenantContext.clear();

        // Download with Medecin role
        mockMvc.perform(get("/api/visits/" + visit.getId() + "/document")
                .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertTrue(content.contains("Mock PDF content"));
                });

        // Download with Pharmacien role
        mockMvc.perform(get("/api/visits/" + visit.getId() + "/document")
                .header("Authorization", "Bearer " + tokenPharmacienA))
                .andExpect(status().isOk());
    }

    @Test
    void givenNoToken_whenDownloadDocument_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/visits/" + UUID.randomUUID() + "/document"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenWrongRole_whenDownloadDocument_thenForbidden() throws Exception {
        // Admin Joprelys is a system admin role and does not have access to patient documents
        mockMvc.perform(get("/api/visits/" + UUID.randomUUID() + "/document")
                .header("Authorization", "Bearer " + tokenAdminJoprelys))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenCrossTenant_whenDownloadDocument_thenNotFound() throws Exception {
        // Create visit in Tenant B
        TenantContext.setTenantId(orgB.getId());
        VisitEntity visitB = new VisitEntity(patientB, "VIS-B", "Motif B", "Orientation");
        visitB = visitRepository.save(visitB);

        File tempFile = File.createTempFile("joprelys-test-b", ".pdf");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), "Mock PDF content B");

        MedicalDocumentEntity docB = new MedicalDocumentEntity(visitB, "DOC-20260702-000003", tempFile.getAbsolutePath());
        medicalDocumentRepository.save(docB);
        TenantContext.clear();

        // Medecin A (from Tenant A) tries to download Tenant B's document
        mockMvc.perform(get("/api/visits/" + visitB.getId() + "/document")
                .header("Authorization", "Bearer " + tokenMedecinA))
                .andExpect(status().isNotFound());
    }
}
