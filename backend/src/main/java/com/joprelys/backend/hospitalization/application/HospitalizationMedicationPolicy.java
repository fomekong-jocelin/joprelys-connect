package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.hospitalization.api.CreateMedicationAdministrationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationMedicationOption;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HospitalizationMedicationPolicy implements HospitalizationMedicationUseCase {
    private final PrescriptionRepository prescriptions;
    private final HospitalizationRepository hospitalizations;
    private final PatientCanonicalResolver patients;

    public HospitalizationMedicationPolicy(PrescriptionRepository prescriptions,
            HospitalizationRepository hospitalizations, PatientCanonicalResolver patients) {
        this.prescriptions = prescriptions;
        this.hospitalizations = hospitalizations;
        this.patients = patients;
    }

    @Transactional(readOnly = true)
    public List<HospitalizationMedicationOption> options(UUID hospitalizationId) {
        HospitalizationEntity stay = hospitalizations.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Séjour introuvable."));
        requireActive(stay);
        return eligibleItems(stay).stream()
                .map(item -> new HospitalizationMedicationOption(item.getId(), item.getDrugName(), item.getDosage(),
                        item.getPosology(), item.getRoute(), item.getPrescription().getPrescriptionNumber()))
                .toList();
    }

    @Transactional
    public PrescriptionItemEntity validate(HospitalizationEntity stay, CreateMedicationAdministrationRequest request) {
        requireActive(stay);
        if (request.prescriptionItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une prescription validée est obligatoire.");
        }
        var patient = patients.resolve(stay.getPatientId());
        var lockedPrescriptions = prescriptions.lockMedicationPrescriptionsForPatients(
                stay.getOrganizationId(), patient.contributingPatientIds());
        Instant now = Instant.now();
        PrescriptionItemEntity item = lockedPrescriptions.stream()
                .filter(prescription -> "ACTIVE".equals(prescription.getStatus()))
                .filter(prescription -> prescription.getExpiresAt() == null || prescription.getExpiresAt().isAfter(now))
                .flatMap(prescription -> prescription.getItems().stream())
                .filter(candidate -> candidate.getId().equals(request.prescriptionItemId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "La prescription n'est pas admissible pour ce séjour. Actualisez les médicaments prescrits."));
        if (request.medicationName() == null || !item.getDrugName().trim().equalsIgnoreCase(request.medicationName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Le médicament administré ne correspond pas à la prescription.");
        }
        return item;
    }

    private List<PrescriptionItemEntity> eligibleItems(HospitalizationEntity stay) {
        var patient = patients.resolve(stay.getPatientId());
        Instant now = Instant.now();
        return prescriptions.findMedicationItemsForPatients(stay.getOrganizationId(), patient.contributingPatientIds())
                .stream().filter(item -> "ACTIVE".equals(item.getPrescription().getStatus()))
                .filter(item -> item.getPrescription().getExpiresAt() == null
                        || item.getPrescription().getExpiresAt().isAfter(now)).toList();
    }

    private void requireActive(HospitalizationEntity stay) {
        if (!"EN_COURS".equals(stay.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le séjour est clôturé.");
        }
    }

}
