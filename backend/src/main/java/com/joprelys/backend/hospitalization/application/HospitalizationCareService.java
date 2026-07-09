package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.api.*;
import com.joprelys.backend.hospitalization.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class HospitalizationCareService {

    private final HospitalizationRepository hospitalizationRepository;
    private final HospitalizationDailyCareRepository dailyCareRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final PatientConsumptionRepository patientConsumptionRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public HospitalizationCareService(HospitalizationRepository hospitalizationRepository,
                                      HospitalizationDailyCareRepository dailyCareRepository,
                                      MedicationAdministrationRepository medicationAdministrationRepository,
                                      PatientConsumptionRepository patientConsumptionRepository,
                                      UserAccountRepository userAccountRepository,
                                      AuditService auditService) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.dailyCareRepository = dailyCareRepository;
        this.medicationAdministrationRepository = medicationAdministrationRepository;
        this.patientConsumptionRepository = patientConsumptionRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DailyCareResponse addDailyCare(UUID hospitalizationId, CreateDailyCareRequest request) {
        HospitalizationEntity hosp = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hosp.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'ajouter des soins à une hospitalisation clôturée");
        }

        UserAccountEntity actor = getCurrentUser();
        String performedBy = actor != null ? actor.getDisplayName() : "Inconnu";
        Instant performedAt = request.performedAt() != null ? request.performedAt() : Instant.now();

        HospitalizationDailyCareEntity care = new HospitalizationDailyCareEntity(
                hospitalizationId,
                request.careType(),
                request.description(),
                request.billable(),
                request.price(),
                performedBy,
                performedAt
        );

        HospitalizationDailyCareEntity saved = dailyCareRepository.save(care);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hosp.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "ADD_DAILY_CARE",
                    "Ajout d'un soin journalier : " + request.careType()
            );
        }

        return DailyCareResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DailyCareResponse> getDailyCares(UUID hospitalizationId) {
        hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        return dailyCareRepository.findByHospitalizationIdOrderByPerformedAtDesc(hospitalizationId).stream()
                .map(DailyCareResponse::fromEntity)
                .toList();
    }

    @Transactional
    public MedicationAdministrationResponse addMedicationAdministration(UUID hospitalizationId, CreateMedicationAdministrationRequest request) {
        HospitalizationEntity hosp = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hosp.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'ajouter des administrations à une hospitalisation clôturée");
        }

        UserAccountEntity actor = getCurrentUser();
        String administeredBy = actor != null ? actor.getDisplayName() : "Inconnu";
        Instant administeredAt = request.administeredAt() != null ? request.administeredAt() : Instant.now();

        MedicationAdministrationEntity admin = new MedicationAdministrationEntity(
                hospitalizationId,
                request.prescriptionItemId(),
                request.medicationName(),
                request.dose(),
                administeredBy,
                administeredAt
        );

        MedicationAdministrationEntity saved = medicationAdministrationRepository.save(admin);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hosp.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "ADD_MEDICATION_ADMINISTRATION",
                    "Administration de médicament : " + request.medicationName() + " (" + request.dose() + ")"
            );
        }

        return MedicationAdministrationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<MedicationAdministrationResponse> getMedicationAdministrations(UUID hospitalizationId) {
        hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        return medicationAdministrationRepository.findByHospitalizationIdOrderByAdministeredAtDesc(hospitalizationId).stream()
                .map(MedicationAdministrationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PatientConsumptionResponse addPatientConsumption(UUID hospitalizationId, CreatePatientConsumptionRequest request) {
        HospitalizationEntity hosp = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hosp.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'ajouter des consommations à une hospitalisation clôturée");
        }

        UserAccountEntity actor = getCurrentUser();
        String consumedBy = actor != null ? actor.getDisplayName() : "Inconnu";
        Instant consumedAt = request.consumedAt() != null ? request.consumedAt() : Instant.now();

        PatientConsumptionEntity consumption = new PatientConsumptionEntity(
                hospitalizationId,
                request.itemName(),
                request.quantity(),
                request.unitPrice(),
                consumedBy,
                consumedAt
        );

        PatientConsumptionEntity saved = patientConsumptionRepository.save(consumption);

        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hosp.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "ADD_PATIENT_CONSUMPTION",
                    "Consommation rattachée : " + request.itemName() + " (Qté: " + request.quantity() + ")"
            );
        }

        return PatientConsumptionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<PatientConsumptionResponse> getPatientConsumptions(UUID hospitalizationId) {
        hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        return patientConsumptionRepository.findByHospitalizationIdOrderByConsumedAtDesc(hospitalizationId).stream()
                .map(PatientConsumptionResponse::fromEntity)
                .toList();
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
