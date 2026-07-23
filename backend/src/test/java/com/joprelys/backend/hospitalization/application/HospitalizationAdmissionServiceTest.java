package com.joprelys.backend.hospitalization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.hospitalization.api.CreateHospitalizationRequest;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.spatial.application.ActiveBedAssignmentService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedCapacityStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedReadinessStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
import com.joprelys.backend.visit.application.VisitNumberGenerator;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class HospitalizationAdmissionServiceTest {

    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditService auditService;
    @Mock private BedRepository bedRepository;
    @Mock private BedAssignmentRepository bedAssignmentRepository;
    @Mock private ActiveBedAssignmentService activeBedAssignmentService;
    @Mock private EmergencyRepository emergencyRepository;
    @Mock private PatientCanonicalResolver canonicalResolver;
    @Mock private VisitRepository visitRepository;
    @Mock private VisitNumberGenerator visitNumberGenerator;
    @Mock private OrganizationalUnitRepository unitRepository;
    @Mock private HospitalServiceCatalogRepository serviceCatalogRepository;
    @Mock private FacilitySpaceRepository spaceRepository;
    @Mock private InpatientSpaceProfileRepository inpatientProfileRepository;
    @Mock private OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;

    @InjectMocks
    private HospitalizationAdmissionService service;

    private CreateHospitalizationRequest request;
    private PatientEntity patient;
    private UUID organizationId;
    private OrganizationalUnitEntity unit;
    private FacilitySpaceEntity space;

    @BeforeEach
    void setUp() {
        request = request();
        organizationId = UUID.randomUUID();
        patient = org.mockito.Mockito.mock(PatientEntity.class);
        unit = org.mockito.Mockito.mock(OrganizationalUnitEntity.class);
        space = org.mockito.Mockito.mock(FacilitySpaceEntity.class);
    }

    @Test
    void shouldRejectAdmissionWhenBedIsNotConfiguredForPatientOrganization() {
        prepareVisitBasedAdmission();
        preparePlacementWithoutBed();
        when(bedRepository.findByIdAndOrganizationId(request.bedId(), organizationId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bedRepository, never()).claimIfAvailable(any(), any(), any(), any(), any());
        verify(hospitalizationRepository, never()).save(any());
        verify(activeBedAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    void shouldAtomicallyClaimConfiguredBedWithoutCreatingSpatialData() {
        VisitEntity visit = prepareVisitBasedAdmission();
        when(visit.getId()).thenReturn(request.visitId());
        UUID bedId = request.bedId();
        BedEntity bed = configuredBed(bedId);
        preparePlacementWithBed(bed);

        when(bedRepository.claimIfAvailable(
                bedId,
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY)).thenReturn(1);
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(hospitalizationRepository.getNextHospitalizationNumberSequenceValue()).thenReturn(42L);
        when(hospitalizationRepository.save(any(HospitalizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.admitPatient(request);

        assertNotNull(response);
        assertEquals(request.serviceUnitId(), response.currentServiceUnitId());
        assertEquals(request.spaceId(), response.currentSpaceId());
        assertEquals(request.bedId(), response.currentBedId());
        verify(bedRepository).claimIfAvailable(
                bedId,
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY);
        verify(hospitalizationRepository).save(any(HospitalizationEntity.class));
        verify(activeBedAssignmentService).assign(any(), any(), any());
    }

    @Test
    void shouldRejectAdmissionWhenAtomicBedClaimLosesTheRaceOrBedIsNotReady() {
        prepareVisitBasedAdmission();
        UUID bedId = request.bedId();
        BedEntity bed = configuredBed(bedId);
        preparePlacementWithBed(bed);

        when(bedRepository.claimIfAvailable(
                bedId,
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY)).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Le lit demandé n'est pas ouvert, prêt et disponible.", exception.getReason());
        verify(hospitalizationRepository, never()).save(any());
        verify(activeBedAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    void shouldRejectAdmissionWhenUnitDoesNotUseRequestedSpace() {
        prepareVisitBasedAdmission();
        prepareUnitAndSpace();
        when(unitSpaceAssignmentRepository.existsActiveAt(
                eq(organizationId), eq(request.serviceUnitId()), eq(request.spaceId()), any(Instant.class)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("L'unité sélectionnée n'utilise pas cet espace à la date de l'admission.", exception.getReason());
        verify(bedRepository, never()).claimIfAvailable(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectAdmissionWhenBedBelongsToAnotherSpace() {
        prepareVisitBasedAdmission();
        prepareUnitAndSpace();
        when(unitSpaceAssignmentRepository.existsActiveAt(
                eq(organizationId), eq(request.serviceUnitId()), eq(request.spaceId()), any(Instant.class)))
                .thenReturn(true);
        BedEntity bed = org.mockito.Mockito.mock(BedEntity.class);
        FacilitySpaceEntity otherSpace = org.mockito.Mockito.mock(FacilitySpaceEntity.class);
        when(otherSpace.getId()).thenReturn(UUID.randomUUID());
        when(bed.getSpace()).thenReturn(otherSpace);
        when(bedRepository.findByIdAndOrganizationId(request.bedId(), organizationId))
                .thenReturn(Optional.of(bed));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Le lit sélectionné n'appartient pas à l'espace demandé.", exception.getReason());
    }

    @Test
    void shouldCreateAVisitAndRetainEmergencyLinkWhenNoVisitWasProvided() {
        prepareCanonicalPatient();
        UUID emergencyId = UUID.randomUUID();
        CreateHospitalizationRequest emergencyRequest = new CreateHospitalizationRequest(
                request.patientId(),
                request.serviceUnitId(),
                request.spaceId(),
                request.bedId(),
                request.admissionReason(),
                null,
                emergencyId,
                request.responsiblePractitionerId());
        EmergencyEntity emergency = org.mockito.Mockito.mock(EmergencyEntity.class);
        BedEntity bed = configuredBed(request.bedId());
        preparePlacementWithBed(bed);

        when(emergencyRepository.findByIdWithPatientAndLogs(emergencyId)).thenReturn(Optional.of(emergency));
        when(emergency.getId()).thenReturn(emergencyId);
        when(emergency.getPatient()).thenReturn(patient);
        when(emergency.getOrganizationId()).thenReturn(organizationId);
        when(emergency.getChiefComplaint()).thenReturn("Altération de la conscience");
        when(emergency.getCreatedAt()).thenReturn(Instant.now());
        when(emergency.getVisitId()).thenReturn(null);
        when(visitRepository.findFirstByPatientIdAndStatusOrderByCreatedAtDesc(
                request.patientId(),
                "EN_COURS"))
                .thenReturn(Optional.empty());
        when(visitNumberGenerator.generateNextVisitNumber()).thenReturn("VIS-20260721-000001");
        when(visitRepository.save(any(VisitEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hospitalizationRepository.findByEmergencyId(emergencyId)).thenReturn(Optional.empty());
        when(hospitalizationRepository.findActiveByPatientIds(Set.of(request.patientId())))
                .thenReturn(Optional.empty());
        when(bedRepository.claimIfAvailable(
                request.bedId(),
                BedStatus.FREE,
                BedStatus.OCCUPIED,
                BedCapacityStatus.OPEN,
                BedReadinessStatus.READY)).thenReturn(1);
        when(bedRepository.findById(request.bedId())).thenReturn(Optional.of(bed));
        when(hospitalizationRepository.getNextHospitalizationNumberSequenceValue()).thenReturn(43L);
        when(hospitalizationRepository.save(any(HospitalizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.admitPatient(emergencyRequest);

        assertEquals(emergencyId, response.emergencyId());
        verify(emergency).setVisitId(any(UUID.class));
        verify(emergencyRepository).save(emergency);
        ArgumentCaptor<HospitalizationEntity> captor = ArgumentCaptor.forClass(HospitalizationEntity.class);
        verify(hospitalizationRepository).save(captor.capture());
        assertEquals(emergencyId, captor.getValue().getEmergencyId());
        assertEquals(request.serviceUnitId(), captor.getValue().getCurrentServiceUnitId());
        assertEquals(request.spaceId(), captor.getValue().getCurrentSpaceId());
    }

    private VisitEntity prepareVisitBasedAdmission() {
        prepareCanonicalPatient();
        VisitEntity visit = org.mockito.Mockito.mock(VisitEntity.class);
        when(visit.getPatient()).thenReturn(patient);
        when(visitRepository.findById(request.visitId())).thenReturn(Optional.of(visit));
        when(hospitalizationRepository.findActiveByPatientIds(Set.of(request.patientId())))
                .thenReturn(Optional.empty());
        when(bedAssignmentRepository.findActiveByBedId(request.bedId())).thenReturn(Optional.empty());
        when(hospitalizationRepository.findActiveByBedId(request.bedId())).thenReturn(Optional.empty());
        return visit;
    }

    private void prepareCanonicalPatient() {
        when(patient.getId()).thenReturn(request.patientId());
        when(patient.getOrganizationId()).thenReturn(organizationId);
        when(canonicalResolver.resolve(request.patientId())).thenReturn(context(patient));
    }

    private void preparePlacementWithoutBed() {
        prepareUnitAndSpace();
        when(unitSpaceAssignmentRepository.existsActiveAt(
                eq(organizationId), eq(request.serviceUnitId()), eq(request.spaceId()), any(Instant.class)))
                .thenReturn(true);
    }

    private void preparePlacementWithBed(BedEntity bed) {
        preparePlacementWithoutBed();
        when(bedRepository.findByIdAndOrganizationId(request.bedId(), organizationId))
                .thenReturn(Optional.of(bed));
    }

    private void prepareUnitAndSpace() {
        when(unitRepository.findByIdAndOrganizationId(request.serviceUnitId(), organizationId))
                .thenReturn(Optional.of(unit));
        when(unit.getId()).thenReturn(request.serviceUnitId());
        when(unit.getUnitType()).thenReturn(OrganizationalUnitType.CARE_UNIT);
        when(unit.isActive()).thenReturn(true);
        when(unit.getName()).thenReturn("Médecine");
        when(spaceRepository.findByIdAndOrganizationId(request.spaceId(), organizationId))
                .thenReturn(Optional.of(space));
        when(space.getId()).thenReturn(request.spaceId());
        when(space.isActive()).thenReturn(true);
        when(space.getName()).thenReturn("Chambre 101");
        when(inpatientProfileRepository.existsBySpaceIdAndOrganizationId(request.spaceId(), organizationId))
                .thenReturn(true);
    }

    private BedEntity configuredBed(UUID bedId) {
        BedEntity bed = org.mockito.Mockito.mock(BedEntity.class);
        when(bed.getId()).thenReturn(bedId);
        when(bed.getSpace()).thenReturn(space);
        when(bed.getBedNumber()).thenReturn("101-A");
        return bed;
    }

    private PatientCanonicalResolver.CanonicalPatientContext context(PatientEntity resolvedPatient) {
        return new PatientCanonicalResolver.CanonicalPatientContext(
                resolvedPatient,
                resolvedPatient,
                Set.of(request.patientId()),
                false);
    }

    private CreateHospitalizationRequest request() {
        return new CreateHospitalizationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Surveillance clinique",
                UUID.randomUUID(),
                null,
                UUID.randomUUID());
    }
}
