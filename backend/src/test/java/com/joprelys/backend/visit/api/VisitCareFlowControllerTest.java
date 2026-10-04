package com.joprelys.backend.visit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisitCareFlowControllerTest {

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
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository unitRepository;

	@Autowired
	private com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository staffUnitAssignmentRepository;

	private OrganizationEntity organization;
	private UserAccountEntity savedDoctorA;
	private String tokenAgent;
	private String tokenDoctorA;
	private String tokenDoctorB;
	private VisitEntity visit;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		jdbcTemplate.update("DELETE FROM staff_organizational_unit_assignments");
		jdbcTemplate.update("DELETE FROM organizational_units");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		OrganizationEntity org = organization = organizationRepository.save(
				new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));

		UserAccountEntity agent = new UserAccountEntity("agent@joprelys.local", "Agent", "AGENT_ACCUEIL", "passhash");
		agent.setOrganizationId(org.getId());
		UserAccountEntity doctorA = new UserAccountEntity("doc.a@joprelys.local", "Dr Alpha", "MEDECIN", "passhash");
		doctorA.setOrganizationId(org.getId());
		UserAccountEntity doctorB = new UserAccountEntity("doc.b@joprelys.local", "Dr Beta", "MEDECIN", "passhash");
		doctorB.setOrganizationId(org.getId());
		tokenAgent = jwtService.createToken(userAccountRepository.save(agent)).value();
		savedDoctorA = userAccountRepository.save(doctorA);
		tokenDoctorA = jwtService.createToken(savedDoctorA).value();
		tokenDoctorB = jwtService.createToken(userAccountRepository.save(doctorB)).value();

		TenantContext.setTenantId(org.getId());
		PatientEntity patient = patientRepository.save(new PatientEntity(
				"DPU-A", "PAT-A", "Patient A", "MASCULIN", LocalDate.of(1990, 1, 1), "+123", "Douala", "", "", "", "", "", ""));
		visit = visitRepository.save(new VisitEntity(patient, "VIS-A", "Fièvre", "CONSULTATION"));
		TenantContext.clear();
	}

    @Test
    void simultaneousTakeChargeAllowsExactlyOnePractitioner() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
                        .header("Authorization", "Bearer " + tokenDoctorA)).andReturn().getResponse().getStatus();
            });
            var second = workers.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
                        .header("Authorization", "Bearer " + tokenDoctorB)).andReturn().getResponse().getStatus();
            });
            start.countDown();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(200, 409),
                    java.util.stream.Stream.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                            second.get(15, java.util.concurrent.TimeUnit.SECONDS)).sorted().toList());
        }
    }

	@Test
	void vitalsMoveVisitToReadyAndAreKeptAsHistoryWithAlerts() throws Exception {
		postVitals("{\"temperature\": 39.2, \"systolic\": 120, \"diastolic\": 80}");
		postVitals("{\"temperature\": 37.4, \"spo2\": 88}");

		mockMvc.perform(get("/api/visits/" + visit.getId())
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careStage").value("PRET_MEDECIN"))
				.andExpect(jsonPath("$.vitals.alerts[0].code").value("SPO2_LOW"))
				.andExpect(jsonPath("$.vitals.alerts[0].severity").value("CRITICAL"));

		mockMvc.perform(get("/api/visits/" + visit.getId() + "/vitals/history")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].spo2").value(88))
				.andExpect(jsonPath("$[0].recordedByName").value("Agent"))
				.andExpect(jsonPath("$[1].temperature").value(39.2))
				.andExpect(jsonPath("$[1].alerts[0].code").value("TEMPERATURE_HIGH"));
	}

	@Test
	void onlyOnePractitionerHoldsTheConsultationUnlessExplicitTakeOver() throws Exception {
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careStage").value("EN_CONSULTATION"))
				.andExpect(jsonPath("$.consultingPractitionerName").value("Dr Alpha"));

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Dr Alpha")));

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenDoctorB)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symptoms\": \"Fièvre\", \"diagnosis\": \"Paludisme\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.param("takeOver", "true")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.consultingPractitionerName").value("Dr Beta"));
	}

	@Test
	void practitionerCanPutPatientBackInQueue() throws Exception {
		postVitals("{\"temperature\": 37.0}");
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/release")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/release")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.careStage").value("PRET_MEDECIN"))
				.andExpect(jsonPath("$.consultingPractitionerId").doesNotExist());
	}

	@Test
	void savingConsultationWithoutTakeChargeImplicitlyAssignsTheDoctor() throws Exception {
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/consultation")
				.header("Authorization", "Bearer " + tokenDoctorA)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"symptoms\": \"Fièvre\", \"diagnosis\": \"Paludisme\"}"))
				.andExpect(status().is2xxSuccessful());

		mockMvc.perform(get("/api/visits/" + visit.getId())
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(jsonPath("$.careStage").value("EN_CONSULTATION"))
				.andExpect(jsonPath("$.consultingPractitionerName").value("Dr Alpha"));
	}

	@Test
	void mineScopeListsOnlyPatientsHeldByTheConnectedPractitioner() throws Exception {
		mockMvc.perform(get("/api/visits/active").param("scope", "MINE")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(post("/api/visits/" + visit.getId() + "/take-charge")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/visits/active").param("scope", "MINE")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/visits/active").param("scope", "MINE")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get("/api/visits/active")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void receptionAgentGetsServicesAndCliniciansWithoutStaffManagementRights() throws Exception {
		mockMvc.perform(get("/api/visits/admission-options")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.services").isNotEmpty())
				.andExpect(jsonPath("$.services", org.hamcrest.Matchers.hasItem("Médecine générale")))
				.andExpect(jsonPath("$.services", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Pharmacie"))))
				.andExpect(jsonPath("$.services", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Bloc opératoire"))))
				.andExpect(jsonPath("$.practitioners.length()").value(2))
				.andExpect(jsonPath("$.practitioners[*].displayName",
						org.hamcrest.Matchers.containsInAnyOrder("Dr Alpha", "Dr Beta")));

		mockMvc.perform(get("/api/staff")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isForbidden());
	}

	@Test
	void configuredStructureDrivesAdmissionServicesAndPractitionerUnits() throws Exception {
		TenantContext.setTenantId(organization.getId());
		var medicine = unitRepository.save(new OrganizationalUnitEntity(
				organization.getId(), null, "SRV-MG", null, OrganizationalUnitType.SERVICE, "GENERAL_MEDICINE"));
		var consultations = unitRepository.save(new OrganizationalUnitEntity(
				organization.getId(), medicine.getId(), "UNIT-CONSULT", "Consultations", OrganizationalUnitType.CARE_UNIT, null));
		unitRepository.save(new OrganizationalUnitEntity(
				organization.getId(), null, "SRV-IMG", null, OrganizationalUnitType.SERVICE, "IMAGING"));
		unitRepository.save(new OrganizationalUnitEntity(
				organization.getId(), null, "SRV-PHARMA", null, OrganizationalUnitType.SERVICE, "PHARMACY"));
		staffUnitAssignmentRepository.save(new StaffOrganizationalUnitAssignmentEntity(
				organization.getId(), savedDoctorA.getId(), consultations.getId(), "PRACTITIONER", true,
				java.time.Instant.now().minusSeconds(60), null));
		TenantContext.clear();

		mockMvc.perform(get("/api/visits/admission-options")
				.header("Authorization", "Bearer " + tokenAgent))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.services", org.hamcrest.Matchers.contains("Consultations", "Imagerie")))
				.andExpect(jsonPath("$.practitioners[?(@.displayName == 'Dr Alpha')].unitNames[0]").value("Consultations"));

		// Visite orientée au niveau du service : visible pour les soignants de ses unités de soins.
		jdbcTemplate.update("UPDATE visits SET service_name = 'Médecine générale' WHERE id = ?", visit.getId());
		mockMvc.perform(get("/api/visits/active").param("scope", "SERVICE")
				.header("Authorization", "Bearer " + tokenDoctorA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/visits/active").param("scope", "SERVICE")
				.header("Authorization", "Bearer " + tokenDoctorB))
				.andExpect(jsonPath("$.length()").value(0));
	}

	private void postVitals(String json) throws Exception {
		mockMvc.perform(post("/api/visits/" + visit.getId() + "/vitals")
				.header("Authorization", "Bearer " + tokenAgent)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().isOk());
	}
}
