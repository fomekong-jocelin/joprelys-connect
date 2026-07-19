package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.api.CreateHospitalizationRequest;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalizationAdmissionServiceTest {

    @Mock
    private HospitalizationRepository hospitalizationRepository;

    @Mock
    private PatientService patientService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private BedAssignmentRepository bedAssignmentRepository;

    @InjectMocks
    private HospitalizationAdmissionService service;

    @Test
    void shouldRejectAdmissionWhenBedIsNotConfigured() {
        CreateHospitalizationRequest request = request();
        when(patientService.getPatientById(request.patientId())).thenReturn(org.mockito.Mockito.mock(PatientEntity.class));
        when(hospitalizationRepository.findActiveByPatientId(request.patientId())).thenReturn(Optional.empty());
        when(hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber()))
                .thenReturn(Optional.empty());
        when(bedRepository.findByWardRoomAndBedNumber(
                request.serviceName(), request.roomNumber(), request.bedNumber()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.admitPatient(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(bedRepository, never()).saveAndFlush(any());
        verify(hospitalizationRepository, never()).save(any());
        verify(bedAssignmentRepository, never()).save(any());
    }

    @Test
    void shouldOccupyAnExistingFreeBedWithoutCreatingSpatialData() {
        CreateHospitalizationRequest request = request();
        BedEntity bed = org.mockito.Mockito.mock(BedEntity.class);
        RoomEntity room = org.mockito.Mockito.mock(RoomEntity.class);
        WardEntity ward = org.mockito.Mockito.mock(WardEntity.class);

        when(patientService.getPatientById(request.patientId())).thenReturn(org.mockito.Mockito.mock(PatientEntity.class));
        when(hospitalizationRepository.findActiveByPatientId(request.patientId())).thenReturn(Optional.empty());
        when(hospitalizationRepository.findActiveByBed(request.roomNumber(), request.bedNumber()))
                .thenReturn(Optional.empty());
        when(bedRepository.findByWardRoomAndBedNumber(
                request.serviceName(), request.roomNumber(), request.bedNumber()))
                .thenReturn(Optional.of(bed));
        when(bed.getRoom()).thenReturn(room);
        when(room.getWard()).thenReturn(ward);
        when(bed.getStatus()).thenReturn(BedStatus.FREE);
        when(hospitalizationRepository.getNextHospitalizationNumberSequenceValue()).thenReturn(42L);
        when(hospitalizationRepository.save(any(HospitalizationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.admitPatient(request);

        assertNotNull(response);
        verify(ward).requireRoomsAllowed();
        verify(bed).setStatus(BedStatus.OCCUPIED);
        verify(bedRepository).saveAndFlush(bed);
        verify(hospitalizationRepository).save(any(HospitalizationEntity.class));
        verify(bedAssignmentRepository).save(any());
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
