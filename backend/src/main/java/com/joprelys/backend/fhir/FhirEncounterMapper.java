package com.joprelys.backend.fhir;

import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.fhir.model.*;

import java.util.ArrayList;
import java.util.List;

public class FhirEncounterMapper {

    public static FhirEncounterDto toFhir(VisitEntity visit) {
        if (visit == null) {
            return null;
        }

        String id = visit.getId() != null ? visit.getId().toString() : null;

        // Identifier
        List<Identifier> identifiers = new ArrayList<>();
        if (visit.getVisitNumber() != null) {
            identifiers.add(new Identifier(visit.getVisitNumber()));
        }

        // Status
        String status = "unknown";
        if (visit.getStatus() != null) {
            String dbStatus = visit.getStatus().toUpperCase();
            if ("EN_COURS".equals(dbStatus) || "IN_PROGRESS".equals(dbStatus)) {
                status = "in-progress";
            } else if ("TERMINEE".equals(dbStatus) || "FINISHED".equals(dbStatus)) {
                status = "finished";
            }
        }

        // Class (encounterClass)
        Coding encounterClass = new Coding(
                "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                "AMB",
                "ambulatory"
            );

        // Subject
        Reference subject = null;
        if (visit.getPatient() != null && visit.getPatient().getId() != null) {
            subject = new Reference("Patient/" + visit.getPatient().getId());
        }

        // Period
        Period period = null;
        if (visit.getCreatedAt() != null) {
            String start = visit.getCreatedAt().toString();
            String end = visit.getClosedAt() != null ? visit.getClosedAt().toString() : null;
            period = new Period(start, end);
        }

        return new FhirEncounterDto(id, identifiers, status, encounterClass, subject, period);
    }
}
