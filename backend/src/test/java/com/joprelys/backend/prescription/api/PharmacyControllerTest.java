package com.joprelys.backend.prescription.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.application.PharmacyService;
import com.joprelys.backend.prescription.infrastructure.persistence.*;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
public class PharmacyControllerTest {

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
	private ConsultationRepository consultationRepository;

	@Autowired
	private PrescriptionRepository prescriptionRepository;

	@Autowired
	private PrescriptionDispensationRepository dispensationRepository;

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Autowired
	private PharmacyService pharmacyService;

	@Autowired private com.joprelys.backend.auth.security.JwtService jwtService;
    private String pharmacistToken;
    private OrganizationEntity orgA;
	private UserAccountEntity doctor;
	private PatientEntity patient;
	private VisitEntity visit;
	private ConsultationEntity consultation;
	private PrescriptionEntity prescription;
	private PrescriptionItemEntity item1;

	@BeforeEach
	void setUp() throws Exception {
		pharmacyService.resetLockouts();
		jdbcTemplate.update("DELETE FROM dispensation_items");
		jdbcTemplate.update("DELETE FROM prescription_dispensations");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();

		orgA = organizationRepository.save(new OrganizationEntity("Clinique A", "contacta@joprelys.local", "123", "Street A", "Douala"));
		doctor = userAccountRepository.save(new UserAccountEntity("doctor@joprelys.local", "Dr. House", "MEDECIN", "passhash"));

		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());

		patient = new PatientEntity(
				"DPU-1", "PAT-1", "John Doe", "MASCULIN",
				java.time.LocalDate.of(1990, 1, 1), "12345", "Douala",
				"", "", "", "", "", ""
		);
		patient = patientRepository.save(patient);

		visit = new VisitEntity(patient, "VIS-1", "Fever", "Général");
		visit = visitRepository.save(visit);

		consultation = new ConsultationEntity(visit, doctor, "DOC-1", "Symptoms", "Exam", "Diagnosis", null, null);
		consultation = consultationRepository.save(consultation);

		prescription = new PrescriptionEntity(consultation);
		prescription.setPrescriptionNumber("ORD-20260703-000001");
		prescription.setPinCode("1234");
		prescription.setExpiresAt(Instant.now().plus(90, ChronoUnit.DAYS));
		prescription.setStatus("ACTIVE");
		prescription = prescriptionRepository.save(prescription);

		item1 = new PrescriptionItemEntity(prescription, "Doliprane 1000mg", "1000mg", "1x/day", "5 days", "10", "A prendre avec de l'eau", 0);
		prescription.getItems().add(item1);
		prescription = prescriptionRepository.save(prescription);
		com.joprelys.backend.auth.security.TenantContext.clear();
        UserAccountEntity pharmacist = new UserAccountEntity("pharmacist@joprelys.local", "Pharmacien", "PHARMACIEN", "passhash");
        pharmacist.setOrganizationId(orgA.getId());
        pharmacist = userAccountRepository.save(pharmacist);
        pharmacistToken = jwtService.createToken(pharmacist).value();
        mockMvc.perform(post("/api/public/pharmacy/prescriptions/validate")
                .header("Authorization", "Bearer " + pharmacistToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"prescriptionNumber":"ORD-20260703-000001","pinCode":"1234","reviewNotes":"Doses et interactions revues."}
                        """))
                .andExpect(status().isNoContent());
	}

	@Test
	void testVerifyPrescriptionSuccess() throws Exception {
		String body = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234"
				}
				""";

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prescriptionNumber").value("ORD-20260703-000001"))
				.andExpect(jsonPath("$.patientName").value("John Doe"))
				.andExpect(jsonPath("$.doctorName").value("Dr. House"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.items[0].drugName").value("Doliprane 1000mg"))
				.andExpect(jsonPath("$.items[0].quantityAlreadyDispensed").value(0));
	}

	@Test
	void testVerifyPrescriptionWrongPin() throws Exception {
		String body = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "9999"
				}
				""";

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testVerifyPrescriptionLockout() throws Exception {
		String body = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "9999"
				}
				""";

		// Fail 3 times
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isTooManyRequests());
	}

	@Test
	void testDispensePrescriptionSuccess() throws Exception {
		String verifyBody = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234"
				}
				""";

		String dispenseBody = String.format("""
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234",
					"pharmacyName": "Pharmacie du Centre",
					"pharmacistLicense": "LIC-123",
					"dispensedItems": [
						{
							"prescriptionItemId": "%s",
							"quantityDispensed": 10
						}
					]
				}
				""", item1.getId());

		// Perform Dispensation
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(dispenseBody))
				.andExpect(status().isNoContent());

		// Verify changes
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(verifyBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FULLY_DISPENSED"))
				.andExpect(jsonPath("$.items[0].quantityAlreadyDispensed").value(10));
	}

	@Test
	void testDispensationHistorySuccess() throws Exception {
		String dispenseBody = String.format("""
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234",
					"pharmacyName": "Pharmacie du Centre",
					"pharmacistLicense": "LIC-123",
					"dispensedItems": [
						{
							"prescriptionItemId": "%s",
							"quantityDispensed": 4,
							"substitutedWith": "Paracetamol generique"
						}
					]
				}
				""", item1.getId());

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(dispenseBody))
				.andExpect(status().isNoContent());

		String historyBody = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234"
				}
				""";

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/history")
						.contentType(MediaType.APPLICATION_JSON)
						.content(historyBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].pharmacyName").value("Pharmacie du Centre"))
				.andExpect(jsonPath("$[0].pharmacistLicense").value("LIC-123"))
				.andExpect(jsonPath("$[0].items[0].drugName").value("Doliprane 1000mg"))
				.andExpect(jsonPath("$[0].items[0].quantityDispensed").value(4))
				.andExpect(jsonPath("$[0].items[0].substitutedWith").value("Paracetamol generique"));
	}

	@Test
	void testDispensePrescriptionPartialSuccess() throws Exception {
		String verifyBody = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234"
				}
				""";

		String dispenseBody = String.format("""
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234",
					"pharmacyName": "Pharmacie du Centre",
					"pharmacistLicense": "LIC-123",
					"dispensedItems": [
						{
							"prescriptionItemId": "%s",
							"quantityDispensed": 4
						}
					]
				}
				""", item1.getId());

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(dispenseBody))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(verifyBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PARTIALLY_DISPENSED"))
				.andExpect(jsonPath("$.items[0].quantityAlreadyDispensed").value(4));
	}

	@Test
	void testVerifyExpiredPrescription() throws Exception {
		com.joprelys.backend.auth.security.TenantContext.setTenantId(orgA.getId());
		prescription.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
		prescriptionRepository.save(prescription);
		com.joprelys.backend.auth.security.TenantContext.clear();

		String body = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234"
				}
				""";

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EXPIRED"));
	}

	@Test
	void testVerifyLockoutBlocksSubsequentVerify() throws Exception {
		String wrongPin = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "XXXX"
				}
				""";

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(wrongPin)).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(wrongPin)).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(wrongPin)).andExpect(status().isTooManyRequests());

		// Verify is also blocked after lockout
		String correctPin = """
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234"
				}
				""";
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/verify").contentType(MediaType.APPLICATION_JSON).content(correctPin)).andExpect(status().isTooManyRequests());
	}

	@Test
	void testDispenseFullyDispensedPrescriptionIsBlocked() throws Exception {
		String dispenseBody = String.format("""
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234",
					"pharmacyName": "Pharmacie du Centre",
					"pharmacistLicense": "LIC-123",
					"dispensedItems": [
						{
							"prescriptionItemId": "%s",
							"quantityDispensed": 10
						}
					]
				}
				""", item1.getId());

		// First dispense — full
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(dispenseBody))
				.andExpect(status().isNoContent());

		// Second dispense — must be blocked
		mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(dispenseBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Cette ordonnance a déjà été entièrement dispensée."));
	}
	@Test
	void testDispensePrescriptionExceedsQuantity() throws Exception {
		String dispenseBody = String.format("""
				{
					"prescriptionNumber": "ORD-20260703-000001",
					"pinCode": "1234",
					"pharmacyName": "Pharmacie du Centre",
					"pharmacistLicense": "LIC-123",
					"dispensedItems": [
						{
							"prescriptionItemId": "%s",
							"quantityDispensed": 11
						}
					]
				}
				""", item1.getId());

		mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(dispenseBody))
				.andExpect(status().isBadRequest());
	}
    @Test
    void unauthenticatedPinHolderCannotDispense() throws Exception {
        mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dispenseBody())).andExpect(status().isForbidden());
    }

    @Test
    void pharmacistCannotDispenseWithoutRecordedReview() throws Exception {
        jdbcTemplate.update("UPDATE prescriptions SET pharmacy_validated_at = NULL, pharmacy_validated_by = NULL WHERE id = ?", prescription.getId());
        mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                .header("Authorization", "Bearer " + pharmacistToken)
                .contentType(MediaType.APPLICATION_JSON).content(dispenseBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void clinicalWriterCannotValidateOrDispense() throws Exception {
        UserAccountEntity nurse = new UserAccountEntity("nurse@joprelys.local", "Infirmier", "INFIRMIER", "passhash");
        nurse.setOrganizationId(orgA.getId());
        String nurseToken = jwtService.createToken(userAccountRepository.save(nurse)).value();
        mockMvc.perform(post("/api/public/pharmacy/prescriptions/dispense")
                .header("Authorization", "Bearer " + nurseToken)
                .contentType(MediaType.APPLICATION_JSON).content(dispenseBody())).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/public/pharmacy/prescriptions/validate")
                .header("Authorization", "Bearer " + nurseToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"prescriptionNumber":"ORD-20260703-000001","pinCode":"1234","reviewNotes":"Review"}
                    """)).andExpect(status().isForbidden());
    }

    private String dispenseBody() {
        return """
            {"prescriptionNumber":"ORD-20260703-000001","pinCode":"1234",
             "pharmacyName":"Pharmacie","pharmacistLicense":"LIC-1",
             "dispensedItems":[{"prescriptionItemId":"%s","quantityDispensed":1}]}
            """.formatted(item1.getId());
    }

}
