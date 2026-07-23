package com.joprelys.backend.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class BedAssignmentOverlapPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_overlap_migration_test")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldRequireQuarantineThenEnforceHalfOpenPeriods() {
        migrateToVersion79();
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        TestFixture fixture = seedFixture(jdbcTemplate);

        UUID firstAssignmentId = insertReleasedAssignment(
                jdbcTemplate,
                fixture.firstHospitalizationId(),
                fixture.firstBedId(),
                fixture.organizationId(),
                fixture.referenceTime().minus(4, ChronoUnit.HOURS),
                fixture.referenceTime().minus(2, ChronoUnit.HOURS));
        UUID conflictingAssignmentId = insertReleasedAssignment(
                jdbcTemplate,
                fixture.secondHospitalizationId(),
                fixture.firstBedId(),
                fixture.organizationId(),
                fixture.referenceTime().minus(3, ChronoUnit.HOURS),
                fixture.referenceTime().minus(1, ChronoUnit.HOURS));

        FlywayException migrationFailure = assertThrows(
                FlywayException.class,
                this::migrateThroughVersion80,
                "V80 doit refuser d'activer la contrainte tant qu'un conflit VALID subsiste");
        assertTrue(allMessages(migrationFailure).contains("HOS-BED-001-D bloque V80"));
        assertEquals(1L, unresolvedOverlapCount(jdbcTemplate));

        quarantineAssignment(jdbcTemplate, conflictingAssignmentId);
        migrateThroughVersion80();

        assertEquals(1, constraintCount(jdbcTemplate));
        assertEquals(1, extensionCount(jdbcTemplate));
        assertEquals(1, quarantinedAssignmentCount(jdbcTemplate));
        assertEquals(0L, unresolvedOverlapCount(jdbcTemplate));

        UUID adjacentAssignmentId = insertReleasedAssignment(
                jdbcTemplate,
                fixture.thirdHospitalizationId(),
                fixture.firstBedId(),
                fixture.organizationId(),
                fixture.referenceTime().minus(2, ChronoUnit.HOURS),
                fixture.referenceTime().minus(1, ChronoUnit.HOURS));

        insertReleasedAssignment(
                jdbcTemplate,
                fixture.fourthHospitalizationId(),
                fixture.secondBedId(),
                fixture.organizationId(),
                fixture.referenceTime().minus(4, ChronoUnit.HOURS),
                fixture.referenceTime().minus(2, ChronoUnit.HOURS));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertReleasedAssignment(
                        jdbcTemplate,
                        fixture.fifthHospitalizationId(),
                        fixture.firstBedId(),
                        fixture.organizationId(),
                        fixture.referenceTime().minus(3, ChronoUnit.HOURS),
                        fixture.referenceTime().minus(150, ChronoUnit.MINUTES)),
                "Une nouvelle période VALID chevauchante doit être refusée");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        UPDATE bed_assignments
                        SET assigned_at = ?
                        WHERE id = ?
                        """,
                        timestamp(fixture.referenceTime().minus(3, ChronoUnit.HOURS)),
                        adjacentAssignmentId),
                "Une correction rétroactive qui recrée un chevauchement doit être refusée");

        assertEquals("VALID", integrityStatus(jdbcTemplate, firstAssignmentId));
        assertEquals("QUARANTINED", integrityStatus(jdbcTemplate, conflictingAssignmentId));
        assertEquals("VALID", integrityStatus(jdbcTemplate, adjacentAssignmentId));
    }

    private void migrateToVersion79() {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("79"))
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private void migrateThroughVersion80() {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("80"))
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()));
    }

    private TestFixture seedFixture(JdbcTemplate jdbcTemplate) {
        UUID organizationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID firstBedId = UUID.randomUUID();
        UUID secondBedId = UUID.randomUUID();
        UUID firstHospitalizationId = UUID.randomUUID();
        UUID secondHospitalizationId = UUID.randomUUID();
        UUID thirdHospitalizationId = UUID.randomUUID();
        UUID fourthHospitalizationId = UUID.randomUUID();
        UUID fifthHospitalizationId = UUID.randomUUID();
        Instant referenceTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        jdbcTemplate.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 'CLINIC', 'Cameroun', 'Responsable', TRUE, ?, ?)
                """,
                organizationId,
                "Clinique chevauchements",
                "overlap-integrity@joprelys.local",
                "000",
                "Adresse test",
                "Douala",
                timestamp(referenceTime),
                timestamp(referenceTime));

        jdbcTemplate.update("""
                INSERT INTO patients (
                    id, organization_id, global_patient_number, local_patient_number,
                    full_name, gender, birth_date, phone, city, status, created_at, updated_at
                ) VALUES (?, ?, 'DPU-OVERLAP-PG', 'PAT-OVERLAP-PG', 'Patient test intégrité',
                    'MASCULIN', ?, '+237600000001', 'Douala', 'ACTIVE', ?, ?)
                """,
                patientId,
                organizationId,
                LocalDate.of(1990, 1, 1),
                timestamp(referenceTime),
                timestamp(referenceTime));

        jdbcTemplate.update("""
                INSERT INTO wards (id, name, organization_id, created_at, updated_at, service_type)
                VALUES (?, 'Hospitalisation', ?, ?, ?, 'HOSPITALIZATION')
                """, wardId, organizationId, timestamp(referenceTime), timestamp(referenceTime));

        jdbcTemplate.update("""
                INSERT INTO rooms (
                    id, ward_id, room_number, capacity, comfort_level, organization_id, created_at, updated_at
                ) VALUES (?, ?, '201', 2, 'STANDARD', ?, ?, ?)
                """, roomId, wardId, organizationId, timestamp(referenceTime), timestamp(referenceTime));

        insertBed(jdbcTemplate, firstBedId, roomId, organizationId, "201-A", referenceTime);
        insertBed(jdbcTemplate, secondBedId, roomId, organizationId, "201-B", referenceTime);

        insertHospitalization(jdbcTemplate, firstHospitalizationId, patientId, organizationId,
                "HOSP-OVERLAP-1", "201-A", referenceTime);
        insertHospitalization(jdbcTemplate, secondHospitalizationId, patientId, organizationId,
                "HOSP-OVERLAP-2", "201-A", referenceTime);
        insertHospitalization(jdbcTemplate, thirdHospitalizationId, patientId, organizationId,
                "HOSP-OVERLAP-3", "201-A", referenceTime);
        insertHospitalization(jdbcTemplate, fourthHospitalizationId, patientId, organizationId,
                "HOSP-OVERLAP-4", "201-B", referenceTime);
        insertHospitalization(jdbcTemplate, fifthHospitalizationId, patientId, organizationId,
                "HOSP-OVERLAP-5", "201-A", referenceTime);

        return new TestFixture(
                organizationId,
                firstBedId,
                secondBedId,
                firstHospitalizationId,
                secondHospitalizationId,
                thirdHospitalizationId,
                fourthHospitalizationId,
                fifthHospitalizationId,
                referenceTime);
    }

    private void insertBed(
            JdbcTemplate jdbcTemplate,
            UUID bedId,
            UUID roomId,
            UUID organizationId,
            String bedNumber,
            Instant referenceTime) {
        jdbcTemplate.update("""
                INSERT INTO beds (
                    id, room_id, bed_number, status, organization_id, version, created_at, updated_at
                ) VALUES (?, ?, ?, 'FREE', ?, 0, ?, ?)
                """,
                bedId,
                roomId,
                bedNumber,
                organizationId,
                timestamp(referenceTime),
                timestamp(referenceTime));
    }

    private void insertHospitalization(
            JdbcTemplate jdbcTemplate,
            UUID hospitalizationId,
            UUID patientId,
            UUID organizationId,
            String hospitalizationNumber,
            String bedNumber,
            Instant admittedAt) {
        Timestamp timestamp = timestamp(admittedAt);
        jdbcTemplate.update("""
                INSERT INTO hospitalizations (
                    id, patient_id, organization_id, version, service_name, room_number,
                    bed_number, admission_reason, hospitalization_number, status,
                    admitted_at, created_at, updated_at
                ) VALUES (?, ?, ?, 0, 'Hospitalisation', '201', ?, 'Test chevauchement', ?,
                    'EN_COURS', ?, ?, ?)
                """,
                hospitalizationId,
                patientId,
                organizationId,
                bedNumber,
                hospitalizationNumber,
                timestamp,
                timestamp,
                timestamp);
    }

    private UUID insertReleasedAssignment(
            JdbcTemplate jdbcTemplate,
            UUID hospitalizationId,
            UUID bedId,
            UUID organizationId,
            Instant assignedAt,
            Instant releasedAt) {
        UUID assignmentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO bed_assignments (
                    id, hospitalization_id, bed_id, assigned_at, released_at,
                    active_bed_id, active_hospitalization_id, organization_id
                ) VALUES (?, ?, ?, ?, ?, NULL, NULL, ?)
                """,
                assignmentId,
                hospitalizationId,
                bedId,
                timestamp(assignedAt),
                timestamp(releasedAt),
                organizationId);
        return assignmentId;
    }

    private void quarantineAssignment(JdbcTemplate jdbcTemplate, UUID assignmentId) {
        jdbcTemplate.update("""
                UPDATE bed_assignments
                SET integrity_status = 'QUARANTINED',
                    quarantined_at = CURRENT_TIMESTAMP,
                    quarantine_reason = 'Conflit historique validé par le test de migration',
                    quarantined_by = 'postgresql-migration-test'
                WHERE id = ?
                """, assignmentId);
    }

    private long unresolvedOverlapCount(JdbcTemplate jdbcTemplate) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM bed_assignments left_assignment
                JOIN bed_assignments right_assignment
                  ON left_assignment.organization_id = right_assignment.organization_id
                 AND left_assignment.bed_id = right_assignment.bed_id
                 AND left_assignment.id < right_assignment.id
                WHERE left_assignment.integrity_status = 'VALID'
                  AND right_assignment.integrity_status = 'VALID'
                  AND tsrange(
                        left_assignment.assigned_at,
                        COALESCE(left_assignment.released_at, 'infinity'::timestamp),
                        '[)')
                      &&
                      tsrange(
                        right_assignment.assigned_at,
                        COALESCE(right_assignment.released_at, 'infinity'::timestamp),
                        '[)')
                """, Long.class);
        return count == null ? 0L : count;
    }

    private int constraintCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = 'ex_bed_assignments_valid_period_no_overlap'
                  AND contype = 'x'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int extensionCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_extension
                WHERE extname = 'btree_gist'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int quarantinedAssignmentCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM bed_assignments
                WHERE integrity_status = 'QUARANTINED'
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private String integrityStatus(JdbcTemplate jdbcTemplate, UUID assignmentId) {
        return jdbcTemplate.queryForObject("""
                SELECT integrity_status
                FROM bed_assignments
                WHERE id = ?
                """, String.class, assignmentId);
    }

    private String allMessages(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage()).append('\n');
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private record TestFixture(
            UUID organizationId,
            UUID firstBedId,
            UUID secondBedId,
            UUID firstHospitalizationId,
            UUID secondHospitalizationId,
            UUID thirdHospitalizationId,
            UUID fourthHospitalizationId,
            UUID fifthHospitalizationId,
            Instant referenceTime) {
    }
}
