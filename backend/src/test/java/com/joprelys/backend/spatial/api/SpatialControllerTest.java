package com.joprelys.backend.spatial.api;

import static org.hamcrest.Matchers.contains;

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
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.spatial.application.SpatialService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SpatialControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalizationRepository hospitalizationRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private OrganizationalUnitRepository organizationalUnitRepository;
    @Autowired private FacilitySpaceRepository spaceRepository;
    @Autowired private InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    @Autowired private OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private BedAssignmentRepository bedAssignmentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    @Autowired private SpatialService spatialService;
    @Autowired private JsonMapper jsonMapper;

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

    private OrganizationalUnitEntity unit;
    private FacilitySpaceEntity space;
    private BedEntity bedFree;
    private BedEntity bedOccupied;
    private BedEntity bedSourceB;

    @BeforeEach
    void setUp() {
        clearTestData();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        org = organizationRepository.save(new OrganizationEntity(
                "Clinique Spatiale " + suffix,
                "spatial-" + suffix + "@joprelys.local",
                "123456", "Street Spatial", "Douala"));
        TenantContext.setTenantId(org.getId());

        doctor = new UserAccountEntity("dr.spatial-" + suffix + "@joprelys.local", "Dr. Spatial", "MEDECIN", "passhash");
        doctor.setOrganizationId(org.getId());
        doctor = userAccountRepository.save(doctor);
        tokenDoctor = jwtService.createToken(doctor).value();

        UserAccountEntity admin = new UserAccountEntity(
                "admin.spatial-" + suffix + "@joprelys.local", "Admin Spatial", "ADMIN_CLINIQUE", "passhash");
        admin.setOrganizationId(org.getId());
        admin = userAccountRepository.save(admin);
        tokenAdmin = jwtService.createToken(admin).value();

        UserAccountEntity platformAdmin = new UserAccountEntity(
                "platform.spatial-" + suffix + "@joprelys.local", "Platform Spatial", "SUPER_ADMIN", "passhash");
        platformAdmin = userAccountRepository.save(platformAdmin);
        tokenPlatformAdmin = jwtService.createToken(platformAdmin).value();

        patientA = patientRepository.save(new PatientEntity(
                "DPU-S-A-" + suffix, "PAT-S-A-" + suffix, "Alice Spatial", "FEMININ",
                LocalDate.of(1990, 5, 10), "+237699999991", "Douala", "Akwa", "Street A",
                "Bob", "+237699445566", "Aucune", "Aucun"));
        patientB = patientRepository.save(new PatientEntity(
                "DPU-S-B-" + suffix, "PAT-S-B-" + suffix, "Bob Spatial", "MASCULIN",
                LocalDate.of(1985, 7, 20), "+237699999992", "Douala", "Akwa", "Street B",
                "Alice", "+237699445577", "Aucune", "Aucun"));

        visitA = visitRepository.save(new VisitEntity(
                patientA, "VIS-S-A-" + suffix, "Motif A", "Général", "Médecine",
                doctor.getId(), Instant.now()));
        visitB = visitRepository.save(new VisitEntity(
                patientB, "VIS-S-B-" + suffix, "Motif B", "Général", "Médecine",
                doctor.getId(), Instant.now()));

        unit = organizationalUnitRepository.saveAndFlush(new OrganizationalUnitEntity(
                org.getId(), null, "CARE_SPATIAL_" + suffix.toUpperCase(),
                "Unité hospitalisation", OrganizationalUnitType.CARE_UNIT, null));
        space = spaceRepository.saveAndFlush(new FacilitySpaceEntity(
                org.getId(), null, "ROOM_10_" + suffix.toUpperCase(), "Chambre 10", "HOSPITAL_ROOM"));
        inpatientSpaceProfileRepository.saveAndFlush(new InpatientSpaceProfileEntity(
                space.getId(), org.getId(), "HOSPITAL_ROOM", "STANDARD"));
        unitSpaceAssignmentRepository.saveAndFlush(new OrganizationalUnitSpaceAssignmentEntity(
                org.getId(), unit.getId(), space.getId(), Instant.now().minusSeconds(3600), null));

        bedFree = bedRepository.saveAndFlush(new BedEntity(space, "Lit 10-A"));
        bedOccupied = new BedEntity(space, "Lit 10-B");
        bedOccupied.setStatus(BedStatus.OCCUPIED);
        bedOccupied = bedRepository.saveAndFlush(bedOccupied);
        bedSourceB = new BedEntity(space, "Lit 10-C");
        bedSourceB.setStatus(BedStatus.OCCUPIED);
        bedSourceB = bedRepository.saveAndFlush(bedSourceB);

        hospA = new HospitalizationEntity(
                patientA.getId(), unit.getId(), space.getId(), bedOccupied.getId(),
                "Unité hospitalisation", space.getName(), bedOccupied.getBedNumber(),
                "Observation générale", "HOSP-S-A-" + suffix, visitA.getId(), null, doctor.getId());
        hospA.setOrganizationId(org.getId());
        hospA = hospitalizationRepository.saveAndFlush(hospA);
        BedAssignmentEntity assignmentA = new BedAssignmentEntity(hospA.getId(), bedOccupied);
        assignmentA.setOrganizationId(org.getId());
        bedAssignmentRepository.saveAndFlush(assignmentA);

        hospB = new HospitalizationEntity(
                patientB.getId(), unit.getId(), space.getId(), bedSourceB.getId(),
                "Unité hospitalisation", space.getName(), bedSourceB.getBedNumber(),
                "Fièvre", "HOSP-S-B-" + suffix, visitB.getId(), null, doctor.getId());
        hospB.setOrganizationId(org.getId());
        hospB = hospitalizationRepository.saveAndFlush(hospB);
        BedAssignmentEntity assignmentB = new BedAssignmentEntity(hospB.getId(), bedSourceB);
        assignmentB.setOrganizationId(org.getId());
        bedAssignmentRepository.saveAndFlush(assignmentB);

        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        clearTestData();
    }

    private void clearTestData() {
        TenantContext.clear();
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM bed_state_changes");
        jdbcTemplate.update("DELETE FROM bed_assignments");
        jdbcTemplate.update("DELETE FROM hospitalizations");
        jdbcTemplate.update("DELETE FROM beds");
        jdbcTemplate.update("DELETE FROM organizational_unit_space_assignments");
        jdbcTemplate.update("DELETE FROM inpatient_space_profiles");
        jdbcTemplate.update("DELETE FROM facility_spaces");
        jdbcTemplate.update("DELETE FROM facility_location_nodes");
        jdbcTemplate.update("DELETE FROM organizational_units");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void shouldExposeSpaceOccupancy() throws Exception {
        mockMvc.perform(get("/api/spatial/spaces/" + space.getId() + "/occupancy")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceName").value("Chambre 10"))
                .andExpect(jsonPath("$.installedBeds").value(3))
                .andExpect(jsonPath("$.occupiedBeds").value(2))
                .andExpect(jsonPath("$.availableBeds").value(1))
                .andExpect(jsonPath("$.beds.length()").value(3));
    }

    @Test
    void occupancyShouldOnlyCountOperationallyAvailableBeds() throws Exception {
        TenantContext.setTenantId(org.getId());
        try {
            BedEntity cleaningBed = new BedEntity(space, "Lit 10-D");
            cleaningBed.setStatus(BedStatus.CLEANING);
            bedRepository.saveAndFlush(cleaningBed);
            BedEntity maintenanceBed = new BedEntity(space, "Lit 10-E");
            maintenanceBed.setStatus(BedStatus.MAINTENANCE);
            bedRepository.saveAndFlush(maintenanceBed);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(get("/api/spatial/spaces/" + space.getId() + "/occupancy")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installedBeds").value(5))
                .andExpect(jsonPath("$.occupiedBeds").value(2))
                .andExpect(jsonPath("$.availableBeds").value(1));
    }

    @Test
    void testUpdateBedStatus() throws Exception {
        String statusRequest = """
                {"status":"MAINTENANCE"}
                """;
        mockMvc.perform(post("/api/spatial/beds/" + bedFree.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    void adminShouldCreateLocationSpaceProfileAndBed() throws Exception {
        String locationJson = mockMvc.perform(post("/api/spatial/configuration/locations")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BLD_CARDIO\",\"name\":\"Bâtiment cardio\",\"nodeType\":\"BUILDING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BLD_CARDIO"))
                .andReturn().getResponse().getContentAsString();
        String locationId = jsonMapper.readTree(locationJson).get("id").asString();

        String spaceJson = mockMvc.perform(post("/api/spatial/configuration/spaces")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"locationNodeId":"%s","code":"CARDIO_201","name":"Chambre 201","spaceTypeCode":"HOSPITAL_ROOM","enableInpatientProfile":true}
                                """.formatted(locationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inpatientProfile").value(true))
                .andReturn().getResponse().getContentAsString();
        String spaceId = jsonMapper.readTree(spaceJson).get("id").asString();

        String bedBody = "{\"spaceId\":\"%s\",\"bedNumber\":\"201-A\"}".formatted(spaceId);
        mockMvc.perform(post("/api/spatial/configuration/beds")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bedBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceId").value(spaceId));

        mockMvc.perform(post("/api/spatial/configuration/beds")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bedBody))
                .andExpect(status().isConflict());
    }

    @Test
    void nonInpatientSpaceShouldRejectProfileAndBeds() throws Exception {
        mockMvc.perform(post("/api/spatial/configuration/spaces")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WAIT_01\",\"name\":\"Attente 01\",\"spaceTypeCode\":\"WAITING_ROOM\",\"enableInpatientProfile\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configurationShouldBeForbiddenToDoctorAndProtectOccupiedBed() throws Exception {
        mockMvc.perform(get("/api/spatial/configuration/locations")
                        .header("Authorization", "Bearer " + tokenDoctor))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/spatial/configuration/beds/" + bedOccupied.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict());
    }

    @Test
    void platformAdministratorShouldSelectClinicScope() throws Exception {
        mockMvc.perform(get("/api/spatial/configuration/locations")
                        .header("Authorization", "Bearer " + tokenPlatformAdmin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/spatial/configuration/spaces")
                        .queryParam("organizationId", org.getId().toString())
                        .header("Authorization", "Bearer " + tokenPlatformAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Chambre 10"));
    }

    @Test
    void testTransferPatientSuccess() throws Exception {
        String transferRequest = transferRequest(hospA.getId(), bedFree.getId());
        mockMvc.perform(post("/api/spatial/transfers")
                        .header("Authorization", "Bearer " + tokenDoctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bedId").value(bedFree.getId().toString()));

        TenantContext.setTenantId(org.getId());
        try {
            BedEntity updatedOldBed = bedRepository.findById(bedOccupied.getId()).orElseThrow();
            BedEntity updatedNewBed = bedRepository.findById(bedFree.getId()).orElseThrow();
            HospitalizationEntity updatedHosp = hospitalizationRepository.findById(hospA.getId()).orElseThrow();
            assertEquals(BedStatus.CLEANING, updatedOldBed.getStatus());
            assertEquals(BedStatus.OCCUPIED, updatedNewBed.getStatus());
            assertEquals(space.getId(), updatedHosp.getCurrentSpaceId());
            assertEquals(bedFree.getId(), updatedHosp.getCurrentBedId());
            assertEquals("Lit 10-A", updatedHosp.getBedNumber());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void testTransferPatientToOccupiedBedThrowsConflict() throws Exception {
        mockMvc.perform(post("/api/spatial/transfers")
                        .header("Authorization", "Bearer " + tokenDoctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(hospB.getId(), bedOccupied.getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void transferShouldRollbackWhenTargetBedHasHiddenActiveAssignment() throws Exception {
        TenantContext.setTenantId(org.getId());
        try {
            BedAssignmentEntity currentB = bedAssignmentRepository.findActiveByHospitalizationId(hospB.getId()).orElseThrow();
            currentB.releaseAt(Instant.now());
            bedAssignmentRepository.saveAndFlush(currentB);
            bedSourceB.setStatus(BedStatus.CLEANING);
            bedRepository.saveAndFlush(bedSourceB);

            BedAssignmentEntity hiddenAssignment = new BedAssignmentEntity(hospB.getId(), bedFree);
            hiddenAssignment.setOrganizationId(org.getId());
            bedAssignmentRepository.saveAndFlush(hiddenAssignment);
        } finally {
            TenantContext.clear();
        }

        mockMvc.perform(post("/api/spatial/transfers")
                        .header("Authorization", "Bearer " + tokenDoctor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest(hospA.getId(), bedFree.getId())))
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

    private String transferRequest(UUID hospitalizationId, UUID bedId) {
        return """
                {"hospitalizationId":"%s","targetServiceUnitId":"%s","targetSpaceId":"%s","targetBedId":"%s"}
                """.formatted(hospitalizationId, unit.getId(), space.getId(), bedId);
    }

    private int executeConcurrentTransfer(
            UUID hospitalizationId,
            UUID bedId,
            CountDownLatch startLatch) throws InterruptedException {
        TenantContext.setTenantId(org.getId());
        try {
            startLatch.await();
            spatialService.transferPatient(hospitalizationId, unit.getId(), space.getId(), bedId);
            return 200;
        } catch (ResponseStatusException exception) {
            return exception.getStatusCode().value();
        } finally {
            TenantContext.clear();
        }
    }
    @Test
    void clinicalAndAmbulatorySpaceTypesSupportInpatientProfiles() throws Exception {
        for (String code : java.util.List.of("NEONATAL_ROOM", "EMERGENCY_BOX", "DAY_HOSPITAL",
                "DIALYSIS_STATION", "CHEMOTHERAPY_STATION", "AMBULATORY_SURGERY")) {
            mockMvc.perform(get("/api/spatial/configuration/space-types")
                    .header("Authorization", "Bearer " + tokenAdmin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.code == '" + code + "')].inpatientCompatible").value(contains(true)));
            String response = mockMvc.perform(post("/api/spatial/configuration/spaces")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"code":"TEST-%s","name":"Espace %s","spaceTypeCode":"%s","enableInpatientProfile":true}
                        """.formatted(code, code, code)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.inpatientProfile").value(true))
                    .andReturn().getResponse().getContentAsString();
            String spaceId = jsonMapper.readTree(response).get("id").asString();
            mockMvc.perform(post("/api/spatial/configuration/beds")
                    .header("Authorization", "Bearer " + tokenAdmin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"spaceId\":\"" + spaceId + "\",\"bedNumber\":\"TEST-01\"}"))
                    .andExpect(status().isCreated());
        }
    }

}
