package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Compte les usages en cours d'une unité avant sa désactivation (ADR-20261017 D-10). */
@Repository
public class OrganizationalUnitUsageQuery {

    private final JdbcTemplate jdbcTemplate;

    public OrganizationalUnitUsageQuery(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UnitUsage usageAt(UUID organizationId, UUID unitId, Instant at) {
        Timestamp now = Timestamp.from(at);
        int stays = count("""
                SELECT COUNT(*) FROM hospitalizations
                WHERE organization_id = ? AND current_service_unit_id = ? AND status = 'EN_COURS'
                """, organizationId, unitId);
        int staff = count("""
                SELECT COUNT(*) FROM staff_organizational_unit_assignments
                WHERE organization_id = ? AND organizational_unit_id = ?
                  AND valid_from <= ? AND (valid_to IS NULL OR valid_to > ?)
                """, organizationId, unitId, now, now);
        int spaces = count("""
                SELECT COUNT(*) FROM organizational_unit_space_assignments
                WHERE organization_id = ? AND organizational_unit_id = ?
                  AND valid_from <= ? AND (valid_to IS NULL OR valid_to > ?)
                """, organizationId, unitId, now, now);
        return new UnitUsage(stays, staff, spaces);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    public record UnitUsage(int activeStays, int activeStaffAssignments, int activeSpaceAssignments) {
        public boolean inUse() {
            return activeStays > 0 || activeStaffAssignments > 0 || activeSpaceAssignments > 0;
        }
    }
}
