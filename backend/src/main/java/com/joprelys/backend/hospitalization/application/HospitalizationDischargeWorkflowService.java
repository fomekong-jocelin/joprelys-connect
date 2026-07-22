package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.api.ConfirmPhysicalDepartureRequest;
import com.joprelys.backend.hospitalization.api.DischargeHospitalizationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.spatial.application.BedStateChangeService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateAxis;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeSource;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateReasonCode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HospitalizationDischargeWorkflowService {

    private static final String ACTIVE_STATUS = "EN_COURS";

    private final HospitalizationRepository hospitalizationRepository;
    private final HospitalizationService hospitalizationService;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final BedStateChangeService bedStateChangeService;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public HospitalizationDischargeWorkflowService(
            HospitalizationRepository hospitalizationRepository,
            HospitalizationService hospitalizationService,
            BedAssignmentRepository bedAssignmentRepository,
            BedStateChangeService bedStateChangeService,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.hospitalizationService = hospitalizationService;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.bedStateChangeService = bedStateChangeService;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public HospitalizationResponse decideDischarge(UUID hospitalizationId, DischargeHospitalizationRequest request) {
        HospitalizationEntity hospitalization = requireActiveHospitalization(hospitalizationId);
        if (hospitalization.hasDischargeDecision()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une décision médicale de sortie existe déjà pour cette hospitalisation.");
        }

        UserAccountEntity actor = currentUserOrNull();
        boolean againstMedicalAdvice = Boolean.TRUE.equals(request.againstMedicalAdvice());
        hospitalization.decideDischarge(
                request.dischargeDiagnosis().trim(),
                request.dischargeInstructions().trim(),
                againstMedicalAdvice,
                actor == null ? null : actor.getId(),
                Instant.now());
        HospitalizationEntity saved = hospitalizationRepository.save(hospitalization);

        audit(
                actor,
                saved,
                againstMedicalAdvice ? "DECIDE_DISCHARGE_AGAINST_ADVICE" : "DECIDE_DISCHARGE",
                "Décision médicale de sortie enregistrée. Le patient reste physiquement hospitalisé et le lit demeure affecté.");
        return HospitalizationResponse.fromEntity(saved);
    }

    @Transactional
    public HospitalizationResponse confirmPhysicalDeparture(
            UUID hospitalizationId,
            ConfirmPhysicalDepartureRequest request) {
        if (!Boolean.TRUE.equals(request.confirmed())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le départ physique doit être explicitement confirmé.");
        }

        HospitalizationEntity hospitalization = requireActiveHospitalization(hospitalizationId);
        if (!hospitalization.hasDischargeDecision()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le départ physique ne peut pas être confirmé avant la décision médicale de sortie.");
        }
        if (hospitalization.hasPhysicalDeparture()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le départ physique a déjà été confirmé.");
        }

        BedEntity departingBed = bedAssignmentRepository.findActiveByHospitalizationId(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Aucune affectation de lit active n'est associée au séjour."))
                .getBed();
        BedReadinessStatus previousReadiness = departingBed.getReadinessStatus();
        if (previousReadiness != BedReadinessStatus.READY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le lit affecté présente un état de préparation incohérent avant le départ physique.");
        }

        UserAccountEntity actor = currentUserOrNull();
        String departureNote = normalizedNote(request.note());
        hospitalization.confirmPhysicalDeparture(
                actor == null ? null : actor.getId(),
                departureNote,
                Instant.now());
        hospitalizationRepository.save(hospitalization);

        bedStateChangeService.recordSystem(
                departingBed,
                BedStateAxis.READINESS,
                previousReadiness.name(),
                BedReadinessStatus.CLEANING.name(),
                BedStateReasonCode.CLEANING_AFTER_DEPARTURE,
                departureNote == null
                        ? "Nettoyage déclenché après le départ physique du séjour " + hospitalizationId
                        : departureNote,
                BedStateChangeSource.SYSTEM_PHYSICAL_DEPARTURE);

        HospitalizationResponse response = hospitalizationService.dischargePatient(
                hospitalizationId,
                new DischargeHospitalizationRequest(
                        hospitalization.getDischargeDiagnosis(),
                        hospitalization.getDischargeInstructions(),
                        hospitalization.getDischargeAgainstMedicalAdvice()));

        HospitalizationEntity saved = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new IllegalStateException(
                        "L'hospitalisation a disparu pendant la confirmation du départ physique."));
        audit(
                actor,
                saved,
                "CONFIRM_PHYSICAL_DEPARTURE",
                "Départ physique confirmé. L'affectation du lit est clôturée et le nettoyage peut démarrer.");
        return response;
    }

    private HospitalizationEntity requireActiveHospitalization(UUID hospitalizationId) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hospitalisation introuvable"));
        if (!ACTIVE_STATUS.equals(hospitalization.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'hospitalisation est déjà physiquement clôturée.");
        }
        return hospitalization;
    }

    private String normalizedNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }

    private UserAccountEntity currentUserOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .orElse(null);
    }

    private void audit(
            UserAccountEntity actor,
            HospitalizationEntity hospitalization,
            String action,
            String details) {
        if (actor == null) {
            return;
        }
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                hospitalization.getPatientId(),
                "HOSPITALIZATION",
                hospitalization.getId(),
                action,
                details);
    }
}
