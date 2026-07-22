package com.joprelys.backend.spatial.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateAxis;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeSource;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStateReasonCode;
import com.joprelys.backend.spatial.infrastructure.persistence.RoomEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.WardEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BedStateChangeServiceTest {

    @Mock
    private BedStateChangeRepository bedStateChangeRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    private BedStateChangeService service;
    private BedEntity bed;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        service = new BedStateChangeService(
                bedStateChangeRepository,
                bedRepository,
                userAccountRepository);

        WardEntity ward = new WardEntity("Médecine", HospitalServiceType.HOSPITALIZATION);
        RoomEntity room = new RoomEntity(ward, "101", 1, "STANDARD");
        bed = new BedEntity(room, "101-A");
        bed.setOrganizationId(UUID.randomUUID());
    }

    @Test
    void shouldRecordManualCapacityClosureWithSystemFallbackActor() {
        service.recordManual(
                bed,
                BedStateAxis.CAPACITY,
                "OPEN",
                "CLOSED",
                BedStateReasonCode.CAPACITY_STAFFING_SHORTAGE,
                "Équipe de nuit incomplète");

        ArgumentCaptor<BedStateChangeEntity> captor = ArgumentCaptor.forClass(BedStateChangeEntity.class);
        verify(bedStateChangeRepository).save(captor.capture());
        BedStateChangeEntity event = captor.getValue();
        assertEquals(BedStateReasonCode.CAPACITY_STAFFING_SHORTAGE, event.getReasonCode());
        assertEquals("Système Joprelys", event.getActorDisplayName());
        assertEquals(BedStateChangeSource.MANUAL, event.getSource());
    }

    @Test
    void shouldRejectOtherCapacityReasonWithoutNote() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.recordManual(
                        bed,
                        BedStateAxis.CAPACITY,
                        "OPEN",
                        "CLOSED",
                        BedStateReasonCode.CAPACITY_OTHER,
                        null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(bedStateChangeRepository, never()).save(any(BedStateChangeEntity.class));
    }

    @Test
    void shouldRejectAutomaticDepartureReasonFromManualCommand() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.recordManual(
                        bed,
                        BedStateAxis.READINESS,
                        "READY",
                        "CLEANING",
                        BedStateReasonCode.CLEANING_AFTER_DEPARTURE,
                        "Tentative manuelle"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(bedStateChangeRepository, never()).save(any(BedStateChangeEntity.class));
    }

    @Test
    void shouldAllowAutomaticCleaningAfterTransfer() {
        service.recordSystem(
                bed,
                BedStateAxis.READINESS,
                "READY",
                "CLEANING",
                BedStateReasonCode.CLEANING_AFTER_TRANSFER,
                "Transfert vers un autre lit",
                BedStateChangeSource.SYSTEM_TRANSFER);

        verify(bedStateChangeRepository).save(any(BedStateChangeEntity.class));
    }

    @Test
    void shouldReturnHistoryInRepositoryOrder() {
        when(bedRepository.findById(bed.getId())).thenReturn(Optional.of(bed));
        BedStateChangeEntity first = new BedStateChangeEntity(
                bed,
                BedStateAxis.CAPACITY,
                "OPEN",
                "CLOSED",
                BedStateReasonCode.CAPACITY_SAFETY,
                "Contrôle électrique",
                null,
                "Système Joprelys",
                BedStateChangeSource.MANUAL,
                java.time.Instant.parse("2026-07-22T06:00:00Z"));
        when(bedStateChangeRepository.findByBedIdOrderByOccurredAtDesc(bed.getId()))
                .thenReturn(List.of(first));

        var history = service.listHistory(bed.getId());

        assertEquals(1, history.size());
        assertEquals("CAPACITY_SAFETY", history.getFirst().reasonCode());
    }
}
