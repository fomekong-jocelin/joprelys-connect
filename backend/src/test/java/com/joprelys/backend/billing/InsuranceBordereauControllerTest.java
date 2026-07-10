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

import java.math.BigDecimal;
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

        org = new OrganizationEntity("Clinique de Test", "test@joprelys.local", "999999", "Rue Test", "Douala");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        admin = new UserAccountEntity("admin@joprelys.local", "Admin Test", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        convention = new InsuranceConventionEntity("SAAR Assurance", new BigDecimal("0.8000"));
        convention.setOrganizationId(org.getId());
        convention = insuranceConventionRepository.save(convention);

        patient = new PatientEntity("DPU-EST-01", "PAT-EST-01", "Marie Dupont", "FEMININ", LocalDate.of(1990, 8, 12), "670000001", "Douala", "Akwa", "Street Y", "Jean", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        visit = new VisitEntity(patient, "VIS-EST-01", "Consultation générale", "Général", "MÉDECINE GÉNÉRALE", admin.getId(), Instant.now());
        visit = visitRepository.save(visit);

        invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-TEST-001", convention);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Prestation",
                InvoiceItemType.CONSULTATION,
                new BigDecimal("25000.0000"),
                new BigDecimal("1.0000"),
                null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice.setStatus(InvoiceStatus.VALIDATED);
        invoice.setValidatedAt(Instant.now());
        invoice.setValidatedByUserId(admin.getId());
        invoice = invoiceRepository.save(invoice);

        ReceivableEntity rec = new ReceivableEntity(invoice.getId(), "INSURANCE", convention.getId(), invoice.getInsuranceShare());
        rec.setOrganizationId(org.getId());
        receivableRepository.save(rec);
    }

    @Test
    void testCompleteBordereauWorkflowWithPartialPayments() throws Exception {
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
                .andExpect(jsonPath("$.totalAmount").value(20000.0))
                .andExpect(jsonPath("$.paidAmount").value(0.0))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        InsuranceBordereauController.BordereauResponse genResponse = objectMapper.readValue(
                genResult, InsuranceBordereauController.BordereauResponse.class);
        UUID bordereauId = genResponse.id();

        mockMvc.perform(get("/api/billing/insurance-bordereaux/" + bordereauId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices", hasSize(1)))
                .andExpect(jsonPath("$.invoices[0].invoiceNumber").value("FAC-TEST-001"));

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/send")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/receive")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"insurerReference\":\"AR-SAAR-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.insurerReference").value("AR-SAAR-001"));

        InsuranceBordereauController.AcceptBordereauRequest acceptReq =
                new InsuranceBordereauController.AcceptBordereauRequest(new BigDecimal("20000.0000"), "AR-SAAR-001");
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/accept")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAmount").value(20000.0));

        InsuranceBordereauController.BordereauPaymentRequest firstPayment =
                new InsuranceBordereauController.BordereauPaymentRequest(new BigDecimal("8000.0000"), "VIR-12345-A");
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/pay")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.paidAmount").value(8000.0))
                .andExpect(jsonPath("$.remainingAmount").value(12000.0));

        InsuranceBordereauController.BordereauPaymentRequest secondPayment =
                new InsuranceBordereauController.BordereauPaymentRequest(new BigDecimal("12000.0000"), "VIR-12345-B");
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereauId + "/pay")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondPayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"))
                .andExpect(jsonPath("$.paidAmount").value(20000.0))
                .andExpect(jsonPath("$.remainingAmount").value(0.0));

        TenantContext.setTenantId(org.getId());
        List<ReceivableEntity> recs = receivableRepository.findAll().stream()
                .filter(r -> invoice.getId().equals(r.getInvoiceId()) && "INSURANCE".equalsIgnoreCase(r.getDebtorType()))
                .toList();
        assertFalse(recs.isEmpty());
        assertEquals("PAID", recs.get(0).getStatus());
        assertEquals(new BigDecimal("20000.0000"), recs.get(0).getPaidAmount());

        InvoiceEntity synchronizedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.VALIDATED, synchronizedInvoice.getStatus());
        List<ReceivableEntity> patientReceivables = receivableRepository
                .findByInvoiceIdAndDebtorTypeIgnoreCase(invoice.getId(), "PATIENT");
        assertEquals(1, patientReceivables.size());
        assertEquals("UNPAID", patientReceivables.get(0).getStatus());
    }

    @Test
    void testRejectsInvalidTransitionAndExcessPayment() throws Exception {
        LocalDate today = LocalDate.now();
        InsuranceBordereauController.GenerateBordereauRequest genReq = new InsuranceBordereauController.GenerateBordereauRequest(
                convention.getId(), today.minusDays(5), today.plusDays(5));

        String result = mockMvc.perform(post("/api/billing/insurance-bordereaux")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = objectMapper.readValue(result, InsuranceBordereauController.BordereauResponse.class).id();

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/accept")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedAmount\":20000}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/send")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/receive")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"insurerReference\":\"AR-002\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/accept")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedAmount\":20000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/pay")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20001,\"referenceNumber\":\"OVERPAY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenerateBordereauFailsIfNoInvoices() throws Exception {
        LocalDate today = LocalDate.now();
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
