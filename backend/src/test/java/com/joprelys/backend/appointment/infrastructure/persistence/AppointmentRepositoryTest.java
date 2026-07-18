package com.joprelys.backend.appointment.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Autowired
    private DoctorAvailabilityExceptionRepository doctorAvailabilityExceptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private UserAccountEntity doctor;
    private PatientEntity patient;

    @BeforeEach
    void setUp() {
        cleanAppointmentTables();
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
        organization = organizationRepository.save(
                new OrganizationEntity("Clinique RDV", "rdv@joprelys.local", "321", "Street RDV", "Douala"));
        TenantContext.setTenantId(organization.getId());
        doctor = new UserAccountEntity("dr.rdv@joprelys.local", "Dr RDV", "MEDECIN", "hash");
        doctor.setOrganizationId(organization.getId());
        doctor = userAccountRepository.save(doctor);
        patient = patientRepository.save(new PatientEntity(
                "DPU-RDV-1", "PAT-RDV-1", "Patient RDV", "MASCULIN",
                LocalDate.of(1990, 1, 1), "+237600000010", "Douala", null, null, null, null, null, null));
    }

    @AfterEach
    void tearDown() {
        cleanAppointmentTables();
        TenantContext.clear();
    }

    private void cleanAppointmentTables() {
        jdbcTemplate.update("DELETE FROM appointments");
        jdbcTemplate.update("DELETE FROM doctor_availability_exceptions");
        jdbcTemplate.update("DELETE FROM doctor_availabilities");
    }

    @Test
    void shouldPersistAndReloadAvailabilityExceptionAndAppointment() {
        DoctorAvailabilityEntity availability = doctorAvailabilityRepository.save(new DoctorAvailabilityEntity(
                doctor, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), LocalDate.of(2026, 8, 1), null));

        DoctorAvailabilityEntity reloadedAvailability = doctorAvailabilityRepository
                .findById(availability.getId())
                .orElseThrow(() -> new AssertionError("Disponibilité introuvable après rechargement"));
        assertEquals(organization.getId(), reloadedAvailability.getOrganizationId(),
                "Le tenant doit être posé automatiquement");
        assertEquals(doctor.getId(), reloadedAvailability.getDoctor().getId());
        assertEquals(1, reloadedAvailability.getWeekday());
        assertEquals(LocalTime.of(8, 0), reloadedAvailability.getStartTime());
        assertEquals(LocalTime.of(12, 0), reloadedAvailability.getEndTime());
        assertEquals(LocalDate.of(2026, 8, 1), reloadedAvailability.getValidFrom());
        assertNull(reloadedAvailability.getValidTo());
        assertTrue(reloadedAvailability.getActive(), "Une règle créée doit être active");
        assertNotNull(reloadedAvailability.getCreatedAt());
        assertNotNull(reloadedAvailability.getUpdatedAt());

        doctorAvailabilityExceptionRepository.save(new DoctorAvailabilityExceptionEntity(
                doctor, Instant.parse("2026-08-05T08:00:00Z"), Instant.parse("2026-08-05T12:00:00Z"), "Congé"));
        assertEquals(1, doctorAvailabilityExceptionRepository
                .findByDoctorIdAndStartAtLessThanAndEndAtGreaterThan(
                        doctor.getId(), Instant.parse("2026-08-05T09:00:00Z"), Instant.parse("2026-08-05T07:00:00Z"))
                .size(), "L'exception doit être retrouvée sur sa période de chevauchement");

        Instant startAt = Instant.parse("2026-08-03T08:00:00Z");
        AppointmentEntity appointment = appointmentRepository.save(new AppointmentEntity(
                doctor, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Consultation de suivi"));

        AppointmentEntity reloaded = appointmentRepository.findById(appointment.getId())
                .orElseThrow(() -> new AssertionError("Rendez-vous introuvable après rechargement"));
        assertEquals(organization.getId(), reloaded.getOrganizationId());
        assertEquals(doctor.getId(), reloaded.getDoctor().getId());
        assertEquals(patient.getId(), reloaded.getPatient().getId());
        assertEquals(startAt, reloaded.getStartAt());
        assertEquals(startAt.plus(30, ChronoUnit.MINUTES), reloaded.getEndAt());
        assertEquals(AppointmentStatus.CONFIRMED, reloaded.getStatus(),
                "Le statut par défaut doit être CONFIRMED");
        assertEquals(startAt, reloaded.getActiveStartAt(),
                "Un rendez-vous confirmé doit occuper son créneau");
        assertEquals("Consultation de suivi", reloaded.getReason());
        assertNull(reloaded.getVisit());

        assertEquals(1, doctorAvailabilityRepository
                .findByDoctorIdAndWeekdayAndActiveTrue(doctor.getId(), 1).size());
        assertEquals(1, doctorAvailabilityRepository
                .findByDoctorIdAndActiveTrue(doctor.getId()).size());
        assertEquals(1, appointmentRepository
                .findByDoctorIdAndStartAtBetweenOrderByStartAtAsc(
                        doctor.getId(), startAt.minus(1, ChronoUnit.HOURS), startAt.plus(1, ChronoUnit.HOURS))
                .size());
        assertTrue(appointmentRepository.existsByDoctorIdAndStartAtAndStatusIn(
                doctor.getId(), startAt, List.of(AppointmentStatus.CONFIRMED)));
    }

    @Test
    void shouldReleaseSlotWhenAppointmentIsCancelled() {
        Instant startAt = Instant.parse("2026-08-03T09:00:00Z");
        AppointmentEntity appointment = appointmentRepository.saveAndFlush(new AppointmentEntity(
                doctor, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Consultation"));

        appointment.cancel(AppointmentStatus.CANCELLED_BY_PATIENT, "Empêchement");
        appointmentRepository.saveAndFlush(appointment);

        AppointmentEntity reloaded = appointmentRepository.findById(appointment.getId())
                .orElseThrow(() -> new AssertionError("Rendez-vous introuvable après annulation"));
        assertEquals(AppointmentStatus.CANCELLED_BY_PATIENT, reloaded.getStatus());
        assertNull(reloaded.getActiveStartAt(), "Un rendez-vous annulé doit libérer le créneau");
        assertNotNull(reloaded.getCancelledAt());
        assertEquals("Empêchement", reloaded.getCancellationReason());

        // Le créneau libéré redevient réservable (NULL multiples autorisés par l'index unique).
        AppointmentEntity rebooked = appointmentRepository.saveAndFlush(new AppointmentEntity(
                doctor, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Nouvelle réservation"));
        assertNotNull(rebooked.getId());
        assertTrue(appointmentRepository.existsByDoctorIdAndStartAtAndStatusIn(
                doctor.getId(), startAt, List.of(AppointmentStatus.CONFIRMED)),
                "Le créneau libéré doit être de nouveau réservable");
    }

    @Test
    void shouldRejectTwoActiveAppointmentsOnSameSlot() {
        Instant startAt = Instant.parse("2026-08-03T10:00:00Z");
        appointmentRepository.saveAndFlush(new AppointmentEntity(
                doctor, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Premier rendez-vous"));

        AppointmentEntity duplicate = new AppointmentEntity(
                doctor, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Doublon");
        assertThrows(DataIntegrityViolationException.class,
                () -> appointmentRepository.saveAndFlush(duplicate),
                "L'index unique doit interdire deux rendez-vous actifs sur le même créneau");
    }

    @Test
    void shouldIsolateAppointmentsPerTenant() {
        Instant startAt = Instant.parse("2026-08-03T11:00:00Z");
        appointmentRepository.saveAndFlush(new AppointmentEntity(
                doctor, patient, startAt, startAt.plus(30, ChronoUnit.MINUTES), "Rendez-vous clinique A"));

        OrganizationEntity otherOrganization = organizationRepository.save(
                new OrganizationEntity("Clinique B", "clinique.b@joprelys.local", "654", "Street B", "Yaoundé"));
        TenantContext.setTenantId(otherOrganization.getId());
        try {
            assertTrue(appointmentRepository.findByPatientIdOrderByStartAtDesc(patient.getId()).isEmpty(),
                    "Un autre tenant ne doit voir aucun rendez-vous du patient");
            assertTrue(appointmentRepository.findAll().isEmpty(),
                    "findAll doit rester filtré par tenant");
        } finally {
            TenantContext.setTenantId(organization.getId());
        }
    }
}
