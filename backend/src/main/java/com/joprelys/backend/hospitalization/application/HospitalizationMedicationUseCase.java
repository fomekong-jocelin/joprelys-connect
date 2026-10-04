package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.hospitalization.api.CreateMedicationAdministrationRequest;
import com.joprelys.backend.hospitalization.api.HospitalizationMedicationOption;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import java.util.List;
import java.util.UUID;

/** Prescription admissible et validation atomique de sa référence avant administration. */
public interface HospitalizationMedicationUseCase {
    List<HospitalizationMedicationOption> options(UUID hospitalizationId);
    PrescriptionItemEntity validate(HospitalizationEntity stay, CreateMedicationAdministrationRequest request);
}
