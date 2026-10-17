package com.joprelys.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridEntity;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Facturation par sources, avoirs approuvés, remboursement, acomptes et paiement avant prélèvement (ADR-20261017 D-11 à D-17). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingJourneyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private TariffGridRepository tariffGridRepository;
    @Autowired private LabOrderRepository labOrderRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private OrganizationEntity org;
    private PatientEntity patient;
    private VisitEntity visit;
    private String secretary;
    private String daf;
    private String cashier;
    private String doctor;
    private String admin;

    @BeforeEach
    void setUp() {
        clean();
        org = organizationRepository.save(new OrganizationEntity("Clinique Facturation", "fact@joprelys.local", "1", "Rue", "Douala"));
        TenantContext.setTenantId(org.getId());
        try {
            secretary = token("secretaire@joprelys.local", "SECRETAIRE_COMPTABLE");
            daf = token("daf@joprelys.local", "DAF");
            cashier = token("caissier@joprelys.local", "CAISSIER");
            admin = token("admin.fact@joprelys.local", "ADMIN_CLINIQUE");
            UserAccountEntity physician = user("medecin.fact@joprelys.local", "MEDECIN");
            doctor = jwtService.createToken(physician).value();
            patient = patientRepository.save(new PatientEntity("DPU-F-1", "PAT-F-1", "Patient Facturation", "MASCULIN",
                    LocalDate.of(1980, 1, 1), "+237600000009", "Douala", "", "", "", "", "", ""));
            visit = visitRepository.save(new VisitEntity(patient, "VIS-F-1", "Fièvre", "CONSULTATION", "Médecine générale",
                    physician.getId(), Instant.now()));
            ConsultationEntity consultation = new ConsultationEntity(visit, physician, "CONS-F-1", "Fièvre", "T 39",
                    "Paludisme", "Traitement", "Repos", "J3");
            consultation.setOrganizationId(org.getId());
            consultationRepository.save(consultation);
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    @Test
    void aMissingTariffBlocksValidationUntilTheInvoiceIsRecalculated() throws Exception {
        String invoiceId = createInvoice(secretary);
        mockMvc.perform(get("/api/invoices/{id}", invoiceId).header("Authorization", bearer(secretary)))
                .andExpect(jsonPath("$.items[0].tariffMissing").value(true))
                .andExpect(jsonPath("$.totalAmount").value(0.0));
        mockMvc.perform(post("/api/invoices/{id}/validate", invoiceId).header("Authorization", bearer(secretary)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("CS")));

        tariff("CS", "12000");
        mockMvc.perform(post("/api/invoices/{id}/recalculate", invoiceId).header("Authorization", bearer(secretary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(12000.0))
                .andExpect(jsonPath("$.items[0].tariffMissing").value(false));
        mockMvc.perform(post("/api/invoices/{id}/validate", invoiceId).header("Authorization", bearer(secretary)))
                .andExpect(status().isOk());
    }

    @Test
    void aSupplementaryInvoiceBillsOnlyChargesAddedSinceTheFirstOne() throws Exception {
        tariff("CS", "12000");
        tariff("EXAM:NFS", "6000");
        createInvoice(secretary);
        requestBloodCount();

        String second = createInvoice(secretary);
        mockMvc.perform(get("/api/invoices/{id}", second).header("Authorization", bearer(secretary)))
                .andExpect(jsonPath("$.totalAmount").value(6000.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemType").value("LAB_EXAM"));
        mockMvc.perform(post("/api/invoices").header("Authorization", bearer(secretary))
                        .contentType(MediaType.APPLICATION_JSON).content(invoicePayload()))
                .andExpect(status().isConflict());
    }

    @Test
    void largeCreditNotesNeedApprovalByAnotherUserAndRefundsAreBoundedByTheCreditNote() throws Exception {
        tariff("CS", "120000");
        String invoiceId = validatedInvoice();
        openCashSession(cashier);
        pay(invoiceId, "120000");

        String pending = issueCreditNote(secretary, invoiceId, 60000);
        mockMvc.perform(get("/api/invoices/{id}/credit-notes", invoiceId).header("Authorization", bearer(secretary)))
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"));
        mockMvc.perform(post("/api/credit-notes/{id}/approve", pending).header("Authorization", bearer(secretary)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/credit-notes/{id}/approve", pending).header("Authorization", bearer(daf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String ownCredit = issueCreditNote(daf, invoiceId, 55000);
        mockMvc.perform(post("/api/credit-notes/{id}/approve", ownCredit).header("Authorization", bearer(daf)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/credit-notes/{id}/refund", pending).header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":70000,\"method\":\"CASH\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/credit-notes/{id}/refund", pending).header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":60000,\"method\":\"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundedAmount").value(60000.0));
    }

    @Test
    void aDepositIsCountedOnceInCashAndAppliedToTheInvoice() throws Exception {
        tariff("CS", "12000");
        String sessionId = openCashSession(cashier);
        String deposit = JsonPath.read(mockMvc.perform(post("/api/deposits").header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"visitId\":\"%s\",\"amount\":5000,\"method\":\"CASH\"}"
                                .formatted(patient.getId(), visit.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        String invoiceId = validatedInvoice();

        mockMvc.perform(post("/api/invoices/{id}/apply-deposit", invoiceId).header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"depositId\":\"%s\"}".formatted(deposit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(5000.0));
        mockMvc.perform(get("/api/deposits").param("patientId", patient.getId().toString()).header("Authorization", bearer(cashier)))
                .andExpect(jsonPath("$[0].remainingAmount").value(0.0));
        mockMvc.perform(get("/api/cash-registers/sessions/{id}/movements", sessionId).header("Authorization", bearer(cashier)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void outpatientExamsWaitForPaymentWhenTheEstablishmentRequiresIt() throws Exception {
        mockMvc.perform(put("/api/billing/settings").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"labPaymentBeforeSampling\":true,\"creditNoteApprovalThreshold\":50000}"))
                .andExpect(status().isOk());
        tariff("CS", "12000");
        tariff("EXAM:NFS", "6000");
        UUID orderId = requestBloodCount();
        TenantContext.setTenantId(org.getId());
        try {
            assertThat(labOrderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(LabOrderStatus.AWAITING_PAYMENT);
        } finally {
            TenantContext.clear();
        }

        String invoiceId = validatedInvoice();
        openCashSession(cashier);
        pay(invoiceId, "18000");

        TenantContext.setTenantId(org.getId());
        try {
            assertThat(labOrderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(LabOrderStatus.PAID);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID requestBloodCount() throws Exception {
        String body = mockMvc.perform(post("/api/lab-orders").header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"%s\",\"visitId\":\"%s\",\"examType\":\"LABORATOIRE\",\"exams\":[\"NFS\"],\"reason\":\"Fièvre\",\"priority\":\"NORMALE\"}"
                                .formatted(patient.getId(), visit.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private String validatedInvoice() throws Exception {
        String invoiceId = createInvoice(secretary);
        mockMvc.perform(post("/api/invoices/{id}/validate", invoiceId).header("Authorization", bearer(secretary)))
                .andExpect(status().isOk());
        return invoiceId;
    }

    private String createInvoice(String token) throws Exception {
        String body = mockMvc.perform(post("/api/invoices").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(invoicePayload()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String invoicePayload() {
        return "{\"patientId\":\"%s\",\"visitId\":\"%s\"}".formatted(patient.getId(), visit.getId());
    }

    private String openCashSession(String token) throws Exception {
        String body = mockMvc.perform(post("/api/cash-registers/sessions/open").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openingBalance\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void pay(String invoiceId, String amount) throws Exception {
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId).header("Authorization", bearer(cashier))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":%s,\"method\":\"CASH\"}".formatted(amount)))
                .andExpect(status().isOk());
    }

    private String issueCreditNote(String token, String invoiceId, int amount) throws Exception {
        String body = mockMvc.perform(post("/api/invoices/{id}/credit-notes", invoiceId).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%d,\"reason\":\"Acte facturé à tort\"}".formatted(amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void tariff(String key, String value) {
        TariffGridEntity tariff = new TariffGridEntity(key, new BigDecimal(value));
        tariff.setOrganizationId(org.getId());
        TenantContext.setTenantId(org.getId());
        try {
            tariffGridRepository.save(tariff);
        } finally {
            TenantContext.clear();
        }
    }

    private String token(String email, String role) {
        return jwtService.createToken(user(email, role)).value();
    }

    private UserAccountEntity user(String email, String role) {
        UserAccountEntity account = new UserAccountEntity(email, email.substring(0, email.indexOf('@')), role, "passhash");
        account.setOrganizationId(org.getId());
        return userAccountRepository.save(account);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private void clean() {
        TenantContext.clear();
        for (String table : new String[] {"payment_receipts", "cash_movements", "payments", "patient_deposits",
                "cash_register_sessions", "cash_registers", "credit_notes", "receivables", "invoice_items", "invoices",
                "tariff_grid", "lab_results", "lab_order_items", "lab_orders", "consultations", "visits", "audit_logs", "patients"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }
}
