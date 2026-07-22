package com.joprelys.backend.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RbacCatalogBedOperationalStatusPermissionTest {

    @Test
    void segregatedHospitalPermissionsShouldBeRegisteredInCatalog() {
        Set<String> permissions = RbacCatalog.permissionCodes();

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
    }

    @Test
    void doctorShouldTransferAndDecideDischargeWithoutConfirmingPhysicalDeparture() {
        Set<String> permissions = permissionsFor("MEDECIN");

        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
    }

    @Test
    void nurseShouldTransferWithoutDecidingOrConfirmingDischarge() {
        Set<String> permissions = permissionsFor("INFIRMIER");

        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
    }

    @Test
    void hospitalizationManagerShouldConfirmDepartureAndSuperviseTechnicalCircuits() {
        Set<String> permissions = permissionsFor("RESPONSABLE_HOSPITALISATION");

        assertFalse(permissions.contains("HOSPITALIZATION_MANAGE"));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_TRANSFER));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_CLEANING_MANAGE));
        assertTrue(permissions.contains(RbacCatalog.PERMISSION_BED_MAINTENANCE_MANAGE));
        assertFalse(permissions.contains(RbacCatalog.PERMISSION_HOSPITALIZATION_DISCHARGE_DECIDE));
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
