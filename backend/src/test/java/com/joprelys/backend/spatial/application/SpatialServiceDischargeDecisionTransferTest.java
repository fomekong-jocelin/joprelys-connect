package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.WardRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SpatialServiceDischargeDecisionTransferTest {

    @Mock private WardRepository wardRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private BedAssignmentRepository bedAssignmentRepository;
    @Mock private ActiveBedAssignmentService activeBedAssignmentService;
    @Mock private BedStateChangeService bedStateChangeService;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditService auditService;

    @Test
    void shouldRejectTransferAfterMedicalDischargeDecisionBeforeClaimingTargetBed() {
        UUID hospitalizationId = UUID.randomUUID();
        UUID targetBedId = UUID.randomUUID();
        HospitalizationEntity hospitalization = new HospitalizationEntity(
                UUID.randomUUID(),
                "Médecine",
                "101",
                "A",
                "Surveillance");
        hospitalization.decideDischarge(
                "État stabilisé",
                "Suivi ambulatoire",
                false,
                UUID.randomUUID(),
                Instant.now());
        when(hospitalizationRepository.findById(hospitalizationId)).thenReturn(Optional.of(hospitalization));

        SpatialService service = new SpatialService(
                wardRepository,
                roomRepository,
                bedRepository,
                bedAssignmentRepository,
                activeBedAssignmentService,
                new BedStatusTransitionPolicy(),
                bedStateChangeService,
                hospitalizationRepository,
                userAccountRepository,
                auditService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.transferPatient(hospitalizationId, targetBedId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bedRepository, never()).findById(targetBedId);
    }
}
