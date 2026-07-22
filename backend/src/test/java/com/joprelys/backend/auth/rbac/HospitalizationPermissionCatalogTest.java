package com.joprelys.backend.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class HospitalizationPermissionCatalogTest {

    @Test
    void shouldExposeSixDistinctWritePermissions() {
        Set<String> codes = HospitalizationPermissionCatalog.permissions().stream()
                .map(HospitalizationPermissionCatalog.PermissionDefinition::code)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(6, codes.size());
        assertTrue(codes.contains(HospitalizationPermissionCatalog.ADMIT));
        assertTrue(codes.contains(HospitalizationPermissionCatalog.NOTE_WRITE));
        assertTrue(codes.contains(HospitalizationPermissionCatalog.CONSENT_MANAGE));
        assertTrue(codes.contains(HospitalizationPermissionCatalog.CARE_WRITE));
        assertTrue(codes.contains(HospitalizationPermissionCatalog.MEDICATION_ADMINISTER));
        assertTrue(codes.contains(HospitalizationPermissionCatalog.CONSUMABLE_MANAGE));
        assertFalse(codes.stream().anyMatch(code -> code.contains("PRESCRIBE")),
                "La prescription doit rester distincte de l'administration médicamenteuse");
    }

    @Test
    void doctorShouldNotReceiveNursingExecutionPermissions() {
        Set<String> permissions = HospitalizationPermissionCatalog.systemRolePermissions().get("MEDECIN");

        assertEquals(Set.of(
                HospitalizationPermissionCatalog.ADMIT,
                HospitalizationPermissionCatalog.NOTE_WRITE,
                HospitalizationPermissionCatalog.CONSENT_MANAGE), permissions);
        assertFalse(permissions.contains(HospitalizationPermissionCatalog.CARE_WRITE));
        assertFalse(permissions.contains(HospitalizationPermissionCatalog.MEDICATION_ADMINISTER));
        assertFalse(permissions.contains(HospitalizationPermissionCatalog.CONSUMABLE_MANAGE));
    }

    @Test
    void nurseShouldReceiveCareExecutionButNotAdmissionOrConsent() {
        Set<String> permissions = HospitalizationPermissionCatalog.systemRolePermissions().get("INFIRMIER");

        assertEquals(Set.of(
                HospitalizationPermissionCatalog.NOTE_WRITE,
                HospitalizationPermissionCatalog.CARE_WRITE,
                HospitalizationPermissionCatalog.MEDICATION_ADMINISTER,
                HospitalizationPermissionCatalog.CONSUMABLE_MANAGE), permissions);
        assertFalse(permissions.contains(HospitalizationPermissionCatalog.ADMIT));
        assertFalse(permissions.contains(HospitalizationPermissionCatalog.CONSENT_MANAGE));
    }

    @Test
    void hospitalizationManagerShouldOnlyReceiveAdministrativeAdmission() {
        assertEquals(
                Set.of(HospitalizationPermissionCatalog.ADMIT),
                HospitalizationPermissionCatalog.systemRolePermissions().get("RESPONSABLE_HOSPITALISATION"));
    }
}
