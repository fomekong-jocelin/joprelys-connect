package com.joprelys.backend.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.billing.api.CreateInvoiceRequest;
import com.joprelys.backend.billing.api.InvoiceResponse;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionEntity;
import com.joprelys.backend.billing.infrastructure.persistence.InsuranceConventionRepository;
import com.joprelys.backend.billing.infrastructure.persistence.InvoiceRepository;
import com.joprelys.backend.billing.infrastructure.persistence.PaymentRepository;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridEntity;
import com.joprelys.backend.billing.infrastructure.persistence.TariffGridRepository;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterEntity;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterRepository;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionEntity;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionRepository;
import com.joprelys.backend.cash.infrastructure.persistence.PaymentMethod;
import com.joprelys.backend.cash.api.PaymentRequest;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
public class InvoiceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private ConsultationRepository consultationRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private HospitalizationRepository hospitalizationRepository;
    @Autowired private OrganizationalUnitRepository organizationalUnitRepository;
    @Autowired private FacilitySpaceRepository facilitySpaceRepository;
    @Autowired private InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private InsuranceConventionRepository insuranceConventionRepository;
    @Autowired private TariffGridRepository tariffGridRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;
    @Autowired private CashRegisterSessionRepository cashRegisterSessionRepository;
    @Autowired private JwtService jwtService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private OrganizationEntity org;
    private UserAccountEntity receptionist;
    private UserAccountEntity admin;
    private PatientEntity patient;
    private VisitEntity visit;
    private ConsultationEntity consultation;
    private PrescriptionEntity prescription;
    private HospitalizationEntity hospitalization;
    private InsuranceConventionEntity convention;

    private String tokenReceptionist;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM cash_register_sessions");
        jdbcTemplate.update("DELETE FROM cash_registers");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM insurance_conventions");
        jdbcTemplate.update("DELETE FROM tariff_grid");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM bed_state_changes");
        jdbcTemplate.update("DELETE FROM bed_assignments");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM beds");
        jdbcTemplate.update("DELETE FROM organizational_unit_space_assignments");
        jdbcTemplate.update("DELETE FROM inpatient_space_profiles");
        jdbcTemplate.update("DELETE FROM facility_spaces");
        jdbcTemplate.update("DELETE FROM facility_location_nodes");
        jdbcTemplate.update("DELETE FROM organizational_units");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        org = new OrganizationEntity(
                "Clinique de Facturation",
                "billing@joprelys.local",
                "999999",
                "Avenue Foch",
                "Yaounde");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        receptionist = new UserAccountEntity(
                "reception@joprelys.local",
                "Julie Reception",
                "AGENT_ACCUEIL,CAISSIER",
                "passhash");
        receptionist.setOrganizationId(org.getId());
        receptionist = userAccountRepository.save(receptionist);
        tokenReceptionist = jwtService.createToken(receptionist).value();

        admin = new UserAccountEntity("admin@joprelys.local", "Admin Clinic", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        patient = new PatientEntity(
                "DPU-TEST-001",
                "PAT-TEST-001",
                "Marc Dupont",
                "MASCULIN",
                LocalDate.of(1985, 5, 20),
                "670000000",
                "Yaounde",
                "Bastos",
                "Street X",
                "Julie",
                "671112233",
                "Aucune",
                "Aucun");
        patient = patientRepository.save(patient);

        visit = new VisitEntity(
                patient,
                "VIS-TEST-001",
                "Paludisme aigu",
                "Général",
                "MÉDECINE GÉNÉRALE",
                admin.getId(),
                Instant.now());
        visit = visitRepository.save(visit);

        consultation = new ConsultationEntity(
                visit,
                admin,
                "DOC-2026-0001",
                "Fievre, Courbatures",
                "39°C",
                "Paludisme suspecte",
                "Paludisme",
                "Paludisme severe",
                "Conclusion",
                "Prendre du repos",
                "Suivi 3j");
        consultation.setOrganizationId(org.getId());
        consultation = consultationRepository.save(consultation);

        prescription = new PrescriptionEntity(consultation);
        prescription.setPrescriptionNumber("ORD-2026-0001");
        prescription.setIssuedAt(Instant.now());
        prescription.setExpiresAt(Instant.now().plusSeconds(86400 * 7));
        prescription.setPinCode("1234");
        prescription = prescriptionRepository.save(prescription);

        PrescriptionItemEntity pItem = new PrescriptionItemEntity(
                prescription,
                "Artesunate",
                "60mg",
                "1 injection/jour",
                "3 jours",
                "3",
                "Injection intraveineuse",
                0);
        prescription.getItems().add(pItem);
        prescriptionRepository.save(prescription);

        OrganizationalUnitEntity unit = organizationalUnitRepository.saveAndFlush(new OrganizationalUnitEntity(
                org.getId(),
                null,
                "BILLING_MED_001",
                "Médecine Hommes",
                OrganizationalUnitType.CARE_UNIT,
                null));
        FacilitySpaceEntity space = facilitySpaceRepository.saveAndFlush(new FacilitySpaceEntity(
                org.getId(),
                null,
                "BILLING_A101",
                "A101",
                "HOSPITAL_ROOM"));
        inpatientSpaceProfileRepository.saveAndFlush(new InpatientSpaceProfileEntity(
                space.getId(),
                org.getId(),
                "HOSPITAL_ROOM",
                "STANDARD"));
        BedEntity bed = bedRepository.saveAndFlush(new BedEntity(space, "Bed-1"));

        hospitalization = new HospitalizationEntity(
                patient.getId(),
                unit.getId(),
                space.getId(),
                bed.getId(),
                unit.getName(),
                space.getName(),
                bed.getBedNumber(),
                "Motif chirurgie",
                "HOSP-TEST-999",
                visit.getId(),
                null,
                admin.getId());
        hospitalization.setOrganizationId(org.getId());
        hospitalization = hospitalizationRepository.saveAndFlush(hospitalization);

        convention = new InsuranceConventionEntity("AXA Cameroun", new java.math.BigDecimal("0.8000"));
        convention.setOrganizationId(org.getId());
        convention = insuranceConventionRepository.save(convention);
    }

    @Test
    void testConfigureConventionsAndTariffs() throws Exception {
        mockMvc.perform(post("/api/invoices/tariffs")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("keyLetter", "CS")
                        .param("unitValue", "12000")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyLetter").value("CS"))
                .andExpect(jsonPath("$.unitValue").value(12000.0));

        mockMvc.perform(get("/api/invoices/tariffs")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyLetter").value("CS"));

        mockMvc.perform(post("/api/invoices/conventions")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("name", "Gras Savoye")
                        .param("coveragePercentage", "0.75")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gras Savoye"))
                .andExpect(jsonPath("$.coveragePercentage").value(0.75));
    }

    @Test
    void testPrecalculateAndCreateInvoiceWithPayments() throws Exception {
        TariffGridEntity tRoom = new TariffGridEntity("ROOM_STANDARD", new java.math.BigDecimal("10000.0000"));
        tRoom.setOrganizationId(org.getId());
        tariffGridRepository.save(tRoom);

        TariffGridEntity tDrug = new TariffGridEntity("Artesunate", new java.math.BigDecimal("3000.0000"));
        tDrug.setOrganizationId(org.getId());
        tariffGridRepository.save(tDrug);

        mockMvc.perform(post("/api/invoices/precalculate")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .param("patientId", patient.getId().toString())
                        .param("visitId", visit.getId().toString())
                        .param("insuranceConventionId", convention.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(34000.0))
                .andExpect(jsonPath("$.insuranceShare").value(27200.0))
                .andExpect(jsonPath("$.patientShare").value(6800.0));

        CreateInvoiceRequest req = new CreateInvoiceRequest(
                patient.getId(),
                visit.getId(),
                convention.getId(),
                null);

        String responseStr = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(34000.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        InvoiceResponse created = objectMapper.readValue(responseStr, InvoiceResponse.class);

        mockMvc.perform(post("/api/invoices/" + created.id() + "/validate")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        TenantContext.setTenantId(org.getId());
        CashRegisterEntity register = new CashRegisterEntity("CAISSE-PRINCIPALE", "Caisse Principale");
        register.setOrganizationId(org.getId());
        register = cashRegisterRepository.save(register);

        CashRegisterSessionEntity session = new CashRegisterSessionEntity(register, receptionist.getId(), 50000.0);
        session.setOrganizationId(org.getId());
        cashRegisterSessionRepository.save(session);
        TenantContext.clear();

        PaymentRequest payReq = new PaymentRequest(
                new java.math.BigDecimal("3000.0000"),
                PaymentMethod.CASH,
                "REF-1111");
        mockMvc.perform(post("/api/invoices/" + created.id() + "/payments")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(3000.0));

        mockMvc.perform(get("/api/invoices/" + created.id())
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

        mockMvc.perform(get("/api/invoices/" + created.id() + "/receivables")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].debtorType").value("PATIENT"))
                .andExpect(jsonPath("$[0].paidAmount").value(3000.0))
                .andExpect(jsonPath("$[0].remainingAmount").value(3800.0))
                .andExpect(jsonPath("$[1].debtorType").value("INSURANCE"))
                .andExpect(jsonPath("$[1].paidAmount").value(0.0))
                .andExpect(jsonPath("$[1].remainingAmount").value(27200.0));

        mockMvc.perform(get("/api/invoices/settlement-summaries")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .param("patientId", patient.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collectionStatus").value("PATIENT_PARTIALLY_PAID"))
                .andExpect(jsonPath("$[0].patient.remainingAmount").value(3800.0))
                .andExpect(jsonPath("$[0].insurance.remainingAmount").value(27200.0));

        PaymentRequest payFinal = new PaymentRequest(
                new java.math.BigDecimal("3800.0000"),
                PaymentMethod.CASH,
                "REF-2222");
        mockMvc.perform(post("/api/invoices/" + created.id() + "/payments")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payFinal)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/invoices/" + created.id())
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/invoices/" + created.id() + "/receivables")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paidAmount").value(6800.0))
                .andExpect(jsonPath("$[0].remainingAmount").value(0.0))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[1].remainingAmount").value(27200.0));

        mockMvc.perform(get("/api/invoices/settlement-summaries")
                        .header("Authorization", "Bearer " + tokenReceptionist)
                        .param("patientId", patient.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collectionStatus").value("INSURANCE_DUE"))
                .andExpect(jsonPath("$[0].patient.status").value("PAID"))
                .andExpect(jsonPath("$[0].insurance.remainingAmount").value(27200.0));

        mockMvc.perform(get("/api/invoices/" + created.id() + "/pdf")
                        .header("Authorization", "Bearer " + tokenReceptionist))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.contains("pdf");
                });
    }
}
