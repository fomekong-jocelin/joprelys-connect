package com.joprelys.backend.billing;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EstimateControllerTest {

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
    private EstimateRepository estimateRepository;

    @Autowired
    private CreditNoteRepository creditNoteRepository;

    @Autowired
    private ReceivableRepository receivableRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity org;
    private UserAccountEntity admin;
    private UserAccountEntity receptionist;
    private PatientEntity patient;
    private VisitEntity visit;
    private InvoiceEntity invoice;

    private String tokenAdmin;
    private String tokenReceptionist;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM receivables");
        jdbcTemplate.update("DELETE FROM credit_notes");
        jdbcTemplate.update("DELETE FROM estimate_items");
        jdbcTemplate.update("DELETE FROM estimates");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
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

        // Receptionist
        receptionist = new UserAccountEntity("reception@joprelys.local", "Reception Test", "AGENT_ACCUEIL", "passhash");
        receptionist.setOrganizationId(org.getId());
        receptionist = userAccountRepository.save(receptionist);
        tokenReceptionist = jwtService.createToken(receptionist).value();

        // Patient
        patient = new PatientEntity("DPU-EST-01", "PAT-EST-01", "Marie Dupont", "FEMININ", LocalDate.of(1990, 8, 12), "670000001", "Douala", "Akwa", "Street Y", "Jean", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        // Visit
        visit = new VisitEntity(patient, "VIS-EST-01", "Consultation générale", "Général", "MÉDECINE GÉNÉRALE", admin.getId(), Instant.now());
        visit = visitRepository.save(visit);

        // Invoice PENDING
        invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-TEST-001", null);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity("Prestation", InvoiceItemType.CONSULTATION, 25000.0, 1.0, null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice = invoiceRepository.save(invoice);
    }

    @Test
    void testCreateAndManageEstimate() throws Exception {
        // 1. Create Estimate
        CreateEstimateRequest req = new CreateEstimateRequest(
                patient.getId(),
                visit.getId(),
                List.of(
                        new EstimateItemRequest("Consultation", "CONSULTATION", 15000.0, 1.0),
                        new EstimateItemRequest("Pansement", "STAY_FEE", 10000.0, 1.0)
                )
        );

        String responseStr = mockMvc.perform(post("/api/estimates")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estimateNumber").value(startsWith("DEV-")))
                .andExpect(jsonPath("$.totalAmount").value(25000.0))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        EstimateResponse created = objectMapper.readValue(responseStr, EstimateResponse.class);

        // 2. Change status
        mockMvc.perform(patch("/api/estimates/" + created.id() + "/status")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void testValidateDiscountAndCancelInvoice() throws Exception {
        // 1. Apply Discount
        ApplyDiscountRequest discountReq = new ApplyDiscountRequest(5000.0, "Remise promotionnelle");
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/discount")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(discountReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(5000.0))
                .andExpect(jsonPath("$.discountReason").value("Remise promotionnelle"));

        // 2. Validate Invoice as Admin
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/validate")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.validatedByUserId").value(admin.getId().toString()));

        // 3. Verify Receivables generated
        mockMvc.perform(get("/api/invoices/" + invoice.getId() + "/receivables")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debtorId").value(patient.getId().toString()))
                .andExpect(jsonPath("$[0].totalAmount").value(20000.0));

        // 4. Create Credit Note
        CreateCreditNoteRequest cnReq = new CreateCreditNoteRequest(10000.0, "Ajustement erreur");
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/credit-notes")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cnReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditNoteNumber").value(startsWith("AV-")))
                .andExpect(jsonPath("$.amount").value(10000.0));
    }
}
