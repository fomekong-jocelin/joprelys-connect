package com.joprelys.backend.hospitalization.api;

import java.util.UUID;

public record HospitalizationMedicationOption(UUID prescriptionItemId, String medicationName, String dosage,
                                              String posology, String route, String prescriptionNumber) {}
