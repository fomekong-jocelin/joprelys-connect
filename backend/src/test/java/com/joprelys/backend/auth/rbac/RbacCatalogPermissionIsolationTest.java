package com.joprelys.backend.auth.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RbacCatalogPermissionIsolationTest {

    @Test
    void patientRoleOnlyReceivesPatientPortalCapabilities() {
        Map<String, RbacCatalog.RoleDefinition> roles = rolesByCode();

        assertThat(roles.get("PATIENT").permissions()).containsExactlyInAnyOrder(
                RbacCatalog.PERMISSION_PATIENT_PORTAL_ACCESS,
                RbacCatalog.PERMISSION_PATIENT_APPOINTMENT_MANAGE,
                RbacCatalog.PERMISSION_PATIENT_NOTIFICATION_MANAGE);
        assertThat(roles.get("PATIENT").permissions())
                .doesNotContain("AVAILABILITY_MANAGE", "CLINICAL_READ", "PATIENT_READ", "STAFF_PROFILE_ACCESS");
    }

    @Test
    void everyProfessionalSystemRoleReceivesOnlyExplicitSharedStaffCapabilities() {
        RbacCatalog.systemRoles().stream()
                .filter(role -> !"PATIENT".equals(role.code()))
                .forEach(role -> assertThat(role.permissions())
                        .as("permissions communes du rôle %s", role.code())
                        .contains("STAFF_PROFILE_ACCESS", "FILE_UPLOAD")
                        .doesNotContain(
                                RbacCatalog.PERMISSION_PATIENT_PORTAL_ACCESS,
                                RbacCatalog.PERMISSION_PATIENT_APPOINTMENT_MANAGE,
                                RbacCatalog.PERMISSION_PATIENT_NOTIFICATION_MANAGE));
    }

    @Test
    void platformRolesAreNeverAssignableFromClinicAdministration() {
        Map<String, RbacCatalog.RoleDefinition> roles = rolesByCode();

        assertThat(roles.get(RbacCatalog.ROLE_ADMIN_JOPRELYS).assignable()).isFalse();
        assertThat(roles.get(RbacCatalog.ROLE_SUPER_ADMIN).assignable()).isFalse();
    }

    @Test
    void sensitiveCapabilitiesAreNotInheritedFromUnrelatedRoles() {
        Map<String, RbacCatalog.RoleDefinition> roles = rolesByCode();

        assertThat(roles.get("MEDECIN").permissions())
                .contains("CLINICAL_WRITE", "VISIT_MANAGE", "AVAILABILITY_MANAGE")
                .doesNotContain(
                        "AVAILABILITY_MANAGE_ALL",
                        "BILLING_INVOICE_READ",
                        "BILLING_INVOICE_WRITE",
                        "CASH_QUEUE_READ",
                        "CASH_PAYMENT_COLLECT",
                        "CASH_SESSION_OPEN",
                        "CASH_SESSION_CLOSE",
                        "CASH_MOVEMENT_WRITE",
                        "CASH_HISTORY_READ");
        assertThat(roles.get("CAISSIER").permissions()).contains("CASH_PAYMENT_COLLECT");
        assertThat(roles.get("AUDITEUR").permissions()).contains("AUDIT_CROSS_TENANT_READ");
        assertThat(roles.get("ADMIN_CLINIQUE").permissions()).contains("AVAILABILITY_MANAGE_ALL");

        assertThat(roles.get("AGENT_ACCUEIL").permissions())
                .doesNotContain("CLINICAL_WRITE", "CASH_PAYMENT_COLLECT", "AUDIT_CROSS_TENANT_READ");
        assertThat(roles.get("PHARMACIEN").permissions())
                .doesNotContain("CLINICAL_WRITE", "BILLING_INVOICE_WRITE", "AUDIT_READ");
        assertThat(roles.get("AUDITEUR").permissions())
                .doesNotContain("PATIENT_WRITE", "CLINICAL_WRITE", "CASH_PAYMENT_COLLECT");
    }

    private Map<String, RbacCatalog.RoleDefinition> rolesByCode() {
        return RbacCatalog.systemRoles().stream()
                .collect(Collectors.toMap(RbacCatalog.RoleDefinition::code, Function.identity()));
    }
}
