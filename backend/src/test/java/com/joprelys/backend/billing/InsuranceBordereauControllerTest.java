package com.joprelys.backend.billing;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
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
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InsuranceBordereauControllerTest {

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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InsuranceConventionRepository insuranceConventionRepository;

    @Autowired
    private InsuranceBordereauRepository insuranceBordereauRepository;

    @Autowired
    private ReceivableRepository receivableRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private OrganizationEntity org;
    private UserAccountEntity admin;
    private InsuranceConventionEntity convention;
    private PatientEntity patient;
    private VisitEntity visit;
    private InvoiceEntity invoice;

    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM receivables");
        jdbcTemplate.update("DELETE FROM credit_notes");
        jdbcTemplate.update("DELETE FROM estimate_items");
        jdbcTemplate.update("DELETE FROM estimates");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM insurance_bordereaux");
        jdbcTemplate.update("DELETE FROM insurance_conventions");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create Org
        org = new OrganizationEntity("Clinique de Test", "test@joprelys.local", "999999", "Rue Test", "Douala");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // Admin
        admin = new UserAccountEntity("admin@joprelys.local", "Admin Test", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        // Insurance Convention
        convention = new InsuranceConventionEntity("SAAR Assurance", 0.8);
        convention.setOrganizationId(org.getId());
        convention = insuranceConventionRepository.save(convention);

        // Patient
        patient = new PatientEntity("DPU-EST-01", "PAT-EST-01", "Marie Dupont", "FEMININ", LocalDate.of(1990, 8, 12), "670000001", "Douala", "Akwa", "Street Y", "Jean", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        // Visit
        visit = new VisitEntity(patient, "VIS-EST-01", "Consultation générale", "Général", "MÉDECINE GÉNÉRALE", admin.getId(), Instant.now());
        visit = visitRepository.save(visit);

        // Invoice VALIDATED
        invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-TEST-001", convention);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity("Prestation", InvoiceItemType.CONSULTATION, 25000.0, 1.0, null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice.setStatus(InvoiceStatus.VALIDATED);
        invoice.setValidatedAt(Instant.now());
        invoice.setValidatedByUserId(admin.getId());
        invoice = invoiceRepository.save(invoice);

        // Save insurance receivable
        ReceivableEntity rec = new ReceivableEntity(invoice.getId(), "INSURANCE", convention.getId(), invoice.getInsuranceShare());
        rec.setOrganizationId(org.getId());
        receivableRepository.save(rec);
    }

    @Test
    void testCompleteBordereauWorkflow() throws Exception {
        // 1. Generate Bordereau
        LocalDate today = LocalDate.now();
        InsuranceBordereauController.GenerateBordereauRequest genReq = new InsuranceBordereauController.GenerateBordereauRequest(
                convention.getId(),
                today.minusDays(5),
                today.plusDays(5)
        );

        String genResult = mockMvc.perform(post("/api/billing/insurance-bordereaux")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bordereauNumber").exists())
                .andExpect(jsonPath("$.totalAmount").value(20000.0)) // 80% of 25000 is 20000
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        InsuranceBordereauController.BordereauResponse genResponse = objectMapper.readValue(genResult, InsuranceBordereauController.BordereauResponse.class);
        UUID bordereauId = genResponse.id();

        // 2. Fetch Details
        mockMvc.perform(get("/api/billing/insurance-bordereaux/" + bordereauId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices", hasSize(1)))
                .andExpect(jsonPath("$.invoices[0].invoiceNumber").value("FAC-TEST-001"));

        // 3. Mark As Sent
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/send")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        // 4. Record Payment
        InsuranceBordereauController.BordereauPaymentRequest payReq = new InsuranceBordereauController.BordereauPaymentRequest(
                20000.0,
                "VIR-12345"
        );

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/pay")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // 5. Verify that the associated receivable has been set to PAID
        TenantContext.setTenantId(org.getId());
        List<ReceivableEntity> recs = receivableRepository.findAll().stream()
                .filter(r -> invoice.getId().equals(r.getInvoiceId()) && "INSURANCE".equalsIgnoreCase(r.getDebtorType()))
                .toList();
        assertFalse(recs.isEmpty());
        assertEquals("PAID", recs.get(0).getStatus());
        assertEquals(20000.0, recs.get(0).getPaidAmount());
    }

    @Test
    void testGenerateBordereauFailsIfNoInvoices() throws Exception {
        LocalDate today = LocalDate.now();
        // SAAR has the invoice, but let's query for a different period (past) where there are no invoices
        InsuranceBordereauController.GenerateBordereauRequest genReq = new InsuranceBordereauController.GenerateBordereauRequest(
                convention.getId(),
                today.minusDays(20),
                today.minusDays(10)
        );

        mockMvc.perform(post("/api/billing/insurance-bordereaux")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isBadRequest());
    }
}
