package com.joprelys.backend.auth.rbac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DoctorAppointmentPermissionCatalogTest {

    @Test
    void shouldReserveOwnAppointmentCalendarPermissionToDoctors() {
        Set<String> doctorPermissions = permissionsFor("MEDECIN");
        Set<String> receptionPermissions = permissionsFor("AGENT_ACCUEIL");
        Set<String> clinicAdminPermissions = permissionsFor(RbacCatalog.ROLE_ADMIN_CLINIQUE);

        assertTrue(doctorPermissions.contains(RbacCatalog.PERMISSION_APPOINTMENT_READ_OWN));
        assertFalse(receptionPermissions.contains(RbacCatalog.PERMISSION_APPOINTMENT_READ_OWN));
        assertFalse(clinicAdminPermissions.contains(RbacCatalog.PERMISSION_APPOINTMENT_READ_OWN));
    }

    @Test
    void shouldCatalogOwnAppointmentCalendarPermission() {
        assertTrue(RbacCatalog.permissionCodes().contains(RbacCatalog.PERMISSION_APPOINTMENT_READ_OWN));
    }

    private static Set<String> permissionsFor(String roleCode) {
        return RbacCatalog.systemRoles().stream()
                .filter(role -> role.code().equals(roleCode))
                .findFirst()
                .orElseThrow()
                .permissions();
    }
}
