package com.joprelys.backend.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

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
import com.joprelys.backend.cash.infrastructure.persistence.*;
import com.joprelys.backend.cash.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FullFinancialE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InsuranceConventionRepository insuranceConventionRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;
    @Autowired private CashRegisterSessionRepository cashRegisterSessionRepository;
    @Autowired private InsuranceBordereauRepository insuranceBordereauRepository;
    @Autowired private JwtService jwtService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private OrganizationEntity org;
    private UserAccountEntity receptioniste;
    private UserAccountEntity medecin;
    private UserAccountEntity caissier;
    private UserAccountEntity daf;
    private String tokenReceptioniste;
    private String tokenMedecin;
    private String tokenCaissier;
    private String tokenDaf;
    private InsuranceConventionEntity convention;
    private CashRegisterEntity caissePrincipale;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM receivable_reminders");
        jdbcTemplate.update("DELETE FROM receivables");
        jdbcTemplate.update("DELETE FROM payment_receipts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM insurance_bordereaux");
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM cash_register_sessions");
        jdbcTemplate.update("DELETE FROM cash_registers");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        org = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique E2E Sante", "e2e@joprelys.local", "999999", "Avenue E2E", "Yaounde"));

        receptioniste = saveUser("recep@test.com", "Alice Receptioniste", "AGENT_ACCUEIL");
        tokenReceptioniste = jwtService.createToken(receptioniste).value();
        medecin = saveUser("medecin@test.com", "Dr. Marc Medecin", "MEDECIN");
        tokenMedecin = jwtService.createToken(medecin).value();
        caissier = saveUser("caissier@test.com", "Jean Caissier", "CAISSIER");
        tokenCaissier = jwtService.createToken(caissier).value();
        daf = saveUser("daf@test.com", "Pierre DAF", "DAF");
        tokenDaf = jwtService.createToken(daf).value();

        setTenant();
        convention = new InsuranceConventionEntity("Assurance Sante E2E", new BigDecimal("0.8000"));
        convention.setOrganizationId(org.getId());
        convention = insuranceConventionRepository.saveAndFlush(convention);

        caissePrincipale = new CashRegisterEntity("CAISSE-PRINCIPALE", "Caisse Principale");
        caissePrincipale.setOrganizationId(org.getId());
        caissePrincipale = cashRegisterRepository.saveAndFlush(caissePrincipale);
    }

    private UserAccountEntity saveUser(String email, String name, String role) {
        setTenant();
        UserAccountEntity user = new UserAccountEntity(email, name, role, "password");
        user.setOrganizationId(org.getId());
        return userAccountRepository.saveAndFlush(user);
    }

    private void setTenant() {
        TenantContext.setTenantId(org.getId());
    }

    @Test
    void shouldExecuteFullFinancialLifecycleNominal() throws Exception {
        setTenant();
        PatientEntity patient = patientRepository.saveAndFlush(new PatientEntity(
                "DPU-E2E-01", "PAT-E2E-01", "John Doe E2E", "MASCULIN", LocalDate.of(1990, 5, 10),
                "677889900", "Yaounde", "Bastos", "Street 1", "Marie", "670000000", "Aucune", "Aucun"));
        VisitEntity visit = visitRepository.saveAndFlush(new VisitEntity(
                patient, "VIS-E2E-01", "Consultation", "Général", "MÉDECINE GÉNÉRALE", medecin.getId(), Instant.now()));

        InvoiceEntity invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-E2E-001", convention);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Prestation Acte K", InvoiceItemType.K_SURGEON,
                new BigDecimal("100000.0000"), new BigDecimal("1.0000"), null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice = invoiceRepository.saveAndFlush(invoice);

        TenantContext.clear();
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/validate")
                        .header("Authorization", "Bearer " + tokenReceptioniste))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
        setTenant();
        invoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(new BigDecimal("100000.0000"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("80000.0000"), invoice.getInsuranceShare());
        assertEquals(new BigDecimal("20000.0000"), invoice.getPatientShare());

        OpenSessionRequest openReq = new OpenSessionRequest(caissePrincipale.getId(), 5000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
        setTenant();
        CashRegisterSessionEntity session = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(caissier.getId(), "OPEN").orElseThrow();

        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":20000,\"method\":\"CASH\",\"reference\":\"ENCAISSE-PATIENT\"}"))
                .andExpect(status().isOk());

        setTenant();
        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity(
                "BORD-E2E-01", convention, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                new BigDecimal("80000.0000"));
        bordereau.setOrganizationId(org.getId());
        bordereau = insuranceBordereauRepository.saveAndFlush(bordereau);
        InvoiceEntity invoiceToLink = invoiceRepository.findById(invoice.getId()).orElseThrow();
        invoiceToLink.setInsuranceBordereauId(bordereau.getId());
        invoiceRepository.saveAndFlush(invoiceToLink);

        progressAndPayBordereau(bordereau.getId(), new BigDecimal("80000.0000"), "AR-E2E-01", "VIR-ASSURANCE");

        setTenant();
        assertEquals(InvoiceStatus.SETTLED, invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus());

        CashMovementRequest movementReq = new CashMovementRequest(
                "TRANSFER_TO_BANK", 15000.0, "Dépôt d'espèces du jour", "CASH", "SLIP-DEPOSIT-01", false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movementReq)))
                .andExpect(status().isOk());

        CloseSessionRequest closeReq = new CloseSessionRequest(9000.0, "Perte d'un billet de 1000 FCFA lors du change");
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.discrepancyAmount").value(-1000.0));

        ResolveDiscrepancyRequest resolveReq = new ResolveDiscrepancyRequest(
                "Ecart toléré et imputé sur charges exceptionnelles après validation des pièces justificatives");
        mockMvc.perform(post("/api/cash-registers/sessions/" + session.getId() + "/resolve-discrepancy")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discrepancyResolved").value(true));

        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(containsString("VT")))
                .andExpect(content().string(containsString("CA")))
                .andExpect(content().string(containsString("BQ")))
                .andExpect(content().string(containsString("57110000")))
                .andExpect(content().string(containsString("52110000")))
                .andExpect(content().string(containsString("65600000")));
    }

    @Test
    void shouldEnforceRbacOnCashierOperations() throws Exception {
        OpenSessionRequest openReq = new OpenSessionRequest(caissePrincipale.getId(), 5000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated());
        setTenant();
        CashRegisterSessionEntity session = cashRegisterSessionRepository
                .findByOpenedByUserIdAndStatus(caissier.getId(), "OPEN").orElseThrow();

        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseSessionRequest(4000.0, "Test écart"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cash-registers/sessions/" + session.getId() + "/resolve-discrepancy")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveDiscrepancyRequest("Tentative caissier"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceRbacOnReceptionistOperations() throws Exception {
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenReceptioniste)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/cash-registers/sessions/" + UUID.randomUUID() + "/resolve-discrepancy")
                        .header("Authorization", "Bearer " + tokenReceptioniste)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveDiscrepancyRequest("Tentative agent d'accueil"))))
                .andExpect(status().isForbidden());

        setTenant();
        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity(
                "BORD-E2E-02", convention, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                new BigDecimal("8000.0000"));
        bordereau.setOrganizationId(org.getId());
        bordereau = insuranceBordereauRepository.saveAndFlush(bordereau);
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenReceptioniste)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":8000,\"referenceNumber\":\"TENTATIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceRbacOnDafOperations() throws Exception {
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk());

        setTenant();
        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity(
                "BORD-E2E-03", convention, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                new BigDecimal("5000.0000"));
        bordereau.setOrganizationId(org.getId());
        bordereau = insuranceBordereauRepository.saveAndFlush(bordereau);

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenDaf))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/receive")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"insurerReference\":\"AR-TEST-DAF\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/accept")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedAmount\":5000}"))
                .andExpect(status().isOk());
        // Le rôle est autorisé, mais le domaine refuse un paiement non affectable à une facture/créance.
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000,\"referenceNumber\":\"VIR-TEST\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + UUID.randomUUID() + "\",\"items\":[]}"))
                .andExpect(status().isForbidden());
    }

    private void progressAndPayBordereau(UUID id, BigDecimal amount, String insurerReference, String paymentReference)
            throws Exception {
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/send")
                        .header("Authorization", "Bearer " + tokenDaf))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/receive")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"insurerReference\":\"" + insurerReference + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/accept")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedAmount\":" + amount.toPlainString() + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + id + "/pay")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount.toPlainString() + ",\"referenceNumber\":\"" + paymentReference + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SETTLED"));
    }
}
