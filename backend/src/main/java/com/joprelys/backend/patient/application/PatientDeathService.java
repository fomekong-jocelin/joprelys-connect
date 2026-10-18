package com.joprelys.backend.patient.application;

import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentEntity;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentRepository;
import com.joprelys.backend.appointment.infrastructure.persistence.AppointmentStatus;
import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Déclaration du décès d'un patient et effets sur ses épisodes en cours (ADR-20261017 D-07).
 *
 * <p>Les brouillons cliniques d'autres praticiens ne sont pas signés : les visites sont clôturées en l'état.
 * Le séjour hospitalier reçoit une décision de sortie « décès » ; le lit est libéré au départ du corps,
 * par le circuit de départ physique existant.</p>
 */
@Service
public class PatientDeathService {

    public static final String DEATH_ORIENTATION = "DEATH";
    private static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(5);
    private static final String ACTIVE_VISIT = "EN_COURS";

    private final PatientCanonicalResolver canonicalResolver;
    private final EmergencyRepository emergencyRepository;
    private final VisitRepository visitRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    public PatientDeathService(
            PatientCanonicalResolver canonicalResolver,
            EmergencyRepository emergencyRepository,
            VisitRepository visitRepository,
            HospitalizationRepository hospitalizationRepository,
            AppointmentRepository appointmentRepository,
            AuditService auditService) {
        this.canonicalResolver = canonicalResolver;
        this.emergencyRepository = emergencyRepository;
        this.visitRepository = visitRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PatientEntity declareDeath(UUID patientId, DeathDeclaration declaration, UUID actorId, UUID actorOrganizationId) {
        var context = canonicalResolver.resolve(patientId);
        PatientEntity patient = context.canonicalPatient();
        if (patient.isDeceased()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le décès de ce patient est déjà déclaré.");
        }
        Instant now = Instant.now();
        Instant deceasedAt = declaration.deceasedAt() == null ? now : declaration.deceasedAt();
        if (deceasedAt.isAfter(now.plus(CLOCK_TOLERANCE))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure du décès ne peut pas être dans le futur.");
        }
        if (patient.getBirthDate() != null
                && deceasedAt.isBefore(patient.getBirthDate().atStartOfDay().toInstant(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'heure du décès précède la date de naissance.");
        }

        patient.declareDeath(deceasedAt, normalize(declaration.place()), normalize(declaration.circumstances()), actorId, now);

        for (EmergencyEntity emergency : emergencyRepository.findByPatientIdsWithLogs(context.contributingPatientIds())) {
            if (emergency.isActive()) {
                emergency.decideOrientation(DEATH_ORIENTATION, actorId, now);
                emergency.closeEpisode(actorId, now);
            }
        }

        hospitalizationRepository.findActiveByPatientIds(context.contributingPatientIds()).ifPresent(stay -> {
            if (!stay.hasDischargeDecision()) {
                stay.decideDischarge("Décès", "Décès constaté le " + deceasedAt + ".", false, actorId, now);
            }
        });

        for (UUID contributingId : context.contributingPatientIds()) {
            for (VisitEntity visit : visitRepository.findByPatientId(contributingId)) {
                // La visite liée au séjour reste ouverte jusqu'au départ du corps (D-08).
                if (ACTIVE_VISIT.equals(visit.getStatus())
                        && !hospitalizationRepository.existsByVisitIdAndStatus(visit.getId(), ACTIVE_VISIT)) {
                    visit.setStatus("TERMINEE");
                    visit.setClosedAt(now);
                }
            }
            for (AppointmentEntity appointment : appointmentRepository.findByPatientIdOrderByStartAtDesc(contributingId)) {
                if (appointment.getStatus() == AppointmentStatus.CONFIRMED && appointment.getStartAt().isAfter(now)) {
                    appointment.cancel(AppointmentStatus.CANCELLED_BY_CLINIC, "Décès du patient");
                }
            }
        }

        auditService.logSuccess(actorId, actorOrganizationId, patient.getId(), "PATIENT", patient.getId(),
                "PATIENT_DEATH_DECLARED", "Décès constaté le " + deceasedAt);
        return patient;
    }

    @Transactional
    public PatientEntity revokeDeath(UUID patientId, String reason, UUID actorId, UUID actorOrganizationId) {
        String normalizedReason = normalize(reason);
        if (normalizedReason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif d'annulation de la déclaration est obligatoire.");
        }
        PatientEntity patient = canonicalResolver.resolve(patientId).canonicalPatient();
        if (!patient.isDeceased()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aucun décès n'est déclaré pour ce patient.");
        }
        Instant revokedDeceasedAt = patient.getDeceasedAt();
        patient.revokeDeath();
        auditService.logSuccess(actorId, actorOrganizationId, patient.getId(), "PATIENT", patient.getId(),
                "PATIENT_DEATH_REVOKED", "Déclaration du " + revokedDeceasedAt + " annulée : " + normalizedReason);
        return patient;
    }

    /** Refuse l'ouverture d'un nouvel épisode pour un patient décédé. */
    public static void rejectIfDeceased(PatientEntity patient) {
        if (patient != null && patient.isDeceased()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce patient est déclaré décédé.");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DeathDeclaration(Instant deceasedAt, String place, String circumstances) {
    }
}
