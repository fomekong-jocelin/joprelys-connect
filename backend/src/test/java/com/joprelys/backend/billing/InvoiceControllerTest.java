package com.joprelys.backend.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.billing.api.*;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.*;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.joprelys.backend.cash.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
public class InvoiceControllerTest {

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
    private HospitalizationRepository hospitalizationRepository;

    @Autowired
    private InsuranceConventionRepository insuranceConventionRepository;

    @Autowired
    private TariffGridRepository tariffGridRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private CashRegisterSessionRepository cashRegisterSessionRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity org;
    private UserAccountEntity receptionist;
    private UserAccountEntity admin;
    private PatientEntity patient;
    private VisitEntity visit;
    private ConsultationEntity consultation;
    private PrescriptionEntity prescription;
    private HospitalizationEntity hospitalization;
    private InsuranceConventionEntity convention;

    private String tokenReceptionist;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM cash_register_sessions");
        jdbcTemplate.update("DELETE FROM cash_registers");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM insurance_conventions");
        jdbcTemplate.update("DELETE FROM tariff_grid");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create Org
        org = new OrganizationEntity("Clinique de Facturation", "billing@joprelys.local", "999999", "Avenue Foch", "Yaounde");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // Receptionist (AGENT_ACCUEIL)
        receptionist = new UserAccountEntity("reception@joprelys.local", "Julie Reception", "AGENT_ACCUEIL", "passhash");
        receptionist.setOrganizationId(org.getId());
        receptionist = userAccountRepository.save(receptionist);
        tokenReceptionist = jwtService.createToken(receptionist).value();

        // Admin (ADMIN_CLINIQUE)
        admin = new UserAccountEntity("admin@joprelys.local", "Admin Clinic", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        // Create Patient
        patient = new PatientEntity("DPU-TEST-001", "PAT-TEST-001", "Marc Dupont", "MASCULIN", LocalDate.of(1985, 5, 20), "670000000", "Yaounde", "Bastos", "Street X", "Julie", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        // Create Visit
        visit = new VisitEntity(patient, "VIS-TEST-001", "Paludisme aigu", "Général", "MÉDECINE GÉNÉRALE", admin.getId(), Instant.now());
        visit = visitRepository.save(visit);

        // Create Consultation
        consultation = new ConsultationEntity(visit, admin, "DOC-2026-0001", "Fievre, Courbatures", "39°C", "Paludisme suspecte", "Paludisme", "Paludisme severe", "Conclusion", "Prendre du repos", "Suivi 3j");
        consultation.setOrganizationId(org.getId());
        consultation = consultationRepository.save(consultation);

        // Create Prescription
        prescription = new PrescriptionEntity(consultation);
        prescription.setPrescriptionNumber("ORD-2026-0001");
        prescription.setIssuedAt(Instant.now());
        prescription.setExpiresAt(Instant.now().plusSeconds(86400 * 7));
        prescription.setPinCode("1234");
        prescription = prescriptionRepository.save(prescription);

        PrescriptionItemEntity pItem = new PrescriptionItemEntity(prescription, "Artesunate", "60mg", "1 injection/jour", "3 jours", "3", "Injection intraveineuse", 0);
        prescription.getItems().add(pItem);
        prescriptionRepository.save(prescription);

        // Create Hospitalization
        hospitalization = new HospitalizationEntity(
                patient.getId(),
                "Médecine Hommes",
                "A101",
                "Bed-1",
                "Motif chirurgie",
                "HOSP-TEST-999",
                visit.getId(),
                admin.getId()
        );
        hospitalization = hospitalizationRepository.save(hospitalization);

        // Setup Convention
        convention = new InsuranceConventionEntity("AXA Cameroun", 0.80); // 80% coverage
        convention.setOrganizationId(org.getId());
        convention = insuranceConventionRepository.save(convention);
    }

    @Test
    void testConfigureConventionsAndTariffs() throws Exception {
        // 1. Create Tariff Grid as Admin
        mockMvc.perform(post("/api/invoices/tariffs")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("keyLetter", "CS")
                        .param("unitValue", "12000")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyLetter").value("CS"))
                .andExpect(jsonPath("$.unitValue").value(12000.0));

        // 2. Read Tariffs as Receptionist
        mockMvc.perform(get("/api/invoices/tariffs")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyLetter").value("CS"));

        // 3. Create Convention as Admin
        mockMvc.perform(post("/api/invoices/conventions")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("name", "Gras Savoye")
                        .param("coveragePercentage", "0.75")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gras Savoye"))
                .andExpect(jsonPath("$.coveragePercentage").value(0.75));
    }

    @Test
    void testPrecalculateAndCreateInvoiceWithPayments() throws Exception {
        // Pre-configure ROOM_STANDARD and Artesunate prices
        TariffGridEntity tRoom = new TariffGridEntity("ROOM_STANDARD", 10000.0);
        tRoom.setOrganizationId(org.getId());
        tariffGridRepository.save(tRoom);

        TariffGridEntity tDrug = new TariffGridEntity("Artesunate", 3000.0);
        tDrug.setOrganizationId(org.getId());
        tariffGridRepository.save(tDrug);

        // 1. Precalculate
        mockMvc.perform(post("/api/invoices/precalculate")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .param("patientId", patient.getId().toString())
                        .param("visitId", visit.getId().toString())
                        .param("insuranceConventionId", convention.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(34000.0)) // CS (15000) + Stay 1 night (10000) + Artesunate 3 * 3000 (9000) = 34000
                .andExpect(jsonPath("$.insuranceShare").value(27200.0)) // 80% of 34000
                .andExpect(jsonPath("$.patientShare").value(6800.0)); // 20% of 34000

        // 2. Create Invoice
        CreateInvoiceRequest req = new CreateInvoiceRequest(
                patient.getId(),
                visit.getId(),
                convention.getId(),
                null // let backend auto precalculate
        );

        String responseStr = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(34000.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        InvoiceResponse created = objectMapper.readValue(responseStr, InvoiceResponse.class);

        // 3. Make partial payment
        TenantContext.setTenantId(org.getId());
        CashRegisterEntity register = new CashRegisterEntity("CAISSE-PRINCIPALE", "Caisse Principale");
        register.setOrganizationId(org.getId());
        register = cashRegisterRepository.save(register);

        CashRegisterSessionEntity session = new CashRegisterSessionEntity(register, receptionist.getId(), 50000.0);
        session.setOrganizationId(org.getId());
        cashRegisterSessionRepository.save(session);
        TenantContext.clear();

        PaymentRequest payReq = new PaymentRequest(3000.0, PaymentMethod.CASH, "REF-1111");
        mockMvc.perform(post("/api/invoices/" + created.id() + "/payments")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(3000.0));

        // Invoice status should now be PARTIALLY_PAID
        mockMvc.perform(get("/api/invoices/" + created.id())
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

        // 4. Make final payment
        PaymentRequest payFinal = new PaymentRequest(3800.0, PaymentMethod.CASH, "REF-2222");
        mockMvc.perform(post("/api/invoices/" + created.id() + "/payments")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payFinal)))
                .andExpect(status().isOk());

        // Invoice status should now be PAID
        mockMvc.perform(get("/api/invoices/" + created.id())
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // 5. Download certified PDF
        mockMvc.perform(get("/api/invoices/" + created.id() + "/pdf")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.contains("pdf");
                });
    }
}
