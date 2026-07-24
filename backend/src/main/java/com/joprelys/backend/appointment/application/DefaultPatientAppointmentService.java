package com.joprelys.backend.appointment.application;

import com.joprelys.backend.appointment.api.AppointmentResponse;
import com.joprelys.backend.appointment.api.CancelAppointmentRequest;
import com.joprelys.backend.appointment.api.DoctorDirectoryEntry;
import com.joprelys.backend.appointment.api.PatientBookAppointmentRequest;
import com.joprelys.backend.appointment.api.SlotResponse;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentStatus;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.patient.application.PatientAccessGuardService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implémentation transactionnelle des rendez-vous du portail patient (STORY-2603). */
@Service
public class DefaultPatientAppointmentService implements PatientAppointmentService {

    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.COMPLETED,
            AppointmentStatus.NO_SHOW);

    private final PatientAccessGuardService patientAccessGuardService;
    private final UserAccountRepository userAccountRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final AppointmentProperties properties;
    private final StaffSpecialtyAssignmentRepository specialtyAssignmentRepository;
    private final StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository;
    private final MedicalSpecialtyCatalogRepository specialtyCatalogRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final HospitalServiceCatalogRepository serviceCatalogRepository;

    public DefaultPatientAppointmentService(
            PatientAccessGuardService patientAccessGuardService,
            UserAccountRepository userAccountRepository,
            AppointmentRepository appointmentRepository,
            AvailabilityService availabilityService,
            AppointmentProperties properties,
            StaffSpecialtyAssignmentRepository specialtyAssignmentRepository,
            StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository,
            MedicalSpecialtyCatalogRepository specialtyCatalogRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            HospitalServiceCatalogRepository serviceCatalogRepository) {
        this.patientAccessGuardService = patientAccessGuardService;
        this.userAccountRepository = userAccountRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.properties = properties;
        this.specialtyAssignmentRepository = specialtyAssignmentRepository;
        this.unitAssignmentRepository = unitAssignmentRepository;
        this.specialtyCatalogRepository = specialtyCatalogRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.serviceCatalogRepository = serviceCatalogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorDirectoryEntry> listDoctors(
            Authentication authentication,
            String specialtyCode,
            UUID organizationalUnitId) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        Instant now = Instant.now();
        String normalizedSpecialtyCode = normalizeCode(specialtyCode);

        return userAccountRepository
                .findAllByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(patient.getOrganizationId())
                .stream()
                .filter(doctor -> doctor.hasRole("MEDECIN"))
                .map(doctor -> directoryEntry(doctor, patient.getOrganizationId(), now))
                .filter(entry -> normalizedSpecialtyCode == null
                        || entry.specialties().stream().anyMatch(item -> item.code().equals(normalizedSpecialtyCode)))
                .filter(entry -> organizationalUnitId == null
                        || entry.units().stream().anyMatch(item -> item.id().equals(organizationalUnitId)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponse> listSlots(
            Authentication authentication,
            UUID doctorId,
            Instant from,
            Instant to) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        loadDoctor(doctorId, patient.getOrganizationId(), false);
        Instant now = Instant.now();
        validateRange(from, to);
        Instant horizon = bookingHorizon(now);
        if (from.isAfter(horizon)) {
            throw bookingHorizonExceeded();
        }
        Instant effectiveFrom = from.isAfter(now) ? from : now;
        Instant effectiveTo = to.isBefore(horizon.plus(slotDuration()))
                ? to
                : horizon.plus(slotDuration());
        if (!effectiveTo.isAfter(effectiveFrom)) {
            return List.of();
        }
        return availabilityService.generateSlots(doctorId, effectiveFrom, effectiveTo)
                .stream()
                .filter(slot -> !slot.startAt().isBefore(effectiveFrom))
                .filter(slot -> !slot.startAt().isAfter(horizon))
                .map(slot -> SlotResponse.fromSlot(doctorId, slot))
                .toList();
    }

    @Override
    @Transactional
    public AppointmentResponse book(Authentication authentication, PatientBookAppointmentRequest request) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        UserAccountEntity doctor = loadDoctor(request.doctorId(), patient.getOrganizationId(), true);
        Instant now = Instant.now();
        validateBookingStart(request.startAt(), now);
        Instant endAt = request.startAt().plus(slotDuration());
        assertNoDuplicateDay(patient.getId(), doctor.getId(), request.startAt());
        assertSlotAvailable(doctor.getId(), request.startAt(), endAt);

        AppointmentEntity appointment = new AppointmentEntity(
                doctor,
                patient,
                request.startAt(),
                endAt,
                normalizeReason(request.reason()));
        try {
            return AppointmentResponse.fromEntity(appointmentRepository.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException ex) {
            throw slotUnavailable();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listOwn(Authentication authentication, Instant from, Instant to) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        if (from != null && to != null) {
            validateRange(from, to);
            return appointmentRepository
                    .findByPatientIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtDesc(
                            patient.getId(), from, to)
                    .stream()
                    .map(AppointmentResponse::fromEntity)
                    .toList();
        }
        return appointmentRepository.findByPatientIdOrderByStartAtDesc(patient.getId())
                .stream()
                .filter(appointment -> from == null || !appointment.getStartAt().isBefore(from))
                .filter(appointment -> to == null || appointment.getStartAt().isBefore(to))
                .map(AppointmentResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(
            Authentication authentication,
            UUID appointmentId,
            CancelAppointmentRequest request) {
        PatientEntity patient = patientAccessGuardService.resolve(authentication);
        AppointmentEntity appointment = appointmentRepository.findByIdAndPatientId(appointmentId, patient.getId())
                .orElseThrow(DefaultPatientAppointmentService::appointmentNotFound);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw invalidStatusTransition();
        }
        if (!AppointmentTimePolicy.canPatientCancel(
                appointment.getStartAt(),
                Instant.now(),
                properties.patientCancelDeadlineHours())) {
            throw cancelDeadlinePassed();
        }
        String reason = request == null ? null : normalizeCancellationReason(request.cancellationReason());
        appointment.cancel(AppointmentStatus.CANCELLED_BY_PATIENT, reason);
        return AppointmentResponse.fromEntity(appointmentRepository.saveAndFlush(appointment));
    }

    private DoctorDirectoryEntry directoryEntry(UserAccountEntity doctor, UUID organizationId, Instant now) {
        List<DoctorDirectoryEntry.SpecialtyEntry> specialties = specialtyAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, doctor.getId())
                .stream()
                .filter(item -> item.activeAt(now))
                .map(this::specialtyEntry)
                .toList();

        List<DoctorDirectoryEntry.UnitEntry> units = unitAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, doctor.getId())
                .stream()
                .filter(item -> item.activeAt(now))
                .map(item -> unitEntry(item, organizationId))
                .toList();

        return new DoctorDirectoryEntry(doctor.getId(), doctor.getDisplayName(), specialties, units);
    }

    private DoctorDirectoryEntry.SpecialtyEntry specialtyEntry(StaffSpecialtyAssignmentEntity assignment) {
        MedicalSpecialtyCatalogEntity catalog = specialtyCatalogRepository.findById(assignment.getSpecialtyCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Catalogue spécialité introuvable : " + assignment.getSpecialtyCode()));
        return new DoctorDirectoryEntry.SpecialtyEntry(
                catalog.getCode(),
                catalog.getNameFr(),
                catalog.getNameEn(),
                assignment.isPrimary());
    }

    private DoctorDirectoryEntry.UnitEntry unitEntry(
            StaffOrganizationalUnitAssignmentEntity assignment,
            UUID organizationId) {
        OrganizationalUnitEntity unit = organizationalUnitRepository
                .findByIdAndOrganizationId(assignment.getOrganizationalUnitId(), organizationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Unité organisationnelle introuvable : " + assignment.getOrganizationalUnitId()));

        String nameFr = unit.getName();
        String nameEn = unit.getName();
        if (unit.getServiceCatalogCode() != null) {
            HospitalServiceCatalogEntity service = serviceCatalogRepository.findById(unit.getServiceCatalogCode())
                    .orElseThrow(() -> new IllegalStateException(
                            "Catalogue service introuvable : " + unit.getServiceCatalogCode()));
            nameFr = service.getNameFr();
            nameEn = service.getNameEn();
        }
        if (nameFr == null || nameFr.isBlank()) {
            nameFr = unit.getCode();
            nameEn = unit.getCode();
        }

        return new DoctorDirectoryEntry.UnitEntry(
                unit.getId(),
                unit.getCode(),
                nameFr,
                nameEn,
                assignment.isPrimary());
    }

    private UserAccountEntity loadDoctor(UUID doctorId, UUID organizationId, boolean lock) {
        if (doctorId == null) {
            throw doctorNotFound();
        }
        UserAccountEntity doctor = (lock
                ? userAccountRepository.findByIdAndOrganizationIdForUpdate(doctorId, organizationId)
                : userAccountRepository.findByIdAndOrganizationId(doctorId, organizationId))
                .orElseThrow(DefaultPatientAppointmentService::doctorNotFound);
        if (!doctor.isEnabled() || !doctor.hasRole("MEDECIN")) {
            throw doctorNotFound();
        }
        return doctor;
    }

    private void validateBookingStart(Instant startAt, Instant now) {
        if (startAt == null || !startAt.isAfter(now)) {
            throw new PatientAppointmentApiException(
                    HttpStatus.BAD_REQUEST,
                    "PAST_SLOT",
                    "Le créneau doit être strictement dans le futur.");
        }
        if (startAt.isAfter(bookingHorizon(now))) {
            throw bookingHorizonExceeded();
        }
    }

    private void assertNoDuplicateDay(UUID patientId, UUID doctorId, Instant startAt) {
        ZoneId zone = ZoneId.of(properties.timezone());
        LocalDate localDate = startAt.atZone(zone).toLocalDate();
        Instant dayStart = localDate.atStartOfDay(zone).toInstant();
        Instant dayEnd = localDate.plusDays(1).atStartOfDay(zone).toInstant();
        if (appointmentRepository
                .existsByPatientIdAndDoctorIdAndStartAtGreaterThanEqualAndStartAtLessThanAndStatusIn(
                        patientId, doctorId, dayStart, dayEnd, ACTIVE_STATUSES)) {
            throw new PatientAppointmentApiException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_ACTIVE_APPOINTMENT",
                    "Un rendez-vous actif existe déjà avec ce médecin pour cette journée.");
        }
    }

    private void assertSlotAvailable(UUID doctorId, Instant startAt, Instant endAt) {
        boolean available = availabilityService.generateSlots(doctorId, startAt, endAt)
                .stream()
                .anyMatch(slot -> slot.startAt().equals(startAt) && slot.endAt().equals(endAt));
        if (!available) {
            throw slotUnavailable();
        }
    }

    private Instant bookingHorizon(Instant now) {
        return now.plus(Duration.ofDays(properties.bookingHorizonDays()));
    }

    private Duration slotDuration() {
        return Duration.ofMinutes(properties.defaultSlotDurationMinutes());
    }

    private static void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new PatientAppointmentApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "La période demandée est invalide.");
        }
    }

    private static String normalizeCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeReason(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeCancellationReason(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static PatientAppointmentApiException doctorNotFound() {
        return new PatientAppointmentApiException(
                HttpStatus.NOT_FOUND,
                "DOCTOR_NOT_FOUND",
                "Médecin introuvable dans cet établissement.");
    }

    private static PatientAppointmentApiException appointmentNotFound() {
        return new PatientAppointmentApiException(
                HttpStatus.NOT_FOUND,
                "APPOINTMENT_NOT_FOUND",
                "Rendez-vous introuvable.");
    }

    private static PatientAppointmentApiException slotUnavailable() {
        return new PatientAppointmentApiException(
                HttpStatus.CONFLICT,
                "SLOT_UNAVAILABLE",
                "Ce créneau n'est plus disponible.");
    }

    private static PatientAppointmentApiException bookingHorizonExceeded() {
        return new PatientAppointmentApiException(
                HttpStatus.BAD_REQUEST,
                "BOOKING_HORIZON_EXCEEDED",
                "Le créneau dépasse l'horizon de réservation autorisé.");
    }

    private static PatientAppointmentApiException cancelDeadlinePassed() {
        return new PatientAppointmentApiException(
                HttpStatus.BAD_REQUEST,
                "CANCEL_DEADLINE_PASSED",
                "Le délai autorisé pour annuler ce rendez-vous est dépassé.");
    }

    private static PatientAppointmentApiException invalidStatusTransition() {
        return new PatientAppointmentApiException(
                HttpStatus.CONFLICT,
                "INVALID_STATUS_TRANSITION",
                "Ce rendez-vous ne peut plus être annulé.");
    }
}
