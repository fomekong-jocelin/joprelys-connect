package com.joprelys.backend.spatial.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BedAssignmentRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private WardRepository wardRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private BedRepository bedRepository;
    @Autowired
    private BedAssignmentRepository bedAssignmentRepository;
    @Autowired
    private HospitalizationRepository hospitalizationRepository;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private OrganizationEntity otherOrganization;
    private BedEntity bed;
    private BedEntity secondBed;
    private HospitalizationEntity hospitalization;
    private HospitalizationEntity otherHospitalization;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique intégrité lit " + suffix,
                "bed-integrity-" + suffix + "@joprelys.local",
                "000",
                "Adresse test",
                "Douala"));
        otherOrganization = organizationRepository.save(new OrganizationEntity(
                "Autre clinique intégrité lit " + suffix,
                "other-bed-integrity-" + suffix + "@joprelys.local",
                "000",
                "Autre adresse test",
                "Yaoundé"));
        TenantContext.setTenantId(organization.getId());

        WardEntity ward = wardRepository.save(new WardEntity(
                "Hospitalisation " + suffix,
                HospitalServiceType.HOSPITALIZATION));
        RoomEntity room = roomRepository.save(new RoomEntity(ward, "101", 2, "STANDARD"));
        bed = bedRepository.save(new BedEntity(room, "101-A"));
        secondBed = bedRepository.save(new BedEntity(room, "101-B"));

        PatientEntity patient = patientRepository.save(new PatientEntity(
                "DPU-BED-" + suffix,
                "PAT-BED-" + suffix,
                "Patient intégrité lit",
                "MASCULIN",
                LocalDate.of(1990, 1, 1),
                "+237600000000",
                "Douala",
                null,
                null,
                null,
                null,
                null,
                null));
        VisitEntity visit = visitRepository.save(new VisitEntity(
                patient,
                "VIS-BED-" + suffix,
                "Test intégrité lit",
                "HOSPITALISATION"));
        hospitalization = hospitalizationRepository.save(new HospitalizationEntity(
                patient.getId(),
                ward.getName(),
                room.getRoomNumber(),
                bed.getBedNumber(),
                "Test intégrité",
                "HOSP-BED-1-" + suffix,
                visit.getId(),
                null));
        otherHospitalization = hospitalizationRepository.save(new HospitalizationEntity(
                patient.getId(),
                ward.getName(),
                room.getRoomNumber(),
                secondBed.getBedNumber(),
                "Test second séjour",
                "HOSP-BED-2-" + suffix,
                visit.getId(),
                null));
    }

    @AfterEach
    void tearDown() {
        if (organization == null) {
            TenantContext.clear();
            return;
        }
        bedAssignmentRepository.deleteAll();
        hospitalizationRepository.deleteAll();
        visitRepository.deleteAll();
        patientRepository.deleteAll();
        bedRepository.deleteAll();
        roomRepository.deleteAll();
        wardRepository.deleteAll();
        TenantContext.clear();
        organizationRepository.deleteById(organization.getId());
        organizationRepository.deleteById(otherOrganization.getId());
    }

    @Test
    void shouldRejectTwoActiveAssignmentsForSameBed() {
        bedAssignmentRepository.saveAndFlush(activeAssignment(hospitalization.getId(), bed));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> bedAssignmentRepository.saveAndFlush(activeAssignment(otherHospitalization.getId(), bed)),
                "L'index unique doit refuser une seconde affectation active du même lit.");
    }

    @Test
    void shouldRejectTwoActiveAssignmentsForSameHospitalization() {
        bedAssignmentRepository.saveAndFlush(activeAssignment(hospitalization.getId(), bed));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> bedAssignmentRepository.saveAndFlush(activeAssignment(hospitalization.getId(), secondBed)),
                "L'index unique doit refuser deux lits actifs pour le même séjour.");
    }

    @Test
    void shouldAllowNewAssignmentAfterPreviousRelease() {
        BedAssignmentEntity previous = bedAssignmentRepository.saveAndFlush(
                activeAssignment(hospitalization.getId(), bed));
        previous.releaseAt(Instant.now());
        bedAssignmentRepository.saveAndFlush(previous);

        BedAssignmentEntity current = bedAssignmentRepository.saveAndFlush(
                activeAssignment(hospitalization.getId(), bed));

        assertNull(previous.getActiveBedId());
        assertNull(previous.getActiveHospitalizationId());
        assertEquals(bed.getId(), current.getActiveBedId());
        assertEquals(hospitalization.getId(), current.getActiveHospitalizationId());
    }

    @Test
    void shouldRejectAnActiveRowWithoutConsistentMarker() {
        String sql = """
                INSERT INTO bed_assignments (
                    id, hospitalization_id, bed_id, assigned_at, released_at,
                    active_bed_id, active_hospitalization_id, organization_id
                ) VALUES (?, ?, ?, ?, NULL, NULL, ?, ?)
                """;

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        sql,
                        UUID.randomUUID(),
                        hospitalization.getId(),
                        bed.getId(),
                        Timestamp.from(Instant.now()),
                        hospitalization.getId(),
                        organization.getId()),
                "La contrainte CHECK doit refuser un marqueur actif nul.");
    }

    @Test
    void shouldRejectUnknownHospitalization() {
        UUID unknownHospitalizationId = UUID.randomUUID();
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO bed_assignments (
                            id, hospitalization_id, bed_id, assigned_at, released_at,
                            active_bed_id, active_hospitalization_id, organization_id
                        ) VALUES (?, ?, ?, ?, NULL, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        unknownHospitalizationId,
                        bed.getId(),
                        Timestamp.from(Instant.now()),
                        bed.getId(),
                        unknownHospitalizationId,
                        organization.getId()),
                "La FK doit refuser une affectation sans séjour existant.");
    }

    @Test
    void shouldRejectReleasedAtBeforeAssignedAtInDomainAndDatabase() {
        BedAssignmentEntity assignment = activeAssignment(hospitalization.getId(), bed);
        assertThrows(
                IllegalArgumentException.class,
                () -> assignment.releaseAt(assignment.getAssignedAt().minusSeconds(1)));

        Instant assignedAt = Instant.now();
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO bed_assignments (
                            id, hospitalization_id, bed_id, assigned_at, released_at,
                            active_bed_id, active_hospitalization_id, organization_id
                        ) VALUES (?, ?, ?, ?, ?, NULL, NULL, ?)
                        """,
                        UUID.randomUUID(),
                        hospitalization.getId(),
                        bed.getId(),
                        Timestamp.from(assignedAt),
                        Timestamp.from(assignedAt.minusSeconds(1)),
                        organization.getId()),
                "La contrainte chronologique doit refuser une période inversée.");
    }

    @Test
    void shouldProtectAssignmentHistoryFromHospitalizationDeletion() {
        bedAssignmentRepository.saveAndFlush(activeAssignment(hospitalization.getId(), bed));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> hospitalizationRepository.deleteById(hospitalization.getId()),
                "La FK restrictive doit empêcher la suppression d'un séjour référencé.");
    }

    @Test
    void shouldRejectAssignmentFromAnotherOrganization() {
        Instant assignedAt = Instant.now().minusSeconds(60);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO bed_assignments (
                            id, hospitalization_id, bed_id, assigned_at, released_at,
                            active_bed_id, active_hospitalization_id, organization_id
                        ) VALUES (?, ?, ?, ?, ?, NULL, NULL, ?)
                        """,
                        UUID.randomUUID(),
                        hospitalization.getId(),
                        bed.getId(),
                        Timestamp.from(assignedAt),
                        Timestamp.from(assignedAt.plusSeconds(30)),
                        otherOrganization.getId()),
                "Les FK composites doivent refuser une affectation d'un autre établissement.");
    }

    @Test
    void shouldExposeTenantConsistencyConstraints() {
        assertEquals(1, constraintCount("uq_hospitalizations_id_organization"));
        assertEquals(1, constraintCount("uq_beds_id_organization"));
        assertEquals(1, constraintCount("fk_bed_assignments_hospitalization_organization"));
        assertEquals(1, constraintCount("fk_bed_assignments_bed_organization"));
    }

    private Integer constraintCount(String constraintName) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE LOWER(constraint_name) = ?
                """, Integer.class, constraintName);
    }

    private BedAssignmentEntity activeAssignment(UUID hospitalizationId, BedEntity targetBed) {
        BedAssignmentEntity assignment = new BedAssignmentEntity(hospitalizationId, targetBed);
        assignment.setOrganizationId(organization.getId());
        return assignment;
    }
}
