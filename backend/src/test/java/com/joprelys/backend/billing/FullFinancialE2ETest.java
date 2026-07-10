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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FullFinancialE2ETest {

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
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private CashRegisterSessionRepository cashRegisterSessionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CashMovementRepository cashMovementRepository;

    @Autowired
    private InsuranceBordereauRepository insuranceBordereauRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
        // Nettoyage du contexte de tenant et de la base de données
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

        // Créer l'organisation (pas de @TenantId, donc pas besoin de tenant préalable)
        org = new OrganizationEntity("Clinique E2E Sante", "e2e@joprelys.local", "999999", "Avenue E2E", "Yaounde");
        org = organizationRepository.saveAndFlush(org);

        // Créer les utilisateurs
        setTenant();
        receptioniste = new UserAccountEntity("recep@test.com", "Alice Receptioniste", "AGENT_ACCUEIL", "password");
        receptioniste.setOrganizationId(org.getId());
        receptioniste = userAccountRepository.saveAndFlush(receptioniste);
        tokenReceptioniste = jwtService.createToken(receptioniste).value();

        setTenant();
        medecin = new UserAccountEntity("medecin@test.com", "Dr. Marc Medecin", "MEDECIN", "password");
        medecin.setOrganizationId(org.getId());
        medecin = userAccountRepository.saveAndFlush(medecin);
        tokenMedecin = jwtService.createToken(medecin).value();

        setTenant();
        caissier = new UserAccountEntity("caissier@test.com", "Jean Caissier", "CAISSIER", "password");
        caissier.setOrganizationId(org.getId());
        caissier = userAccountRepository.saveAndFlush(caissier);
        tokenCaissier = jwtService.createToken(caissier).value();

        setTenant();
        daf = new UserAccountEntity("daf@test.com", "Pierre DAF", "DAF", "password");
        daf.setOrganizationId(org.getId());
        daf = userAccountRepository.saveAndFlush(daf);
        tokenDaf = jwtService.createToken(daf).value();

        // Convention d'assurance (80% de prise en charge)
        setTenant();
        convention = new InsuranceConventionEntity(
                "Assurance Sante E2E",
                new java.math.BigDecimal("0.8000"));
        convention.setOrganizationId(org.getId());
        convention = insuranceConventionRepository.saveAndFlush(convention);

        // Caisse physique par défaut
        setTenant();
        caissePrincipale = new CashRegisterEntity("CAISSE-PRINCIPALE", "Caisse Principale");
        caissePrincipale.setOrganizationId(org.getId());
        caissePrincipale = cashRegisterRepository.saveAndFlush(caissePrincipale);
    }

    private void setTenant() {
        TenantContext.setTenantId(org.getId());
    }

    @Test
    void shouldExecuteFullFinancialLifecycleNominal() throws Exception {
        // 1. Admission Patient
        setTenant();
        PatientEntity patient = new PatientEntity("DPU-E2E-01", "PAT-E2E-01", "John Doe E2E", "MASCULIN", LocalDate.of(1990, 5, 10), "677889900", "Yaounde", "Bastos", "Street 1", "Marie", "670000000", "Aucune", "Aucun");
        patient = patientRepository.saveAndFlush(patient);

        setTenant();
        VisitEntity visit = new VisitEntity(patient, "VIS-E2E-01", "Consultation", "Général", "MÉDECINE GÉNÉRALE", medecin.getId(), Instant.now());
        visit = visitRepository.saveAndFlush(visit);

        // 2. Facturation avec Tiers-Payant (Brut = 100 000 FCFA, 80% Assurance, 20% Patient)
        setTenant();
        InvoiceEntity invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-E2E-001", convention);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Prestation Acte K",
                InvoiceItemType.K_SURGEON,
                new java.math.BigDecimal("100000.0000"),
                new java.math.BigDecimal("1.0000"),
                null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice = invoiceRepository.saveAndFlush(invoice);

        // Déclencher le workflow financier sur la facture
        assertEquals(100000.0, invoice.getTotalAmount());
        assertEquals(80000.0, invoice.getInsuranceShare());
        assertEquals(20000.0, invoice.getPatientShare());

        // 3. Caissier ouvre une session de caisse
        OpenSessionRequest openReq = new OpenSessionRequest(caissePrincipale.getId(), 5000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.openingBalance").value(5000.0));

        // Récupérer l'ID de la session active
        setTenant();
        CashRegisterSessionEntity session = cashRegisterSessionRepository.findByOpenedByUserIdAndStatus(caissier.getId(), "OPEN")
                .orElseThrow(() -> new AssertionError("Session active non trouvée"));

        // 4. Encaissement de la Part Patient (20 000 FCFA)
        // L'API d'encaissement se fait sur POST /api/invoices/{id}/payments avec un body JSON
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 20000, \"method\": \"CASH\", \"reference\": \"ENCAISSE-PATIENT\"}"))
                .andExpect(status().isOk());

        // 5. Génération et règlement d'un bordereau d'assurance (80 000 FCFA)
        // Création du bordereau par l'admin ou DAF
        setTenant();
        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity("BORD-E2E-01", convention, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 80000.0);
        bordereau.setOrganizationId(org.getId());
        bordereau = insuranceBordereauRepository.saveAndFlush(bordereau);

        // Lier la facture au bordereau (recharger pour éviter le conflit de version)
        setTenant();
        InvoiceEntity invoiceToLink = invoiceRepository.findById(invoice.getId()).orElseThrow();
        invoiceToLink.setInsuranceBordereauId(bordereau.getId());
        invoiceRepository.saveAndFlush(invoiceToLink);

        // Marquer le bordereau comme envoyé puis payé
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenDaf))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 80000, \"referenceNumber\": \"VIR-ASSURANCE\"}"))
                .andExpect(status().isOk());

        // Vérifier que la facture est complètement réglée
        setTenant();
        InvoiceEntity savedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, savedInvoice.getStatus());

        // 6. Versement en Banque (espèces)
        CashMovementRequest movementReq = new CashMovementRequest("TRANSFER_TO_BANK", 15000.0, "Dépôt d'espèces du jour", "CASH", "SLIP-DEPOSIT-01", false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movementReq)))
                .andExpect(status().isOk());

        // 7. Clôture de Caisse avec écart
        // En caisse : Solde initial (5000) + Reçu Espèces (20000) - Versement Banque (15000) = 10000 attendu.
        // Déclaration à 9000 (écart de -1000)
        CloseSessionRequest closeReq = new CloseSessionRequest(9000.0, "Perte d'un billet de 1000 FCFA lors du change");
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.discrepancyAmount").value(-1000.0))
                .andExpect(jsonPath("$.discrepancyResolved").value(false));

        // 8. DAF Résout l'écart
        ResolveDiscrepancyRequest resolveReq = new ResolveDiscrepancyRequest("Ecart toléré et imputé sur charges exceptionnelles après validation des pièces justificatives");
        mockMvc.perform(post("/api/cash-registers/sessions/" + session.getId() + "/resolve-discrepancy")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discrepancyResolved").value(true))
                .andExpect(jsonPath("$.resolutionNotes").value("Ecart toléré et imputé sur charges exceptionnelles après validation des pièces justificatives"));

        // 9. Exportation comptable Sage 100
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(containsString("VT"))) // Journal des ventes
                .andExpect(content().string(containsString("CA"))) // Journal de caisse
                .andExpect(content().string(containsString("BQ"))) // Journal de banque
                .andExpect(content().string(containsString("57110000")))
                .andExpect(content().string(containsString("52110000")))
                .andExpect(content().string(containsString("65600000"))); // Charge d'écart
    }

    @Test
    void shouldEnforceRbacOnCashierOperations() throws Exception {
        // Le caissier peut ouvrir une session et encaisser, mais ne peut ni exporter Sage 100 ni résoudre un écart.
        OpenSessionRequest openReq = new OpenSessionRequest(caissePrincipale.getId(), 5000.0);
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated());

        setTenant();
        CashRegisterSessionEntity session = cashRegisterSessionRepository.findByOpenedByUserIdAndStatus(caissier.getId(), "OPEN")
                .orElseThrow(() -> new AssertionError("Session active non trouvée"));

        // Clôture pour créer un écart
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseSessionRequest(4000.0, "Test écart"))))
                .andExpect(status().isOk());

        // Caissier ne doit pas pouvoir résoudre un écart (réservé au DAF/Admin)
        mockMvc.perform(post("/api/cash-registers/sessions/" + session.getId() + "/resolve-discrepancy")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveDiscrepancyRequest("Tentative caissier"))))
                .andExpect(status().isForbidden());

        // Caissier ne doit pas pouvoir exporter Sage 100 (réservé au DAF/Admin)
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceRbacOnReceptionistOperations() throws Exception {
        // L'agent d'accueil peut facturer et encaisser, mais ne peut pas exporter Sage 100, résoudre un écart ni payer un bordereau d'assurance.

        // Agent d'accueil ne doit pas pouvoir exporter Sage 100 (réservé au DAF/Admin)
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenReceptioniste)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isForbidden());

        // Agent d'accueil ne doit pas pouvoir résoudre un écart de caisse (réservé au DAF/Admin)
        mockMvc.perform(post("/api/cash-registers/sessions/" + UUID.randomUUID() + "/resolve-discrepancy")
                        .header("Authorization", "Bearer " + tokenReceptioniste)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveDiscrepancyRequest("Tentative agent d'accueil"))))
                .andExpect(status().isForbidden());

        // Agent d'accueil ne doit pas pouvoir payer un bordereau d'assurance (réservé au DAF/Admin)
        setTenant();
        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity("BORD-E2E-02", convention, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 8000.0);
        bordereau.setOrganizationId(org.getId());
        bordereau = insuranceBordereauRepository.saveAndFlush(bordereau);

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenReceptioniste)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 8000, \"referenceNumber\": \"TENTATIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceRbacOnDafOperations() throws Exception {
        // Le DAF peut exporter Sage 100 et payer un bordereau, mais ne peut pas créer de facture (réservé à l'accueil/médecin).
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isOk());

        setTenant();
        InsuranceBordereauEntity bordereau = new InsuranceBordereauEntity("BORD-E2E-03", convention, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 5000.0);
        bordereau.setOrganizationId(org.getId());
        bordereau = insuranceBordereauRepository.saveAndFlush(bordereau);

        // Marquer comme envoyé avant paiement (règle métier)
        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/send")
                        .header("Authorization", "Bearer " + tokenDaf))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/billing/insurance-bordereaux/" + bordereau.getId() + "/pay")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 5000, \"referenceNumber\": \"VIR-TEST\"}"))
                .andExpect(status().isOk());

        // DAF ne doit pas pouvoir créer une facture (réservé à l'accueil/médecin/infirmier)
        mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\": \"" + UUID.randomUUID() + "\", \"items\": []}"))
                .andExpect(status().isForbidden());
    }
}
