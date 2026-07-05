package com.joprelys.backend.fhir;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.fhir.model.*;

import java.util.ArrayList;
import java.util.List;

public class FhirPatientMapper {

    public static FhirPatientDto toFhir(PatientEntity patient) {
        if (patient == null) {
            return null;
        }

        String id = patient.getId() != null ? patient.getId().toString() : null;

        // Identifier
        List<Identifier> identifiers = new ArrayList<>();
        if (patient.getGlobalPatientNumber() != null) {
            identifiers.add(new Identifier("urn:oid:1.3.6.1.4.1.59367.1.1", patient.getGlobalPatientNumber()));
        }

        // Name
        List<HumanName> names = new ArrayList<>();
        if (patient.getFullName() != null) {
            String fullName = patient.getFullName();
            String[] parts = fullName.trim().split("\\s+");
            String family = "";
            List<String> given = new ArrayList<>();
            if (parts.length > 0) {
                family = parts[parts.length - 1];
                for (int i = 0; i < parts.length - 1; i++) {
                    given.add(parts[i]);
                }
            }
            names.add(new HumanName("official", fullName, family, given));
        }

        // Gender
        String gender = "unknown";
        if (patient.getGender() != null) {
            String dbGender = patient.getGender().toUpperCase();
            if ("MASCULIN".equals(dbGender) || "MALE".equals(dbGender)) {
                gender = "male";
            } else if ("FEMININ".equals(dbGender) || "FEMALE".equals(dbGender)) {
                gender = "female";
            } else {
                gender = "other";
            }
        }

        // Birth Date
        String birthDate = patient.getBirthDate() != null ? patient.getBirthDate().toString() : null;

        // Telecom
        List<ContactPoint> telecom = new ArrayList<>();
        if (patient.getPhone() != null && !patient.getPhone().isBlank()) {
            telecom.add(new ContactPoint("phone", patient.getPhone()));
        }

        return new FhirPatientDto(id, identifiers, names, gender, birthDate, telecom);
    }
}
