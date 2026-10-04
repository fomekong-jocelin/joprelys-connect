package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.hospitalization.api.HospitalizationPlacementOptions;
import com.joprelys.backend.hospitalization.api.HospitalizationPlacementOptions.AdmissionVisit;
import com.joprelys.backend.hospitalization.api.HospitalizationPlacementOptions.PractitionerOption;
import com.joprelys.backend.spatial.api.BedResponse;
import java.util.List;
import java.util.UUID;

/** Lectures métier limitées à l'établissement courant, sans mutation de configuration. */
public interface HospitalizationPlacementUseCase {
    HospitalizationPlacementOptions options();
    List<PractitionerOption> practitioners();
    List<BedResponse> beds(UUID spaceId);
    List<AdmissionVisit> visits(UUID patientId);
}
