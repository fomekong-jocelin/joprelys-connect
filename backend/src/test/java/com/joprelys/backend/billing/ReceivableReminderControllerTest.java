package com.joprelys.backend.billing;

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
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReceivableReminderControllerTest {

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
    private ReceivableRepository receivableRepository;

    @Autowired
    private ReceivableReminderRepository receivableReminderRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private OrganizationEntity org;
    private UserAccountEntity daf;
    private UserAccountEntity caissier;
    private PatientEntity patient;
    private VisitEntity visit;
    private InvoiceEntity invoice;
    private ReceivableEntity receivable;

    private String tokenDaf;
    private String tokenCaissier;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM receivable_reminders");
        jdbcTemplate.update("DELETE FROM receivables");
        jdbcTemplate.update("DELETE FROM payment_receipts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Org
        org = new OrganizationEntity("Clinique de Recouvrement", "recouv@joprelys.local", "999999", "Avenue Recouvrement", "Yaounde");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // DAF
        daf = new UserAccountEntity("daf@joprelys.local", "Pierre DAF", "DAF", "passhash");
        daf.setOrganizationId(org.getId());
        daf = userAccountRepository.save(daf);
        tokenDaf = jwtService.createToken(daf).value();

        // Caissier
        caissier = new UserAccountEntity("caissier@joprelys.local", "Jean Caissier", "CAISSIER", "passhash");
        caissier.setOrganizationId(org.getId());
        caissier = userAccountRepository.save(caissier);
        tokenCaissier = jwtService.createToken(caissier).value();

        // Patient
        patient = new PatientEntity("DPU-REC-01", "PAT-REC-01", "Antoine Recouvrement", "MASCULIN", LocalDate.of(1992, 10, 5), "670000009", "Yaounde", "Bastos", "Street Z", "Luc", "671112233", "Aucune", "Aucun");
        patient = patientRepository.save(patient);

        // Visit
        visit = new VisitEntity(patient, "VIS-REC-01", "Consultation", "Général", "MÉDECINE GÉNÉRALE", daf.getId(), Instant.now());
        visit = visitRepository.save(visit);

        // Invoice
        invoice = new InvoiceEntity(patient.getId(), visit.getId(), "FAC-REC-001", null);
        invoice.setOrganizationId(org.getId());
        InvoiceItemEntity item = new InvoiceItemEntity("Prestation", InvoiceItemType.CONSULTATION, 30000.0, 1.0, null);
        item.setOrganizationId(org.getId());
        invoice.addItem(item);
        invoice = invoiceRepository.save(invoice);

        // Receivable
        receivable = new ReceivableEntity(invoice.getId(), "PATIENT", patient.getId(), 30000.0);
        receivable.setOrganizationId(org.getId());
        receivable = receivableRepository.save(receivable);
    }

    @Test
    void testCreateAndGetRemindersWorkflow() throws Exception {
        // 1. Essayer de créer une relance avec le rôle CAISSIER -> 403 Forbidden
        CreateReminderRequest req = new CreateReminderRequest(
                ReceivableReminderActionType.PHONE_CALL,
                ReceivableReminderStatus.PROMISED_PAYMENT,
                "Promesse de règlement avant la fin du mois"
        );

        mockMvc.perform(post("/api/receivables/" + receivable.getId() + "/reminders")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // 2. Créer une relance avec le rôle DAF -> 201 Created
        mockMvc.perform(post("/api/receivables/" + receivable.getId() + "/reminders")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actionType").value("PHONE_CALL"))
                .andExpect(jsonPath("$.status").value("PROMISED_PAYMENT"))
                .andExpect(jsonPath("$.notes").value("Promesse de règlement avant la fin du mois"))
                .andExpect(jsonPath("$.actorId").value(daf.getId().toString()));

        // 3. Récupérer l'historique des relances -> 200 OK
        mockMvc.perform(get("/api/receivables/" + receivable.getId() + "/reminders")
                        .header("Authorization", "Bearer " + tokenDaf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].actionType").value("PHONE_CALL"))
                .andExpect(jsonPath("$[0].status").value("PROMISED_PAYMENT"))
                .andExpect(jsonPath("$[0].notes").value("Promesse de règlement avant la fin du mois"));

        // 4. Tenter d'enregistrer une relance pour une créance inexistante -> 404 Not Found
        mockMvc.perform(post("/api/receivables/" + UUID.randomUUID() + "/reminders")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
