package com.joprelys.backend.cash;

import static org.hamcrest.Matchers.startsWith;
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
import com.joprelys.backend.billing.infrastructure.persistence.*;
import com.joprelys.backend.billing.api.PaymentRequest;
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

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM payment_receipts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM cash_register_sessions");
        jdbcTemplate.update("DELETE FROM cash_registers");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Org
        org = new OrganizationEntity("Clinique Caisse", "caisse@joprelys.local", "999999", "Avenue Caisse", "Yaounde");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // Caissier
        caissier = new UserAccountEntity("caissier@joprelys.local", "Pierre Caisse", "CAISSIER", "passhash");
        caissier.setOrganizationId(org.getId());
        caissier = userAccountRepository.save(caissier);
        tokenCaissier = jwtService.createToken(caissier).value();

        // Patient
        patient = new PatientEntity("DPU-CS-01", "PAT-CS-01", "Antoine Caisse", "MASCULIN", LocalDate.of(1992, 10, 5), "670000009", "Yaounde", "Bastos", "Street Z", "Luc", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        // Visit
        visit = new VisitEntity(patient, "VIS-CS-01", "Consultation tri", "Général", "MÉDECINE GÉNÉRALE", caissier.getId(), Instant.now());
        visit = visitRepository.save(visit);

        // Invoice
        invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-CS-001", null);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity("Acte", InvoiceItemType.CONSULTATION, 15000.0, 1.0, null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice = invoiceRepository.save(invoice);
    }

    @Test
    void testOpenCloseSessionAndMovements() throws Exception {
        // 1. Essayer de payer la facture sans session ouverte -> 409 CONFLICT
        PaymentRequest payReq = new PaymentRequest(15000.0, PaymentMethod.CASH, "REF-PAY");
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict());

        // 2. Ouvrir la session de caisse
        OpenSessionRequest openReq = new OpenSessionRequest(null, 50000.0); // Caisse par défaut, 50 000 FCFA fond
        String responseStr = mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.openingBalance").value(50000.0))
                .andReturn().getResponse().getContentAsString();

        CashSessionResponse session = objectMapper.readValue(responseStr, CashSessionResponse.class);

        // 3. Essayer de réouvrir une session -> 409 CONFLICT
        mockMvc.perform(post("/api/cash-registers/sessions/open")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openReq)))
                .andExpect(status().isConflict());

        // 4. Enregistrer une dépense trop élevée sans double visa -> 400 BAD REQUEST
        CashMovementRequest invalidOut = new CashMovementRequest("OUT", 120000.0, "Achat bureau", "CASH", "REF-OUT", false);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOut)))
                .andExpect(status().isBadRequest());

        // 5. Enregistrer la dépense avec double visa -> 200 OK
        CashMovementRequest validOut = new CashMovementRequest("OUT", 120000.0, "Achat bureau", "CASH", "REF-OUT", true);
        mockMvc.perform(post("/api/cash-registers/movements")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOut)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(120000.0));

        // 6. Effectuer le paiement sur la facture (doit réussir maintenant)
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/payments")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        // 7. Vérifier le reçu de paiement généré
        TenantContext.setTenantId(org.getId());
        PaymentEntity createdPayment = paymentRepository.findByInvoiceId(invoice.getId()).get(0);
        mockMvc.perform(get("/api/cash-registers/payments/" + createdPayment.getId() + "/receipt")
                        .header("Authorization", "Bearer " + tokenCaissier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value(startsWith("REC-")));

        // 8. Clôturer la session de caisse avec écart non justifié -> 400 BAD REQUEST (Solde théorique = 50000 - 120000 + 15000 = -55000)
        // Disons que l'on déclare avoir -50 000 (donc écart de +5000), sans raison
        CloseSessionRequest closeInvalid = new CloseSessionRequest(-50000.0, "");
        mockMvc.perform(post("/api/cash-registers/sessions/close")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeInvalid)))
                .andExpect(status().isBadRequest());

        // 9. Clôturer avec justification -> 200 OK
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
}
