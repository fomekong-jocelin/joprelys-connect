package com.joprelys.backend.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.appointment.api.PatientBookAppointmentRequest;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentStatus;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PatientAppointmentConcurrencyTest {

	private static final ZoneId ZONE = ZoneId.of("Africa/Douala");

	@Autowired
	private PatientAppointmentService patientAppointmentService;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PatientRepository patientRepository;

	@Autowired
	private DoctorAvailabilityRepository availabilityRepository;

	@Autowired
	private AppointmentRepository appointmentRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private OrganizationEntity organization;
	private UserAccountEntity doctor;
	private PatientEntity patientA;
	private PatientEntity patientB;

	@BeforeEach
	void setUp() {
		cleanDatabase();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		organization = organizationRepository.saveAndFlush(new OrganizationEntity(
				"Clinique Concurrence " + suffix,
				"concurrency-" + suffix + "@test.local",
				"600010",
				"Akwa",
				"Douala"));
		doctor = new UserAccountEntity("doctor-" + suffix + "@test.local", "Dr Concurrence", "MEDECIN", "password");
		doctor.setOrganizationId(organization.getId());
		doctor = userAccountRepository.saveAndFlush(doctor);

		TenantContext.setTenantId(organization.getId());
		try {
			patientA = savePatient("DPU-CONC-A-" + suffix, "Patient A");
			patientB = savePatient("DPU-CONC-B-" + suffix, "Patient B");
			availabilityRepository.saveAndFlush(new DoctorAvailabilityEntity(
					doctor, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), appointmentMonday(), null));
		} finally {
			TenantContext.clear();
		}
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
		cleanDatabase();
	}

	@Test
	void shouldAllowExactlyOneBookingWhenTwoPatientsRequestSameSlot() throws Exception {
		Instant slot = at(appointmentMonday(), 8, 0);
		List<AttemptResult> results = executeConcurrently(
				new BookingAttempt(patientA, slot),
				new BookingAttempt(patientB, slot));

		assertEquals(1, results.stream().filter(AttemptResult::success).count());
		assertEquals(1, results.stream().filter(result -> "SLOT_UNAVAILABLE".equals(result.errorCode())).count());
		assertActiveAppointmentCount(1);
	}

	@Test
	void shouldAllowExactlyOneActiveAppointmentPerPatientDoctorAndClinicDay() throws Exception {
		List<AttemptResult> results = executeConcurrently(
				new BookingAttempt(patientA, at(appointmentMonday(), 8, 0)),
				new BookingAttempt(patientA, at(appointmentMonday(), 9, 0)));

		assertEquals(1, results.stream().filter(AttemptResult::success).count());
		assertEquals(1, results.stream()
				.filter(result -> "DUPLICATE_ACTIVE_APPOINTMENT".equals(result.errorCode()))
				.count());
		assertActiveAppointmentCount(1);
	}

	private List<AttemptResult> executeConcurrently(BookingAttempt first, BookingAttempt second) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<AttemptResult> firstFuture = executor.submit(() -> attempt(first, ready, start));
			Future<AttemptResult> secondFuture = executor.submit(() -> attempt(second, ready, start));
			ready.await();
			start.countDown();
			return List.of(firstFuture.get(), secondFuture.get());
		} finally {
			executor.shutdownNow();
		}
	}

	private AttemptResult attempt(BookingAttempt attempt, CountDownLatch ready, CountDownLatch start) {
		TenantContext.setTenantId(organization.getId());
		try {
			ready.countDown();
			start.await();
			patientAppointmentService.book(
					authentication(attempt.patient()),
					new PatientBookAppointmentRequest(doctor.getId(), attempt.startAt(), null));
			return new AttemptResult(true, null);
		} catch (PatientAppointmentApiException ex) {
			return new AttemptResult(false, ex.getCode());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		} finally {
			TenantContext.clear();
		}
	}

	private Authentication authentication(PatientEntity patient) {
		return new UsernamePasswordAuthenticationToken(
				patient.getGlobalPatientNumber(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
	}

	private void assertActiveAppointmentCount(long expected) {
		TenantContext.setTenantId(organization.getId());
		try {
			List<AppointmentEntity> active = new ArrayList<>(appointmentRepository.findAll());
			active.removeIf(appointment -> appointment.getStatus() != AppointmentStatus.CONFIRMED);
			assertEquals(expected, active.size());
			assertTrue(active.stream().allMatch(appointment -> appointment.getActiveStartAt() != null));
		} finally {
			TenantContext.clear();
		}
	}

	private PatientEntity savePatient(String globalNumber, String fullName) {
		return patientRepository.saveAndFlush(new PatientEntity(
				globalNumber,
				"LOCAL-" + UUID.randomUUID().toString().substring(0, 6),
				fullName,
				"MASCULIN",
				LocalDate.of(1990, 1, 1),
				"+237600000000",
				"Douala",
				null, null, null, null, null, null));
	}

	private static LocalDate appointmentMonday() {
		return LocalDate.now(ZONE)
				.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
				.plusWeeks(1);
	}

	private static Instant at(LocalDate date, int hour, int minute) {
		return date.atTime(LocalTime.of(hour, minute)).atZone(ZONE).toInstant();
	}

	private void cleanDatabase() {
		jdbcTemplate.update("DELETE FROM appointments");
		jdbcTemplate.update("DELETE FROM doctor_availability_exceptions");
		jdbcTemplate.update("DELETE FROM doctor_availabilities");
		jdbcTemplate.update("DELETE FROM emergency_admission_requests");
		jdbcTemplate.update("DELETE FROM resuscitation_logs");
		jdbcTemplate.update("DELETE FROM emergencies");
		jdbcTemplate.update("DELETE FROM patient_identity_declarations");
		jdbcTemplate.update("DELETE FROM patient_identity_status_history");
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM medical_documents");
		jdbcTemplate.update("DELETE FROM prescription_items");
		jdbcTemplate.update("DELETE FROM prescriptions");
		jdbcTemplate.update("DELETE FROM consultations");
		jdbcTemplate.update("DELETE FROM visits");
		jdbcTemplate.update("DELETE FROM patients");
		userAccountRepository.deleteAll();
		organizationRepository.deleteAll();
	}

	private record BookingAttempt(PatientEntity patient, Instant startAt) {
	}

	private record AttemptResult(boolean success, String errorCode) {
	}
}
