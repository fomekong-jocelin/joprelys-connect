package com.joprelys.backend.auth.rbac;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applique le sous-catalogue hospitalisation après le catalogue RBAC historique.
 *
 * <p>Cette étape rend la migration rejouable : le bootstrap principal peut continuer à
 * resynchroniser les rôles système, puis cette extension retire l'autorisation historique
 * trop large et rétablit la séparation des tâches.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class HospitalizationRbacBootstrap implements ApplicationRunner {

    private static final String DOMAIN = "HOSPITALISATION";
    private static final String LEGACY_PERMISSION = "HOSPITALIZATION_MANAGE";
    private static final Set<String> MIGRATED_SYSTEM_ROLES = Set.of(
            "MEDECIN", "INFIRMIER", "RESPONSABLE_HOSPITALISATION");

    private final JdbcTemplate jdbcTemplate;

    public HospitalizationRbacBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        synchronizeSystemRoles();
    }

    private void seedPermissions() {
        Timestamp now = Timestamp.from(Instant.now());
        for (HospitalizationPermissionCatalog.PermissionDefinition permission
                : HospitalizationPermissionCatalog.permissions()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM permissions WHERE code = ?",
                    Integer.class,
                    permission.code());
            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO permissions(code, domain, name, description, created_at) VALUES (?, ?, ?, ?, ?)",
                        permission.code(), DOMAIN, permission.name(), permission.description(), now);
            } else {
                jdbcTemplate.update(
                        "UPDATE permissions SET domain = ?, name = ?, description = ? WHERE code = ?",
                        DOMAIN, permission.name(), permission.description(), permission.code());
            }
        }
    }

    private void synchronizeSystemRoles() {
        for (String roleCode : MIGRATED_SYSTEM_ROLES) {
            jdbcTemplate.update(
                    "DELETE FROM role_permissions WHERE role_id = ? AND permission_code = ?",
                    RbacCatalog.roleId(roleCode),
                    LEGACY_PERMISSION);
        }

        HospitalizationPermissionCatalog.systemRolePermissions().forEach((roleCode, permissions) -> {
            for (String permission : permissions) {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_code = ?",
                        Integer.class,
                        RbacCatalog.roleId(roleCode),
                        permission);
                if (count != null && count == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO role_permissions(role_id, permission_code) VALUES (?, ?)",
                            RbacCatalog.roleId(roleCode),
                            permission);
                }
            }
        });
    }
}
