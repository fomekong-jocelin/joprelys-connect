package com.joprelys.backend.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RbacCatalogHospitalizationClinicalPermissionTest {

    @Test
    void dedicatedHospitalizationWritePermissionsShouldBeRegisteredWithoutLegacyFallback() {
        Set<String> permissions = RbacCatalog.permissionCodes();

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_RECORD));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_RECORD));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void doctorShouldHandleMedicalDocumentationWithoutMedicationAdministrationOrConsumablesByDefault() {
        Set<String> permissions = permissionsFor("MEDECIN");

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_RECORD));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_RECORD));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void nurseShouldDocumentCareAndAdministrationWithoutOpeningHospitalizations() {
        Set<String> permissions = permissionsFor("INFIRMIER");

        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_RECORD));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_RECORD));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void hospitalizationManagerShouldAdmitWithoutWritingClinicalActs() {
        Set<String> permissions = permissionsFor("RESPONSABLE_HOSPITALISATION");

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_RECORD));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_RECORD));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void clinicAdminShouldRetainAllDedicatedEstablishmentPermissionsWithoutLegacyPermission() {
        Set<String> permissions = permissionsFor(RbacCatalog.ROLE_ADMIN_CLINIQUE);

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_RECORD));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_RECORD));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void technicalBedRolesShouldNotGainClinicalWritePermissions() {
        Set<String> hygiene = permissionsFor(RbacCatalog.ROLE_AGENT_HYGIENE);
        Set<String> maintenance = permissionsFor(RbacCatalog.ROLE_TECHNICIEN_MAINTENANCE);

        for (String permission : Set.of(
                RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT,
                RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE,
                RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_RECORD,
                RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE,
                RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER,
                RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_RECORD)) {
            assertFalse(hygiene.contains(permission));
            assertFalse(maintenance.contains(permission));
        }
    }

    private Set<String> permissionsFor(String roleCode) {
        return RbacCatalog.permissionsForLegacyRoles(Set.of(roleCode));
    }
}
