package com.joprelys.backend.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RbacCatalogBedOperationalStatusPermissionTest {

    @Test
    void permissionShouldBeRegisteredInCatalog() {
        assertTrue(RbacCatalog.permissionCodes().contains(
                RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
    }

    @Test
    void clinicAdministratorDoctorAndHospitalizationManagerShouldReceivePermission() {
        assertTrue(permissionsFor("ADMIN_CLINIQUE").contains(
                RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertTrue(permissionsFor("MEDECIN").contains(
                RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
        assertTrue(permissionsFor("RESPONSABLE_HOSPITALISATION").contains(
                RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
    }

    @Test
    void standardNurseShouldNotReceiveOperationalBedStatusPermission() {
        assertTrue(permissionsFor("INFIRMIER").contains("HOSPITALIZATION_MANAGE"),
                "L'infirmier conserve les autres actions legacy d'hospitalisation dans cet incrément.");
        assertFalse(permissionsFor("INFIRMIER").contains(
                RbacCatalog.PERMISSION_BED_OPERATIONAL_STATUS_MANAGE));
    }

    private Set<String> permissionsFor(String roleCode) {
        return RbacCatalog.permissionsForLegacyRoles(Set.of(roleCode));
    }
}
