package com.joprelys.backend.spatial.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.joprelys.backend.spatial.application.SpatialService;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

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

    @Autowired
    private SpatialService spatialService;

    @Autowired
    private JsonMapper jsonMapper;

    private OrganizationEntity org;
    private UserAccountEntity doctor;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private VisitEntity visitA;
    private VisitEntity visitB;
    private HospitalizationEntity hospA;
    private HospitalizationEntity hospB;
    private String tokenDoctor;
    private String tokenAdmin;
    private String tokenPlatformAdmin;

    private WardEntity ward;
    private RoomEntity room;
    private BedEntity bedFree;
    private BedEntity bedOccupied;

    @BeforeEach
    void setUp() {
        clearTestData();

        org = new OrganizationEntity(
                "Clinique Spatiale",
                "spatial@joprelys.local",
                "123456",
                "Street Spatial",
                "Douala");
        org = organizationRepository.save(org);

        TenantContext.setTenantId(org.getId());

        doctor = new UserAccountEntity("dr.spatial@joprelys.local", "Dr. Spatial", "MEDECIN", "passhash");
        doctor.setOrganizationId(org.getId());
        doctor = userAccountRepository.save(doctor);

        tokenDoctor = jwtService.createToken(doctor).value();

        UserAccountEntity admin = new UserAccountEntity(
                "admin.spatial@joprelys.local", "Admin Spatial", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        UserAccountEntity platformAdmin = new UserAccountEntity(
                "platform.spatial@joprelys.local", "Platform Spatial", "SUPER_ADMIN", "passhash");
        platformAdmin = userAccountRepository.save(platformAdmin);
        tokenPlatformAdmin = jwtService.createToken(platformAdmin).value();

        patientA = new PatientEntity(
                "DPU-S-00001",
                "PAT-S-001",
                "Alice Spatial",
                "FEMININ",
                LocalDate.of(1990, 5, 10),
                "+237699999991",
                "Douala",
                "Akwa",
                "Street A",
                "Bob",
                "+237699445566",
                "Aucune",
                "Aucun");
        patientB = new PatientEntity(
                "DPU-S-00002",
                "PAT-S-002",
                "Bob Spatial",
                "MASCULIN",
                LocalDate.of(1985, 7, 20),
                "+237699999992",
                "Douala",
                "Akwa",
                "Street B",
                "Alice",
                "+237699445577",
                "Aucune",
                "Aucun");
        patientA = patientRepository.save(patientA);
        patientB = patientRepository.save(patientB);

        visitA = new VisitEntity(
                patientA,
                "VIS-S-001",
                "Motif A",
                "Général",
                "MÉDECINE GÉNÉRALE",
                doctor.getId(),
                Instant.now());
        visitB = new VisitEntity(
                patientB,
                "VIS-S-002",
                "Motif B",
                "Général",
                "MÉDECINE GÉNÉRALE",
                doctor.getId(),
                Instant.now());
        visitA = visitRepository.save(visitA);
        visitB = visitRepository.save(visitB);

        ward = new WardEntity("Médecine Hommes", HospitalServiceType.HOSPITALIZATION);
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

        hospA = new HospitalizationEntity(
                patientA.getId(),
                "Médecine Hommes",
                "Chambre 10",
                "Lit 10-B",
                "Observation générale",
                "HOSP-S-001",
                visitA.getId(),
                doctor.getId());
        hospA = hospitalizationRepository.save(hospA);

        BedAssignmentEntity assignmentA = new BedAssignmentEntity(hospA.getId(), bedOccupied);
        assignmentA.setOrganizationId(org.getId());
        bedAssignmentRepository.save(assignmentA);

        hospB = new HospitalizationEntity(
                patientB.getId(),
                "Médecine Hommes",
                "Chambre 10",
                "Temp",
                "Fièvre",
                "HOSP-S-002",
                visitB.getId(),
                doctor.getId());
        hospB = hospitalizationRepository.save(hospB);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        clearTestData();
    }

    private void clearTestData() {
        TenantContext.clear();
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
                .andExpect(jsonPath("$.availableBedsCount").value(1))
                .andExpect(jsonPath("$.rooms[0].roomNumber").value("Chambre 10"))
                .andExpect(jsonPath("$.rooms[0].beds.length()").value(2));
    }

    @Test
    void occupancyShouldOnlyCountFreeBedsAsAvailable() throws Exception {
        TenantContext.setTenantId(org.getId());
        try {
            RoomEntity unavailableRoom = new RoomEntity(ward, "Chambre 11", 2, "STANDARD");
            unavailableRoom.setOrganizationId(org.getId());
            unavailableRoom = roomRepository.save(unavailableRoom);

            BedEntity cleaningBed = new BedEntity(unavailableRoom, "Lit 11-A");
            cleaningBed.setOrganizationId(org.getId());
            cleaningBed.setStatus(BedStatus.CLEANING);
            bedRepository.save(cleaningBed);

            BedEntity maintenanceBed = new BedEntity(unavailableRoom, "Lit 11-B");
            maintenanceBed.setOrganizationId(org.getId());
            maintenanceBed.setStatus(BedStatus.MAINTENANCE);
            bedRepository.save(maintenanceBed);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/spatial/wards/" + ward.getId() + "/occupancy")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBedsCount").value(4))
                .andExpect(jsonPath("$.occupiedBedsCount").value(1))
                .andExpect(jsonPath("$.availableBedsCount").value(1));
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
    void adminShouldCreateHospitalStructureAndRespectRoomCapacity() throws Exception {
        String wardBody = """
                {"name":"Cardiologie","serviceType":"HOSPITALIZATION"}
                """;
        String wardJson = mockMvc.perform(post("/api/spatial/configuration/wards")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wardBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cardiologie"))
                .andExpect(jsonPath("$.serviceType").value("HOSPITALIZATION"))
                .andExpect(jsonPath("$.allowsRooms").value(true))
                .andReturn().getResponse().getContentAsString();
        String wardId = jsonMapper.readTree(wardJson).get("id").asString();

        String roomBody = """
                {"wardId":"%s","roomNumber":"201","capacity":1,"comfortLevel":"STANDARD"}
                """.formatted(wardId);
        String roomJson = mockMvc.perform(post("/api/spatial/configuration/rooms")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String roomId = jsonMapper.readTree(roomJson).get("id").asString();

        String firstBed = "{\"roomId\":\"%s\",\"bedNumber\":\"201-A\"}".formatted(roomId);
        mockMvc.perform(post("/api/spatial/configuration/beds")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBed))
                .andExpect(status().isCreated());

        String secondBed = "{\"roomId\":\"%s\",\"bedNumber\":\"201-B\"}".formatted(roomId);
        mockMvc.perform(post("/api/spatial/configuration/beds")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBed))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/spatial/configuration")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wards[?(@.name == 'Cardiologie')].rooms[0].beds[0].bedNumber")
                        .value("201-A"));
    }

    @Test
    void administrativeServiceShouldRejectRoomCreation() throws Exception {
        String wardJson = mockMvc.perform(post("/api/spatial/configuration/wards")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Caisse\",\"serviceType\":\"ADMINISTRATIVE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceType").value("ADMINISTRATIVE"))
                .andExpect(jsonPath("$.allowsRooms").value(false))
                .andReturn().getResponse().getContentAsString();
        String cashDeskId = jsonMapper.readTree(wardJson).get("id").asString();

        String roomBody = """
                {"wardId":"%s","roomNumber":"CAISSE-01","capacity":1,"comfortLevel":"STANDARD"}
                """.formatted(cashDeskId);

        mockMvc.perform(post("/api/spatial/configuration/rooms")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isConflict());
    }

    @Test
    void configurationShouldBeForbiddenToDoctorAndProtectOccupiedBed() throws Exception {
        mockMvc.perform(get("/api/spatial/configuration")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/spatial/configuration/beds/" + bedOccupied.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    void platformAdministratorShouldSelectClinicScope() throws Exception {
        mockMvc.perform(get("/api/spatial/configuration")
                        .header("Authorization", "Bearer " + tokenPlatformAdmin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/spatial/configuration")
                        .queryParam("organizationId", org.getId().toString())
                        .header("Authorization", "Bearer " + tokenPlatformAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wards[0].name").value("Médecine Hommes"))
                .andExpect(jsonPath("$.wards[0].serviceType").value("HOSPITALIZATION"))
                .andExpect(jsonPath("$.wards[0].allowsRooms").value(true));
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
    void transferShouldRollbackWhenTargetBedHasHiddenActiveAssignment() throws Exception {
        TenantContext.setTenantId(org.getId());
        try {
            BedAssignmentEntity hiddenAssignment = new BedAssignmentEntity(hospB.getId(), bedFree);
            hiddenAssignment.setOrganizationId(org.getId());
            bedAssignmentRepository.saveAndFlush(hiddenAssignment);
        } finally {
            TenantContext.clear();
        }

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
                .andExpect(status().isConflict());

        TenantContext.setTenantId(org.getId());
        try {
            assertEquals(BedStatus.FREE, bedRepository.findById(bedFree.getId()).orElseThrow().getStatus());
            assertEquals(BedStatus.OCCUPIED, bedRepository.findById(bedOccupied.getId()).orElseThrow().getStatus());
            assertTrue(bedAssignmentRepository.findActiveByHospitalizationId(hospA.getId()).isPresent());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void testConcurrencyTransferThrowsConflict() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            Future<Integer> firstTransfer = executor.submit(
                    () -> executeConcurrentTransfer(hospA.getId(), bedFree.getId(), startLatch));
            Future<Integer> secondTransfer = executor.submit(
                    () -> executeConcurrentTransfer(hospB.getId(), bedFree.getId(), startLatch));

            startLatch.countDown();
            int firstStatus = firstTransfer.get(10, TimeUnit.SECONDS);
            int secondStatus = secondTransfer.get(10, TimeUnit.SECONDS);

            int successCount = (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0);
            int conflictCount = (firstStatus == 409 ? 1 : 0) + (secondStatus == 409 ? 1 : 0);
            assertEquals(1, successCount, "Une seule hospitalisation doit pouvoir réserver le lit libre.");
            assertEquals(1, conflictCount, "La seconde réservation concurrente doit être refusée avec un conflit métier.");
        } finally {
            executor.shutdownNow();
        }
    }

    private int executeConcurrentTransfer(
            UUID hospitalizationId,
            UUID bedId,
            CountDownLatch startLatch) throws InterruptedException {
        TenantContext.setTenantId(org.getId());
        try {
            startLatch.await();
            spatialService.transferPatient(hospitalizationId, bedId);
            return 200;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        } finally {
            TenantContext.clear();
        }
    }
}
