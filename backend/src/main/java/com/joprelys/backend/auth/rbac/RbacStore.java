package com.joprelys.backend.auth.rbac;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RbacStore {

    private final JdbcTemplate jdbcTemplate;

    public RbacStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void seedCatalog() {
        Timestamp now = sqlTimestamp(Instant.now());
        for (RbacCatalog.PermissionDefinition permission : RbacCatalog.permissions()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM permissions WHERE code = ?",
                    Integer.class,
                    permission.code());
            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO permissions(code, domain, name, description, created_at) VALUES (?, ?, ?, ?, ?)",
                        permission.code(), permission.domain(), permission.name(), permission.description(), now);
            } else {
                jdbcTemplate.update(
                        "UPDATE permissions SET domain = ?, name = ?, description = ? WHERE code = ?",
                        permission.domain(), permission.name(), permission.description(), permission.code());
            }
        }

        for (RbacCatalog.RoleDefinition role : RbacCatalog.systemRoles()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE id = ?",
                    Integer.class,
                    role.id());
            if (count != null && count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO roles(id, organization_id, code, name, description, system_role, assignable, enabled, created_at, updated_at, version) "
                                + "VALUES (?, NULL, ?, ?, ?, TRUE, ?, TRUE, ?, ?, 0)",
                        role.id(), role.code(), role.name(), role.description(), role.assignable(), now, now);
            } else {
                jdbcTemplate.update(
                        "UPDATE roles SET code = ?, name = ?, description = ?, system_role = TRUE, assignable = ?, enabled = TRUE, updated_at = ? WHERE id = ?",
                        role.code(), role.name(), role.description(), role.assignable(), now, role.id());
            }
            jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", role.id());
            for (String permissionCode : role.permissions()) {
                jdbcTemplate.update(
                        "INSERT INTO role_permissions(role_id, permission_code) VALUES (?, ?)",
                        role.id(), permissionCode);
            }
        }
    }

    @Transactional
    public void synchronizeLegacyAssignments(UserAccountEntity user) {
        if (user.getRole() == null || user.getRole().isBlank()) {
            return;
        }
        Set<String> codes = Arrays.stream(user.getRole().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String code : codes) {
            findSystemRoleByCode(code).ifPresent(role -> insertUserRoleIfMissing(
                    user.getId(), role.id(), user.getOrganizationId(), user.getId()));
        }
    }

    @Transactional(readOnly = true)
    public EffectiveAccess loadEffectiveAccess(UUID userId, UUID organizationId) {
        List<RoleView> roles = jdbcTemplate.query(
                "SELECT DISTINCT r.id, r.organization_id, r.code, r.name, r.description, r.system_role, r.assignable, r.enabled "
                        + "FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                        + "WHERE ur.user_id = ? AND r.enabled = TRUE "
                        + "AND (r.organization_id IS NULL OR r.organization_id = ?) "
                        + "ORDER BY r.system_role DESC, r.name ASC",
                (rs, rowNum) -> mapRole(rs),
                userId,
                organizationId);
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        if (!roles.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(roles.size(), "?"));
            List<Object> parameters = roles.stream().map(RoleView::id).map(value -> (Object) value).toList();
            permissions.addAll(jdbcTemplate.query(
                    "SELECT DISTINCT rp.permission_code FROM role_permissions rp "
                            + "JOIN permissions p ON p.code = rp.permission_code "
                            + "WHERE rp.role_id IN (" + placeholders + ") ORDER BY rp.permission_code",
                    (rs, rowNum) -> rs.getString(1),
                    parameters.toArray()));
        }
        return new EffectiveAccess(
                userId,
                roles.stream().map(RoleView::code).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                permissions);
    }

    @Transactional(readOnly = true)
    public List<RoleView> listVisibleRoles(UUID organizationId) {
        List<RoleView> roles = jdbcTemplate.query(
                "SELECT id, organization_id, code, name, description, system_role, assignable, enabled "
                        + "FROM roles WHERE organization_id IS NULL OR organization_id = ? "
                        + "ORDER BY system_role DESC, name ASC",
                (rs, rowNum) -> mapRole(rs),
                organizationId);
        return roles.stream().map(this::withPermissions).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionView> listPermissions() {
        return jdbcTemplate.query(
                "SELECT code, domain, name, description FROM permissions ORDER BY domain, name",
                (rs, rowNum) -> new PermissionView(
                        rs.getString("code"),
                        rs.getString("domain"),
                        rs.getString("name"),
                        rs.getString("description")));
    }

    @Transactional(readOnly = true)
    public Optional<RoleView> findVisibleRole(UUID roleId, UUID organizationId) {
        try {
            RoleView role = jdbcTemplate.queryForObject(
                    "SELECT id, organization_id, code, name, description, system_role, assignable, enabled "
                            + "FROM roles WHERE id = ? AND (organization_id IS NULL OR organization_id = ?)",
                    (rs, rowNum) -> mapRole(rs),
                    roleId,
                    organizationId);
            return Optional.ofNullable(role).map(this::withPermissions);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Optional<RoleView> findSystemRoleByCode(String code) {
        try {
            RoleView role = jdbcTemplate.queryForObject(
                    "SELECT id, organization_id, code, name, description, system_role, assignable, enabled "
                            + "FROM roles WHERE organization_id IS NULL AND system_role = TRUE AND code = ?",
                    (rs, rowNum) -> mapRole(rs),
                    code);
            return Optional.ofNullable(role).map(this::withPermissions);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public boolean customRoleCodeExists(UUID organizationId, String code, UUID excludedRoleId) {
        Integer count;
        if (excludedRoleId == null) {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE code = ? AND (organization_id IS NULL OR organization_id = ?)",
                    Integer.class,
                    code,
                    organizationId);
        } else {
            count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE code = ? AND (organization_id IS NULL OR organization_id = ?) AND id <> ?",
                    Integer.class,
                    code,
                    organizationId,
                    excludedRoleId);
        }
        return count != null && count > 0;
    }

    @Transactional
    public RoleView createCustomRole(
            UUID organizationId,
            String code,
            String name,
            String description,
            boolean assignable,
            Set<String> permissionCodes) {
        UUID id = UUID.randomUUID();
        Timestamp now = sqlTimestamp(Instant.now());
        jdbcTemplate.update(
                "INSERT INTO roles(id, organization_id, code, name, description, system_role, assignable, enabled, created_at, updated_at, version) "
                        + "VALUES (?, ?, ?, ?, ?, FALSE, ?, TRUE, ?, ?, 0)",
                id, organizationId, code, name, description, assignable, now, now);
        replaceRolePermissions(id, permissionCodes);
        return findVisibleRole(id, organizationId).orElseThrow();
    }

    @Transactional
    public RoleView updateCustomRole(
            UUID roleId,
            UUID organizationId,
            String code,
            String name,
            String description,
            boolean assignable,
            boolean enabled) {
        int updated = jdbcTemplate.update(
                "UPDATE roles SET code = ?, name = ?, description = ?, assignable = ?, enabled = ?, updated_at = ?, version = version + 1 "
                        + "WHERE id = ? AND organization_id = ? AND system_role = FALSE",
                code, name, description, assignable, enabled, sqlTimestamp(Instant.now()), roleId, organizationId);
        if (updated == 0) {
            throw new IllegalStateException("Custom role not found");
        }
        return findVisibleRole(roleId, organizationId).orElseThrow();
    }

    @Transactional
    public RoleView replaceCustomRolePermissions(UUID roleId, UUID organizationId, Set<String> permissionCodes) {
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE id = ? AND organization_id = ? AND system_role = FALSE",
                Integer.class,
                roleId,
                organizationId);
        if (count == 0) {
            throw new IllegalStateException("Custom role not found");
        }
        replaceRolePermissions(roleId, permissionCodes);
        jdbcTemplate.update(
                "UPDATE roles SET updated_at = ?, version = version + 1 WHERE id = ?",
                sqlTimestamp(Instant.now()),
                roleId);
        return findVisibleRole(roleId, organizationId).orElseThrow();
    }

    @Transactional
    public void replaceUserRoles(UUID userId, UUID organizationId, UUID assignedBy, List<RoleView> roles) {
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        for (RoleView role : roles) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles(user_id, role_id, organization_id, assigned_by, assigned_at) VALUES (?, ?, ?, ?, ?)",
                    userId, role.id(), organizationId, assignedBy, sqlTimestamp(Instant.now()));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRoleAssignedToUser(UUID roleId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE role_id = ? AND user_id = ?",
                Integer.class,
                roleId,
                userId);
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public boolean userHasAnyRole(UUID userId, Set<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(roleCodes.size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        parameters.addAll(roleCodes);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                        + "WHERE ur.user_id = ? AND r.code IN (" + placeholders + ") AND r.enabled = TRUE",
                Integer.class,
                parameters.toArray());
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public long countActiveAdministrators(UUID organizationId) {
        String placeholders = String.join(",", java.util.Collections.nCopies(RbacCatalog.adminRoleCodes().size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(organizationId);
        parameters.addAll(RbacCatalog.adminRoleCodes());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT u.id) FROM users u "
                        + "JOIN user_roles ur ON ur.user_id = u.id "
                        + "JOIN roles r ON r.id = ur.role_id "
                        + "WHERE u.organization_id = ? AND u.enabled = TRUE AND r.enabled = TRUE "
                        + "AND r.code IN (" + placeholders + ")",
                Long.class,
                parameters.toArray());
        return count == null ? 0 : count;
    }

    @Transactional
    public void audit(
            UUID organizationId,
            UUID actorUserId,
            String action,
            String targetType,
            String targetId,
            String details) {
        jdbcTemplate.update(
                "INSERT INTO rbac_audit_log(id, organization_id, actor_user_id, action, target_type, target_id, details, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), organizationId, actorUserId, action, targetType, targetId, details,
                sqlTimestamp(Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<AuditView> listAudit(UUID organizationId, int limit) {
        return jdbcTemplate.query(
                "SELECT id, actor_user_id, action, target_type, target_id, details, created_at "
                        + "FROM rbac_audit_log WHERE organization_id = ? ORDER BY created_at DESC LIMIT ?",
                (rs, rowNum) -> new AuditView(
                        rs.getObject("id", UUID.class),
                        rs.getObject("actor_user_id", UUID.class),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        rs.getString("target_id"),
                        rs.getString("details"),
                        rs.getTimestamp("created_at").toInstant()),
                organizationId,
                limit);
    }

    private void insertUserRoleIfMissing(UUID userId, UUID roleId, UUID organizationId, UUID assignedBy) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?",
                Integer.class,
                userId,
                roleId);
        if (count != null && count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO user_roles(user_id, role_id, organization_id, assigned_by, assigned_at) VALUES (?, ?, ?, ?, ?)",
                    userId, roleId, organizationId, assignedBy, sqlTimestamp(Instant.now()));
        }
    }

    private void replaceRolePermissions(UUID roleId, Set<String> permissionCodes) {
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        for (String permissionCode : permissionCodes) {
            jdbcTemplate.update(
                    "INSERT INTO role_permissions(role_id, permission_code) VALUES (?, ?)",
                    roleId,
                    permissionCode);
        }
    }

    private RoleView withPermissions(RoleView role) {
        List<String> permissions = jdbcTemplate.query(
                "SELECT permission_code FROM role_permissions WHERE role_id = ? ORDER BY permission_code",
                (rs, rowNum) -> rs.getString(1),
                role.id());
        return new RoleView(
                role.id(), role.organizationId(), role.code(), role.name(), role.description(),
                role.systemRole(), role.assignable(), role.enabled(), new LinkedHashSet<>(permissions));
    }

    private static RoleView mapRole(ResultSet rs) throws SQLException {
        return new RoleView(
                rs.getObject("id", UUID.class),
                rs.getObject("organization_id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("system_role"),
                rs.getBoolean("assignable"),
                rs.getBoolean("enabled"),
                Set.of());
    }

    private static Timestamp sqlTimestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    public record RoleView(
            UUID id,
            UUID organizationId,
            String code,
            String name,
            String description,
            boolean systemRole,
            boolean assignable,
            boolean enabled,
            Set<String> permissions) {
    }

    public record PermissionView(String code, String domain, String name, String description) {
    }

    public record EffectiveAccess(UUID userId, Set<String> roles, Set<String> permissions) {
    }

    public record AuditView(
            UUID id,
            UUID actorUserId,
            String action,
            String targetType,
            String targetId,
            String details,
            Instant createdAt) {
    }
}
