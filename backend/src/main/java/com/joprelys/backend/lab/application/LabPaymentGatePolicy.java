package com.joprelys.backend.lab.application;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Paiement avant prélèvement (ADR-20261017 D-13) : paramètre d'établissement, jamais appliqué à une urgence
 * en cours ni à un patient hospitalisé, dont la prise en charge ne doit pas attendre le paiement.
 */
@Component
public class LabPaymentGatePolicy {

    private final OrganizationRepository organizationRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final EmergencyRepository emergencyRepository;

    public LabPaymentGatePolicy(
            OrganizationRepository organizationRepository,
            HospitalizationRepository hospitalizationRepository,
            EmergencyRepository emergencyRepository) {
        this.organizationRepository = organizationRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.emergencyRepository = emergencyRepository;
    }

    public boolean requiresPaymentBeforeSampling(UUID organizationId, VisitEntity visit) {
        if (organizationId == null || visit == null) {
            return false;
        }
        boolean enabled = organizationRepository.findById(organizationId)
                .map(organization -> organization.isLabPaymentBeforeSampling())
                .orElse(false);
        if (!enabled || hospitalizationRepository.existsByVisitIdAndStatus(visit.getId(), "EN_COURS")) {
            return false;
        }
        return emergencyRepository.findByPatientIdWithLogs(visit.getPatient().getId()).stream()
                .noneMatch(emergency -> emergency.isActive());
    }
}
