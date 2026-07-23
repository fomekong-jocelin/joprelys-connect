package com.joprelys.backend.auth.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class HospitalOrganizationRbacCatalogTest {

    @Test
    void onlyAdministrativeRolesReceiveOrganizationStructureManagementByDefault() {
        Map<String, RbacCatalog.RoleDefinition> roles = RbacCatalog.systemRoles().stream()
                .collect(Collectors.toMap(RbacCatalog.RoleDefinition::code, Function.identity()));

        assertThat(roles.get(RbacCatalog.ROLE_ADMIN_JOPRELYS).permissions())
                .contains(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);
        assertThat(roles.get(RbacCatalog.ROLE_SUPER_ADMIN).permissions())
                .contains(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);
        assertThat(roles.get(RbacCatalog.ROLE_ADMIN_CLINIQUE).permissions())
                .contains(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);

        assertThat(roles.get("MEDECIN").permissions())
                .doesNotContain(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);
        assertThat(roles.get("INFIRMIER").permissions())
                .doesNotContain(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);
        assertThat(roles.get("RESPONSABLE_HOSPITALISATION").permissions())
                .doesNotContain(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);
    }

    @Test
    void organizationStructureManagementIsNotAPlatformOnlyPermission() {
        assertThat(RbacCatalog.platformPermissionCodes())
                .doesNotContain(RbacCatalog.PERMISSION_ORGANIZATION_STRUCTURE_MANAGE);
    }
}
