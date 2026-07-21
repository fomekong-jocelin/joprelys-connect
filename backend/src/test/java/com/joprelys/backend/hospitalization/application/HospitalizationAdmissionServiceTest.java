package com.joprelys.backend.hospitalization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.spatial.application.ActiveBedAssignmentService;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
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

    @Mock
    private HospitalizationRepository hospitalizationRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private ActiveBedAssignmentService activeBedAssignmentService;
    @Mock
    private EmergencyRepository emergencyRepository;
    @Mock
    private PatientCanonicalResolver canonicalResolver;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private VisitNumberGenerator visitNumberGenerator;

    @InjectMocks
    private HospitalizationAdmissionService service;

    private CreateHospitalizationRequest request;
    private PatientEntity patient;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        request = request();
        organizationId = UUID.randomUUID();
        patient = org.mockito.Mockito.mock(PatientEntity.class);
    }

    @Test
    void shouldRejectAdmissionWhenBedIsNotConfiguredForPatientOrganization() {
        prepareVisitBasedAdmission();
        when(bedRepository.findConfiguredBed(
                organizationId,
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bedRepository, never()).claimIfFree(any(), any(), any());
        verify(hospitalizationRepository, never()).save(any());
        verify(activeBedAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    void shouldAtomicallyClaimConfiguredBedWithoutCreatingSpatialData() {
        VisitEntity visit = prepareVisitBasedAdmission();
        when(visit.getId()).thenReturn(request.visitId());
        UUID bedId = UUID.randomUUID();
        BedEntity bed = configuredBed(bedId);

        when(bedRepository.findConfiguredBed(
                organizationId,
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber()))
                .thenReturn(Optional.of(bed));
        when(bedRepository.claimIfFree(bedId, BedStatus.FREE, BedStatus.OCCUPIED)).thenReturn(1);
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(hospitalizationRepository.getNextHospitalizationNumberSequenceValue()).thenReturn(42L);
        when(hospitalizationRepository.save(any(HospitalizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.admitPatient(request);

        assertNotNull(response);
        verify(bedRepository).claimIfFree(bedId, BedStatus.FREE, BedStatus.OCCUPIED);
        verify(hospitalizationRepository).save(any(HospitalizationEntity.class));
        verify(activeBedAssignmentService).assign(any(), any(), any());
    }

    @Test
    void shouldRejectAdmissionWhenAtomicBedClaimLosesTheRace() {
        prepareVisitBasedAdmission();
        UUID bedId = UUID.randomUUID();
        BedEntity bed = configuredBed(bedId);

        when(bedRepository.findConfiguredBed(
                organizationId,
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber()))
                .thenReturn(Optional.of(bed));
        when(bedRepository.claimIfFree(bedId, BedStatus.FREE, BedStatus.OCCUPIED)).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(hospitalizationRepository, never()).save(any());
        verify(activeBedAssignmentService, never()).assign(any(), any(), any());
    }

    @Test
    void shouldCreateAVisitAndRetainEmergencyLinkWhenNoVisitWasProvided() {
        prepareCanonicalPatient();
        UUID emergencyId = UUID.randomUUID();
        CreateHospitalizationRequest emergencyRequest = new CreateHospitalizationRequest(
                request.patientId(),
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber(),
                request.admissionReason(),
                null,
                emergencyId,
                request.responsiblePractitionerId());
        EmergencyEntity emergency = org.mockito.Mockito.mock(EmergencyEntity.class);
        UUID bedId = UUID.randomUUID();
        BedEntity bed = configuredBed(bedId);

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
        when(hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber()))
                .thenReturn(Optional.empty());
        when(bedRepository.findConfiguredBed(
                organizationId,
                request.serviceName(),
                request.roomNumber(),
                request.bedNumber()))
                .thenReturn(Optional.of(bed));
        when(bedRepository.claimIfFree(bedId, BedStatus.FREE, BedStatus.OCCUPIED)).thenReturn(1);
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
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
    }

    private VisitEntity prepareVisitBasedAdmission() {
        prepareCanonicalPatient();
        VisitEntity visit = org.mockito.Mockito.mock(VisitEntity.class);
        when(visit.getPatient()).thenReturn(patient);
        when(visitRepository.findById(request.visitId())).thenReturn(Optional.of(visit));
        when(hospitalizationRepository.findActiveByPatientIds(Set.of(request.patientId())))
                .thenReturn(Optional.empty());
        when(hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber()))
                .thenReturn(Optional.empty());
        return visit;
    }

    private void prepareCanonicalPatient() {
        when(patient.getId()).thenReturn(request.patientId());
        when(patient.getOrganizationId()).thenReturn(organizationId);
        when(canonicalResolver.resolve(request.patientId())).thenReturn(context(patient));
    }

    private BedEntity configuredBed(UUID bedId) {
        BedEntity bed = org.mockito.Mockito.mock(BedEntity.class);
        RoomEntity room = org.mockito.Mockito.mock(RoomEntity.class);
        WardEntity ward = org.mockito.Mockito.mock(WardEntity.class);
        when(bed.getRoom()).thenReturn(room);
        when(room.getWard()).thenReturn(ward);
        when(bed.getId()).thenReturn(bedId);
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
                "Médecine",
                "101",
                "101-A",
                "Surveillance clinique",
                UUID.randomUUID(),
                UUID.randomUUID());
    }
}
