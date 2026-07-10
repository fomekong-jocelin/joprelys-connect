package com.joprelys.backend.billing;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceStatus;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableEntity;
import com.joprelys.backend.billing.infrastructure.persistence.ReceivableRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
class CashierCollectionQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ReceivableRepository receivableRepository;

    @Autowired
    private JwtService jwtService;

    private String cashierToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM receivable_reminders");
        jdbcTemplate.update("DELETE FROM receivables");
        jdbcTemplate.update("DELETE FROM payment_receipts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM patients");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM organizations");

        OrganizationEntity firstOrganization = organizationRepository.save(new OrganizationEntity(
                "Clinique File Caisse",
                "queue@joprelys.local",
                "670000001",
                "Akwa",
                "Douala"
        ));
        OrganizationEntity secondOrganization = organizationRepository.save(new OrganizationEntity(
                "Clinique Autre Tenant",
                "other-queue@joprelys.local",
                "670000002",
                "Bastos",
                "Yaoundé"
        ));

        TenantContext.setTenantId(firstOrganization.getId());
        UserAccountEntity cashier = new UserAccountEntity(
                "cashier.queue@joprelys.local",
                "Caissier Queue",
                "CAISSIER",
                "passhash"
        );
        cashier.setOrganizationId(firstOrganization.getId());
        cashier = userAccountRepository.save(cashier);
        cashierToken = jwtService.createToken(cashier).value();

        UserAccountEntity doctor = new UserAccountEntity(
                "doctor.queue@joprelys.local",
                "Médecin Queue",
                "MEDECIN",
                "passhash"
        );
        doctor.setOrganizationId(firstOrganization.getId());
        doctor = userAccountRepository.save(doctor);
        doctorToken = jwtService.createToken(doctor).value();

        createCollectableInvoice(
                firstOrganization.getId(),
                "DPU-QUEUE-TENANT-1",
                "Patient Premier Tenant",
                "FAC-QUEUE-TENANT-1"
        );

        TenantContext.clear();
        TenantContext.setTenantId(secondOrganization.getId());
        createCollectableInvoice(
                secondOrganization.getId(),
                "DPU-QUEUE-TENANT-2",
                "Patient Autre Tenant",
                "FAC-QUEUE-TENANT-2"
        );
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnOnlyCurrentTenantPatientCollectionQueueToCashier() throws Exception {
        mockMvc.perform(get("/api/invoices/collection-queue")
                        .header("Authorization", "Bearer " + cashierToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientName").value("Patient Premier Tenant"))
                .andExpect(jsonPath("$[0].globalPatientNumber").value("DPU-QUEUE-TENANT-1"))
                .andExpect(jsonPath("$[0].invoiceNumber").value("FAC-QUEUE-TENANT-1"))
                .andExpect(jsonPath("$[0].patientRemainingAmount").value(20000.0))
                .andExpect(jsonPath("$[0].collectionStatus").value("PATIENT_DUE"));
    }

    @Test
    void shouldRejectClinicalRoleFromCashierQueue() throws Exception {
        mockMvc.perform(get("/api/invoices/collection-queue")
                        .header("Authorization", "Bearer " + doctorToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/invoices/collection-queue")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    private void createCollectableInvoice(UUID organizationId,
                                          String dpu,
                                          String patientName,
                                          String invoiceNumber) {
        PatientEntity patient = new PatientEntity(
                dpu,
                "LOCAL-" + dpu,
                patientName,
                "FEMININ",
                LocalDate.of(1992, 4, 12),
                "699000000",
                "Douala",
                "Akwa",
                "Adresse",
                "Contact",
                "698000000",
                "Aucune",
                "Aucun"
        );
        patient.setOrganizationId(organizationId);
        patient = patientRepository.save(patient);

        InvoiceEntity invoice = new InvoiceEntity(patient.getId(), null, invoiceNumber, null);
        invoice.setOrganizationId(organizationId);
        InvoiceItemEntity item = new InvoiceItemEntity(
                "Consultation",
                InvoiceItemType.CONSULTATION,
                new BigDecimal("20000.0000"),
                new BigDecimal("1.0000"),
                BigDecimal.ONE
        );
        item.setOrganizationId(organizationId);
        invoice.addItem(item);
        invoice.setStatus(InvoiceStatus.VALIDATED);
        invoice.setValidatedAt(Instant.now());
        invoice = invoiceRepository.save(invoice);

        ReceivableEntity receivable = new ReceivableEntity(
                invoice.getId(),
                "PATIENT",
                patient.getId(),
                invoice.getPatientShare()
        );
        receivable.setOrganizationId(organizationId);
        receivableRepository.save(receivable);
    }
}
