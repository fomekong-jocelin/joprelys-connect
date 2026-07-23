package com.joprelys.backend.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

final class BedAssignmentPostgresqlMigrationAssertions {

    private BedAssignmentPostgresqlMigrationAssertions() {
    }

    static void assertSchemaAndIntegrity(JdbcTemplate jdbcTemplate) {
        assertSchema(jdbcTemplate);
        assertDuplicateActiveAssignmentRejected(jdbcTemplate);
    }

    private static void assertSchema(JdbcTemplate jdbcTemplate) {
        Integer activeBedIndexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uq_bed_assignments_active_bed'
                """, Integer.class);
        Integer activeBedCheckCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'ck_bed_assignments_active_bed_consistency'
                """, Integer.class);
        Integer activeHospitalizationIndexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uq_bed_assignments_active_hospitalization'
                """, Integer.class);
        Integer activeHospitalizationCheckCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'ck_bed_assignments_active_hospitalization_consistency'
                """, Integer.class);
        Integer assignmentPeriodCheckCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'ck_bed_assignments_assignment_period'
                """, Integer.class);
        Integer hospitalizationTenantUniqueCount = constraintCount(jdbcTemplate, "uq_hospitalizations_id_organization");
        Integer bedTenantUniqueCount = constraintCount(jdbcTemplate, "uq_beds_id_organization");
        Integer hospitalizationTenantForeignKeyCount = constraintCount(
                jdbcTemplate, "fk_bed_assignments_hospitalization_organization");
        Integer bedTenantForeignKeyCount = constraintCount(jdbcTemplate, "fk_bed_assignments_bed_organization");
        String hospitalizationDeleteRule = jdbcTemplate.queryForObject("""
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_name = 'fk_bed_assignments_hospitalization'
                """, String.class);
        String activeBedNullability = jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'bed_assignments'
                  AND column_name = 'active_bed_id'
                """, String.class);
        String activeHospitalizationNullability = jdbcTemplate.queryForObject("""
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'bed_assignments'
                  AND column_name = 'active_hospitalization_id'
                """, String.class);
        String hospitalizationTenantDeleteRule = deleteRule(
                jdbcTemplate, "fk_bed_assignments_hospitalization_organization");
        String bedTenantDeleteRule = deleteRule(jdbcTemplate, "fk_bed_assignments_bed_organization");

        assertEquals(1, activeBedIndexCount, "L'index unique d'affectation active par lit doit exister");
        assertEquals(1, activeBedCheckCount, "La contrainte de cohérence du marqueur de lit actif doit exister");
        assertEquals("YES", activeBedNullability, "Le marqueur doit être nullable pour conserver l'historique clôturé");
        assertEquals(1, activeHospitalizationIndexCount, "L'index unique d'affectation active par séjour doit exister");
        assertEquals(1, activeHospitalizationCheckCount, "La contrainte de cohérence du marqueur de séjour actif doit exister");
        assertEquals(1, assignmentPeriodCheckCount, "La contrainte chronologique des affectations doit exister");
        assertEquals("RESTRICT", hospitalizationDeleteRule, "La suppression d'un séjour référencé doit être restrictive");
        assertEquals("YES", activeHospitalizationNullability, "Le marqueur de séjour doit être nullable pour l'historique clôturé");
        assertEquals(1, hospitalizationTenantUniqueCount, "La clé candidate tenant du séjour doit exister");
        assertEquals(1, bedTenantUniqueCount, "La clé candidate tenant du lit doit exister");
        assertEquals(1, hospitalizationTenantForeignKeyCount, "La FK composite vers le séjour doit exister");
        assertEquals(1, bedTenantForeignKeyCount, "La FK composite vers le lit doit exister");
        assertEquals("RESTRICT", hospitalizationTenantDeleteRule, "La FK tenant du séjour doit préserver l'historique");
        assertEquals("RESTRICT", bedTenantDeleteRule, "La FK tenant du lit doit préserver l'historique");
    }

    private static void assertDuplicateActiveAssignmentRejected(JdbcTemplate jdbcTemplate) {
        UUID organizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID firstBedId = UUID.randomUUID();
        UUID secondBedId = UUID.randomUUID();
        UUID firstHospitalizationId = UUID.randomUUID();
        UUID secondHospitalizationId = UUID.randomUUID();
        Instant now = Instant.now();

        insertOrganization(jdbcTemplate, organizationId, "bed-integrity", "Douala", now);
        insertOrganization(jdbcTemplate, otherOrganizationId, "other-bed-integrity", "Yaoundé", now);
        jdbcTemplate.update("""
                INSERT INTO patients (
                    id, organization_id, global_patient_number, local_patient_number,
                    full_name, gender, birth_date, phone, city, status, created_at, updated_at
                ) VALUES (?, ?, 'DPU-BED-PG', 'PAT-BED-PG', 'Patient intégrité',
                    'MASCULIN', ?, '+237600000000', 'Douala', 'ACTIVE', ?, ?)
                """, patientId, organizationId, LocalDate.of(1990, 1, 1), timestamp(now), timestamp(now));
        jdbcTemplate.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'BED_INTEGRITY_UNIT', 'Hospitalisation', 'CARE_UNIT',
                    NULL, TRUE, ?, ?)
                """, unitId, organizationId, timestamp(now), timestamp(now));
        jdbcTemplate.update("""
                INSERT INTO facility_spaces (
                    id, organization_id, location_node_id, code, name, space_type_code,
                    active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'BED_INTEGRITY_SPACE', 'Chambre 101', 'HOSPITAL_ROOM',
                    TRUE, ?, ?)
                """, spaceId, organizationId, timestamp(now), timestamp(now));
        jdbcTemplate.update("""
                INSERT INTO inpatient_space_profiles (
                    space_id, organization_id, space_type_code, comfort_level, created_at, updated_at
                ) VALUES (?, ?, 'HOSPITAL_ROOM', 'STANDARD', ?, ?)
                """, spaceId, organizationId, timestamp(now), timestamp(now));
        insertBed(jdbcTemplate, firstBedId, spaceId, organizationId, "101-A", "OCCUPIED", now);
        insertBed(jdbcTemplate, secondBedId, spaceId, organizationId, "101-B", "FREE", now);
        insertHospitalization(
                jdbcTemplate, firstHospitalizationId, patientId, organizationId, unitId, spaceId,
                firstBedId, "HOSP-BED-PG-1", "101-A", now);
        insertHospitalization(
                jdbcTemplate, secondHospitalizationId, patientId, organizationId, unitId, spaceId,
                secondBedId, "HOSP-BED-PG-2", "101-B", now);
        insertActiveAssignment(jdbcTemplate, firstHospitalizationId, firstBedId, organizationId, now);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertActiveAssignment(jdbcTemplate, secondHospitalizationId, firstBedId, organizationId, now),
                "PostgreSQL doit refuser deux affectations actives du même lit");
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertActiveAssignment(jdbcTemplate, firstHospitalizationId, secondBedId, organizationId, now),
                "PostgreSQL doit refuser deux affectations actives du même séjour");
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertActiveAssignment(jdbcTemplate, UUID.randomUUID(), secondBedId, organizationId, now),
                "PostgreSQL doit refuser une affectation sans séjour");
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertReleasedAssignment(
                        jdbcTemplate, secondHospitalizationId, secondBedId, organizationId, now, now.minusSeconds(1)),
                "PostgreSQL doit refuser une période d'affectation inversée");
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertReleasedAssignment(
                        jdbcTemplate, secondHospitalizationId, secondBedId, otherOrganizationId,
                        now.minusSeconds(1), now),
                "PostgreSQL doit refuser une affectation d'un autre établissement");
    }

    private static void insertOrganization(
            JdbcTemplate jdbcTemplate,
            UUID organizationId,
            String slug,
            String city,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, '000', 'Adresse test', ?, 'ACTIVE', 'CLINIC', 'Cameroun',
                    'Responsable', TRUE, ?, ?)
                """,
                organizationId,
                "Clinique " + slug,
                slug + "-" + organizationId + "@joprelys.local",
                city,
                timestamp(now),
                timestamp(now));
    }

    private static void insertBed(
            JdbcTemplate jdbcTemplate,
            UUID bedId,
            UUID spaceId,
            UUID organizationId,
            String bedNumber,
            String status,
            Instant now) {
        jdbcTemplate.update("""
                INSERT INTO beds (
                    id, space_id, bed_number, status, capacity_status, readiness_status,
                    organization_id, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'OPEN', 'READY', ?, 0, ?, ?)
                """, bedId, spaceId, bedNumber, status, organizationId, timestamp(now), timestamp(now));
    }

    private static Integer constraintCount(JdbcTemplate jdbcTemplate, String constraintName) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = ?
                """, Integer.class, constraintName);
    }

    private static String deleteRule(JdbcTemplate jdbcTemplate, String constraintName) {
        return jdbcTemplate.queryForObject("""
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_name = ?
                """, String.class, constraintName);
    }

    private static void insertActiveAssignment(
            JdbcTemplate jdbcTemplate,
            UUID hospitalizationId,
            UUID bedId,
            UUID organizationId,
            Instant assignedAt) {
        jdbcTemplate.update("""
                INSERT INTO bed_assignments (
                    id, hospitalization_id, bed_id, assigned_at, released_at,
                    active_bed_id, active_hospitalization_id, organization_id
                ) VALUES (?, ?, ?, ?, NULL, ?, ?, ?)
                """, UUID.randomUUID(), hospitalizationId, bedId, timestamp(assignedAt),
                bedId, hospitalizationId, organizationId);
    }

    private static void insertReleasedAssignment(
            JdbcTemplate jdbcTemplate,
            UUID hospitalizationId,
            UUID bedId,
            UUID organizationId,
            Instant assignedAt,
            Instant releasedAt) {
        jdbcTemplate.update("""
                INSERT INTO bed_assignments (
                    id, hospitalization_id, bed_id, assigned_at, released_at,
                    active_bed_id, active_hospitalization_id, organization_id
                ) VALUES (?, ?, ?, ?, ?, NULL, NULL, ?)
                """, UUID.randomUUID(), hospitalizationId, bedId,
                timestamp(assignedAt), timestamp(releasedAt), organizationId);
    }

    private static void insertHospitalization(
            JdbcTemplate jdbcTemplate,
            UUID hospitalizationId,
            UUID patientId,
            UUID organizationId,
            UUID serviceUnitId,
            UUID spaceId,
            UUID bedId,
            String hospitalizationNumber,
            String bedNumber,
            Instant admittedAt) {
        Timestamp admittedTimestamp = timestamp(admittedAt);
        jdbcTemplate.update("""
                INSERT INTO hospitalizations (
                    id, patient_id, organization_id, version,
                    service_name_snapshot, space_name_snapshot, bed_number_snapshot,
                    current_service_unit_id, current_space_id, current_bed_id,
                    admission_reason, hospitalization_number, status,
                    admitted_at, created_at, updated_at
                ) VALUES (?, ?, ?, 0, 'Hospitalisation', 'Chambre 101', ?, ?, ?, ?,
                    'Test intégrité', ?, 'EN_COURS', ?, ?, ?)
                """,
                hospitalizationId,
                patientId,
                organizationId,
                bedNumber,
                serviceUnitId,
                spaceId,
                bedId,
                hospitalizationNumber,
                admittedTimestamp,
                admittedTimestamp,
                admittedTimestamp);
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
