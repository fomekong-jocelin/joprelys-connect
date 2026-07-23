package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.FacilitySpaceRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.InpatientSpaceProfileRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.OrganizationalUnitSpaceAssignmentRepository;
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

    @Mock private BedRepository bedRepository;
    @Mock private BedAssignmentRepository bedAssignmentRepository;
    @Mock private ActiveBedAssignmentService activeBedAssignmentService;
    @Mock private BedStateChangeService bedStateChangeService;
    @Mock private HospitalizationRepository hospitalizationRepository;
    @Mock private FacilitySpaceRepository spaceRepository;
    @Mock private InpatientSpaceProfileRepository inpatientSpaceProfileRepository;
    @Mock private OrganizationalUnitSpaceAssignmentRepository unitSpaceAssignmentRepository;
    @Mock private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock private HospitalServiceCatalogRepository serviceCatalogRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditService auditService;

    @Test
    void shouldRejectTransferAfterMedicalDischargeDecisionBeforeClaimingTargetBed() {
        UUID hospitalizationId = UUID.randomUUID();
        UUID targetUnitId = UUID.randomUUID();
        UUID targetSpaceId = UUID.randomUUID();
        UUID targetBedId = UUID.randomUUID();
        HospitalizationEntity hospitalization = new HospitalizationEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Médecine",
                "Chambre 101",
                "A",
                "Surveillance",
                "HOSP-TEST-001",
                UUID.randomUUID(),
                null,
                UUID.randomUUID());
        hospitalization.decideDischarge(
                "État stabilisé",
                "Suivi ambulatoire",
                false,
                UUID.randomUUID(),
                Instant.now());
        when(hospitalizationRepository.findById(hospitalizationId)).thenReturn(Optional.of(hospitalization));

        SpatialService service = new SpatialService(
                bedRepository,
                bedAssignmentRepository,
                activeBedAssignmentService,
                new BedStatusTransitionPolicy(),
                bedStateChangeService,
                hospitalizationRepository,
                spaceRepository,
                inpatientSpaceProfileRepository,
                unitSpaceAssignmentRepository,
                organizationalUnitRepository,
                serviceCatalogRepository,
                userAccountRepository,
                auditService);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.transferPatient(hospitalizationId, targetUnitId, targetSpaceId, targetBedId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bedRepository, never()).findByIdAndOrganizationId(any(), any());
    }
}
