package com.joprelys.backend.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RbacCatalogBedOperationalStatusPermissionTest {

    @Test
    void segregatedHospitalPermissionsShouldBeRegisteredInCatalog() {
        Set<String> permissions = RbacCatalog.permissionCodes();

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
    }

    @Test
    void doctorShouldHandleMedicalHospitalizationDutiesWithoutMedicationAdministration() {
        Set<String> permissions = permissionsFor("MEDECIN");

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void nurseShouldDocumentCareAdministerMedicationAndConsumablesWithoutAdmissionOrConsentAuthority() {
        Set<String> permissions = permissionsFor("INFIRMIER");

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void hospitalizationManagerShouldAdmitAndCoordinateBedsWithoutClinicalCareRights() {
        Set<String> permissions = permissionsFor("RESPONSABLE_HOSPITALISATION");

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_ADMIT));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_NOTE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSENT_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CARE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_MEDICATION_ADMINISTER));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_CONSUMABLE_WRITE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void hygieneAndMaintenanceRolesShouldRemainStrictlySeparated() {
        Set<String> hygienePermissions = permissionsFor(RbacCatalog.ROLE_AGENT_HYGIENE);
        Set<String> maintenancePermissions = permissionsFor(RbacCatalog.ROLE_TECHNICIEN_MAINTENANCE);

        assertTrue(hygienePermissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertFalse(hygienePermissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(hygienePermissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertFalse(hygienePermissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));

        assertTrue(maintenancePermissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(maintenancePermissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertFalse(maintenancePermissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertFalse(maintenancePermissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
    }

    private Set<String> permissionsFor(String roleCode) {
        return RbacCatalog.permissionsForLegacyRoles(Set.of(roleCode));
    }
}
