package com.joprelys.backend.cash;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.billing.api.*;
import com.joprelys.backend.cash.api.*;
import com.joprelys.backend.cash.infrastructure.persistence.*;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CashRegisterControllerTest {

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
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private CashRegisterSessionRepository cashRegisterSessionRepository;

    @Autowired
    private CashMovementRepository cashMovementRepository;

    @Autowired
    private PaymentReceiptRepository paymentReceiptRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InsuranceConventionRepository insuranceConventionRepository;

    @Autowired
    private ReceivableRepository receivableRepository;

    @Autowired
    private InsuranceBordereauRepository insuranceBordereauRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity org;
    private UserAccountEntity caissier;
    private PatientEntity patient;
    private VisitEntity visit;
    private InvoiceEntity invoice;

    private String tokenCaissier;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM payment_receipts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM cash_register_sessions");
        jdbcTemplate.update("DELETE FROM cash_registers");
        jdbcTemplate.update("DELETE FROM receivables");
        jdbcTemplate.update("DELETE FROM insurance_bordereaux");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM insurance_conventions");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        org = new OrganizationEntity("Clinique Caisse", "caisse@joprelys.local", "999999", "Avenue Caisse", "Yaounde");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        caissier = new UserAccountEntity("caissier@joprelys.local", "Pierre Caisse", "CAISSIER", "passhash");
        caissier.setOrganizationId(org.getId());
        caissier = userAccountRepository.save(caissier);
        tokenCaissier = jwtService.createToken(caissier).value();

        UserAccountEntity admin = new UserAccountEntity("admin@joprelys.local", "Admin Caisse", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        patient = new PatientEntity("DPU-CS-01", "PAT-CS-01", "Antoine Caisse", "MASCULIN", LocalDate.of(1992, 10, 5), "670000009", "Yaounde", "Bastos", "Street Z", "Luc", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        visit = new VisitEntity(patient, "VIS-CS-01", "Consultation tri", "Général", "MÉDECINE GÉNÉRALE", caissier.getId(), Instant.now());
        visit = visitRepository.save(visit);

        invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-CS-001", null);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Acte",
                InvoiceItemType.CONSULTATION,
                new BigDecimal("15000.0000"),
                new BigDecimal("1.0000"),
                null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice = invoiceRepository.save(invoice);
    }

    @Test
    void testOpenCloseSessionAndMovements() throws Exception {
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/validate")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        PaymentRequest payReq = new PaymentRequest(
                new BigDecimal("15000.0000"),
                PaymentMethod.CASH,
                "REF-PAY");
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict());

        OpenSessionRequest openReq = new OpenSessionRequest(null, 50000.0);
        String responseStr = mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.openingBalance").value(50000.0))
                .andReturn().getResponse().getContentAsString();

        CashSessionResponse session = objectMapper.readValue(responseStr, CashSessionResponse.class);

        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isConflict());

        CashMovementRequest invalidOut = new CashMovementRequest("OUT", 120000.0, "Achat bureau", "CASH", "REF-OUT", false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOut)))
                .andExpect(status().isBadRequest());

        CashMovementRequest validOut = new CashMovementRequest("OUT", 120000.0, "Achat bureau", "CASH", "REF-OUT", true);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOut)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(120000.0));

        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        TenantContext.setTenantId(org.getId());
        PaymentEntity createdPayment = paymentRepository.findByInvoiceId(invoice.getId()).get(0);
        mockMvc.perform(get("/api/cash-registers/payments/" + createdPayment.getId() + "/receipt")
                        .header("Authorization", "Bearer " + tokenCaissier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value(startsWith("REC-")));

        CloseSessionRequest closeInvalid = new CloseSessionRequest(-50000.0, "");
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeInvalid)))
                .andExpect(status().isBadRequest());

        CloseSessionRequest closeValid = new CloseSessionRequest(-50000.0, "Ecart de test justifié");
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeValid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.discrepancyAmount").value(5000.0))
                .andExpect(jsonPath("$.discrepancyReason").value("Ecart de test justifié"));
    }

    @Test
    void shouldExcludeChequesAndTransfersFromExpectedPhysicalCash() throws Exception {
        OpenSessionRequest openRequest = new OpenSessionRequest(null, 10000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openRequest)))
                .andExpect(status().isCreated());

        addMovement("IN", 6000.0, "Règlement espèces", "CASH", null);
        addMovement("IN", 10000.0, "Règlement chèque", "CHECK", "CHQ-100");
        addMovement("IN", 5000.0, "Règlement virement", "BANK_TRANSFER", "VIR-100");
        addMovement("OUT", 1000.0, "Petite dépense", "CASH", "PIECE-100");
        addMovement("TRANSFER_TO_BANK", 3000.0, "Dépôt journée", "CASH", "BORD-100");

        mockMvc.perform(get("/api/cash-registers/sessions/active/summary")
                        .header("Authorization", "Bearer " + tokenCaissier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashReceipts").value(6000.0))
                .andExpect(jsonPath("$.chequeReceipts").value(10000.0))
                .andExpect(jsonPath("$.transferReceipts").value(5000.0))
                .andExpect(jsonPath("$.cashExpenses").value(1000.0))
                .andExpect(jsonPath("$.bankDeposits").value(3000.0))
                .andExpect(jsonPath("$.expectedCash").value(12000.0));

        CloseSessionRequest closeRequest = new CloseSessionRequest(12000.0, null);
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closingBalance").value(12000.0))
                .andExpect(jsonPath("$.discrepancyAmount").value(0.0));
    }

    @Test
    void shouldRejectBankDepositWithoutDepositReference() throws Exception {
        OpenSessionRequest openRequest = new OpenSessionRequest(null, 10000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openRequest)))
                .andExpect(status().isCreated());

        CashMovementRequest invalidDeposit = new CashMovementRequest(
                "TRANSFER_TO_BANK", 3000.0, "Dépôt journée", "CASH", null, false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDeposit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExecuteFullE2EWorkflow() throws Exception {
        InsuranceConventionEntity saar = new InsuranceConventionEntity(
                "SAAR Assurance",
                new BigDecimal("0.8000"));
        saar.setOrganizationId(org.getId());
        saar = insuranceConventionRepository.save(saar);

        InvoiceEntity e2eInvoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-E2E-99", saar);
        e2eInvoice.setOrganizationId(org.getId());

        InvoiceItemEntity item = new InvoiceItemEntity(
                "Prestation Chirurgie",
                InvoiceItemType.CONSULTATION,
                new BigDecimal("100000.0000"),
                new BigDecimal("1.0000"),
                null);
        item.setOrganizationId(org.getId());
        e2eInvoice.addItem(item);
        e2eInvoice.setStatus(InvoiceStatus.VALIDATED);
        e2eInvoice.setValidatedAt(Instant.now());
        e2eInvoice.setValidatedByUserId(caissier.getId());
        e2eInvoice = invoiceRepository.save(e2eInvoice);

        ReceivableEntity patientRec = new ReceivableEntity(
                e2eInvoice.getId(),
                "PATIENT",
                patient.getId(),
                new BigDecimal("20000.0000"));
        patientRec.setOrganizationId(org.getId());
        receivableRepository.save(patientRec);

        ReceivableEntity insuranceRec = new ReceivableEntity(
                e2eInvoice.getId(),
                "INSURANCE",
                saar.getId(),
                new BigDecimal("80000.0000"));
        insuranceRec.setOrganizationId(org.getId());
        receivableRepository.save(insuranceRec);

        OpenSessionRequest openReq = new OpenSessionRequest(null, 50000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated());

        PaymentRequest payReq = new PaymentRequest(
                new BigDecimal("20000.0000"),
                PaymentMethod.CASH,
                "REF-E2E-PAY");
        mockMvc.perform(post("/api/invoices/" + e2eInvoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        TenantContext.setTenantId(org.getId());
        List<ReceivableEntity> receivables = receivableRepository.findByInvoiceId(e2eInvoice.getId());
        ReceivableEntity updatedPatientRec = receivables.stream()
                .filter(r -> "PATIENT".equals(r.getDebtorType()))
                .findFirst().orElseThrow();
        assertEquals("PAID", updatedPatientRec.getStatus());
        assertEquals(new BigDecimal("20000.0000"), updatedPatientRec.getPaidAmount());

        InsuranceBordereauController.GenerateBordereauRequest genReq =
                new InsuranceBordereauController.GenerateBordereauRequest(saar.getId(), LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        String genResult = mockMvc.perform(post("/api/billing/insurance-bordereaux")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        InsuranceBordereauController.BordereauResponse bordereau =
                objectMapper.readValue(genResult, InsuranceBordereauController.BordereauResponse.class);

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.id() + "/send")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.id() + "/receive")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"insurerReference\":\"AR-CASH-E2E\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.id() + "/accept")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedAmount\":80000}"))
                .andExpect(status().isOk());

        InsuranceBordereauController.BordereauPaymentRequest bordereauPay =
                new InsuranceBordereauController.BordereauPaymentRequest(new BigDecimal("80000.0000"), "CHQ-ASSUR-E2E");
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.id() + "/pay")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bordereauPay)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));

        TenantContext.setTenantId(org.getId());
        List<ReceivableEntity> receivablesAfterPay = receivableRepository.findByInvoiceId(e2eInvoice.getId());
        ReceivableEntity updatedInsuranceRec = receivablesAfterPay.stream()
                .filter(r -> "INSURANCE".equals(r.getDebtorType()))
                .findFirst().orElseThrow();
        assertEquals("PAID", updatedInsuranceRec.getStatus());
        assertEquals(new BigDecimal("80000.0000"), updatedInsuranceRec.getPaidAmount());

        mockMvc.perform(get("/api/invoices/settlement-summaries?patientId=" + patient.getId())
                        .header("Authorization", "Bearer " + tokenCaissier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.invoiceId=='" + e2eInvoice.getId() + "')].collectionStatus").value("SETTLED"));

        CashMovementRequest deposit = new CashMovementRequest(
                "TRANSFER_TO_BANK", 10000.0, "Dépôt SAAR", "CASH", "BORD-E2E-DEP", false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deposit)))
                .andExpect(status().isOk());

        CloseSessionRequest closeReq = new CloseSessionRequest(60000.0, null);
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.discrepancyAmount").value(0.0));
    }

    private void addMovement(String type, double amount, String description, String method, String reference) throws Exception {
        CashMovementRequest request = new CashMovementRequest(type, amount, description, method, reference, false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
