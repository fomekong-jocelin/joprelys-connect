package com.joprelys.backend.appointment.api;

import java.util.List;
import java.util.UUID;

/** Médecin visible dans l'annuaire du portail patient avec référentiels structurés. */
public record DoctorDirectoryEntry(
        UUID doctorId,
        String displayName,
        List<SpecialtyEntry> specialties,
        List<UnitEntry> units
) {
    public record SpecialtyEntry(
            String code,
            String nameFr,
            String nameEn,
            boolean primary) {
    }

    public record UnitEntry(
            UUID id,
            String code,
            String nameFr,
            String nameEn,
            boolean primary) {
    }
}
