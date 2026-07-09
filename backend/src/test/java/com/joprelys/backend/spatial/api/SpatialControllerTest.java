package com.joprelys.backend.spatial.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.*;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SpatialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalizationRepository hospitalizationRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private BedAssignmentRepository bedAssignmentRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity org;
    private UserAccountEntity doctor;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private VisitEntity visitA;
    private VisitEntity visitB;
    private HospitalizationEntity hospA;
    private HospitalizationEntity hospB;
    private String tokenDoctor;

    private WardEntity ward;
    private RoomEntity room;
    private BedEntity bedFree;
    private BedEntity bedOccupied;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM bed_assignments");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM beds");
        jdbcTemplate.update("DELETE FROM rooms");
        jdbcTemplate.update("DELETE FROM wards");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create Org
        org = new OrganizationEntity("Clinique Spatiale", "spatial@joprelys.local", "123456", "Street Spatial", "Douala");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        // Create Doctor
        doctor = new UserAccountEntity("dr.spatial@joprelys.local", "Dr. Spatial", "MEDECIN", "passhash");
        doctor.setOrganizationId(org.getId());
        doctor = userAccountRepository.save(doctor);

        tokenDoctor = jwtService.createToken(doctor).value();

        // Create Patients
        patientA = new PatientEntity("DPU-S-00001", "PAT-S-001", "Alice Spatial", "FEMININ", LocalDate.of(1990, 5, 10), "+237699999991", "Douala", "Akwa", "Street A", "Bob", "+237699445566", "Aucune", "Aucun");
        patientB = new PatientEntity("DPU-S-00002", "PAT-S-002", "Bob Spatial", "MASCULIN", LocalDate.of(1985, 7, 20), "+237699999992", "Douala", "Akwa", "Street B", "Alice", "+237699445577", "Aucune", "Aucun");
        patientA = patientRepository.save(patientA);
        patientB = patientRepository.save(patientB);

        // Create Visits
        visitA = new VisitEntity(patientA, "VIS-S-001", "Motif A", "Général", "MÉDECINE GÉNÉRALE", doctor.getId(), Instant.now());
        visitB = new VisitEntity(patientB, "VIS-S-002", "Motif B", "Général", "MÉDECINE GÉNÉRALE", doctor.getId(), Instant.now());
        visitA = visitRepository.save(visitA);
        visitB = visitRepository.save(visitB);

        // Create Ward, Room, Beds
        ward = new WardEntity("Médecine Hommes");
        ward.setOrganizationId(org.getId());
        ward = wardRepository.save(ward);

        room = new RoomEntity(ward, "Chambre 10", 2, "STANDARD");
        room.setOrganizationId(org.getId());
        room = roomRepository.save(room);

        bedFree = new BedEntity(room, "Lit 10-A");
        bedFree.setOrganizationId(org.getId());
        bedFree = bedRepository.save(bedFree);

        bedOccupied = new BedEntity(room, "Lit 10-B");
        bedOccupied.setOrganizationId(org.getId());
        bedOccupied.setStatus(BedStatus.OCCUPIED);
        bedOccupied = bedRepository.save(bedOccupied);

        // Create Hospitalization for Patient A (admitted in bedOccupied initially)
        hospA = new HospitalizationEntity(
                patientA.getId(),
                "Médecine Hommes",
                "Chambre 10",
                "Lit 10-B",
                "Observation générale",
                "HOSP-S-001",
                visitA.getId(),
                doctor.getId()
        );
        hospA = hospitalizationRepository.save(hospA);

        // Create initial assignment
        BedAssignmentEntity assignmentA = new BedAssignmentEntity(hospA.getId(), bedOccupied);
        assignmentA.setOrganizationId(org.getId());
        bedAssignmentRepository.save(assignmentA);

        // Create Hospitalization for Patient B (awaiting bed assignment / free bed)
        hospB = new HospitalizationEntity(
                patientB.getId(),
                "Médecine Hommes",
                "Chambre 10",
                "Temp",
                "Fièvre",
                "HOSP-S-002",
                visitB.getId(),
                doctor.getId()
        );
        hospB = hospitalizationRepository.save(hospB);

        TenantContext.clear();
    }

    @Test
    void testListWardsAndOccupancy() throws Exception {
        mockMvc.perform(get("/api/spatial/wards")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Médecine Hommes"));

        mockMvc.perform(get("/api/spatial/wards/" + ward.getId() + "/occupancy")
                .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Médecine Hommes"))
                .andExpect(jsonPath("$.totalBedsCount").value(2))
                .andExpect(jsonPath("$.occupiedBedsCount").value(1))
                .andExpect(jsonPath("$.rooms[0].roomNumber").value("Chambre 10"))
                .andExpect(jsonPath("$.rooms[0].beds.length()").value(2));
    }

    @Test
    void testUpdateBedStatus() throws Exception {
        String statusRequest = """
                {
                    "status": "MAINTENANCE"
                }
                """;

        mockMvc.perform(post("/api/spatial/beds/" + bedFree.getId() + "/status")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    void testTransferPatientSuccess() throws Exception {
        String transferRequest = String.format("""
                {
                    "hospitalizationId": "%s",
                    "newBedId": "%s"
                }
                """, hospA.getId(), bedFree.getId());

        mockMvc.perform(post("/api/spatial/transfers")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bedId").value(bedFree.getId().toString()));

        // Verify old bed is CLEANING and new bed is OCCUPIED
        TenantContext.setTenantId(org.getId());
        BedEntity updatedOldBed = bedRepository.findById(bedOccupied.getId()).orElseThrow();
        BedEntity updatedNewBed = bedRepository.findById(bedFree.getId()).orElseThrow();
        HospitalizationEntity updatedHosp = hospitalizationRepository.findById(hospA.getId()).orElseThrow();
        TenantContext.clear();

        assert updatedOldBed.getStatus() == BedStatus.CLEANING;
        assert updatedNewBed.getStatus() == BedStatus.OCCUPIED;
        assert "Chambre 10".equals(updatedHosp.getRoomNumber());
        assert "Lit 10-A".equals(updatedHosp.getBedNumber());
    }

    @Test
    void testTransferPatientToOccupiedBedThrowsConflict() throws Exception {
        String transferRequest = String.format("""
                {
                    "hospitalizationId": "%s",
                    "newBedId": "%s"
                }
                """, hospB.getId(), bedOccupied.getId());

        mockMvc.perform(post("/api/spatial/transfers")
                .header("Authorization", "Bearer " + tokenDoctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferRequest))
                .andExpect(status().isConflict());
    }

    @Test
    void testConcurrencyTransferThrowsConflict() throws Exception {
        final int numThreads = 2;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(numThreads);

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        // Pre-run setup for parallel transfer to the same bedFree
        // We will try to transfer hospA and hospB to bedFree in parallel
        final String transferRequestA = String.format("""
                {
                    "hospitalizationId": "%s",
                    "newBedId": "%s"
                }
                """, hospA.getId(), bedFree.getId());

        final String transferRequestB = String.format("""
                {
                    "hospitalizationId": "%s",
                    "newBedId": "%s"
                }
                """, hospB.getId(), bedFree.getId());

        executor.submit(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/spatial/transfers")
                        .header("Authorization", "Bearer " + tokenDoctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequestA))
                        .andReturn().getResponse().getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    failureCount.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                finishLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/spatial/transfers")
                        .header("Authorization", "Bearer " + tokenDoctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequestB))
                        .andReturn().getResponse().getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    failureCount.incrementAndGet();
                }
            } catch (Exception ignored) {
            } finally {
                finishLatch.countDown();
            }
        });

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // One should succeed, one should fail (either 409 from optimistic lock or 409 from status check)
        assert successCount.get() == 1;
        assert failureCount.get() == 1;
    }
}
