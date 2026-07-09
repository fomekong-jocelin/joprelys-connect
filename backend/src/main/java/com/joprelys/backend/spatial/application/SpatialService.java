package com.joprelys.backend.spatial.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationEntity;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.spatial.api.*;
import com.joprelys.backend.spatial.infrastructure.persistence.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SpatialService {

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final HospitalizationRepository hospitalizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public SpatialService(WardRepository wardRepository,
                          RoomRepository roomRepository,
                          BedRepository bedRepository,
                          BedAssignmentRepository bedAssignmentRepository,
                          HospitalizationRepository hospitalizationRepository,
                          UserAccountRepository userAccountRepository,
                          AuditService auditService) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.bedAssignmentRepository = bedAssignmentRepository;
        this.hospitalizationRepository = hospitalizationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<WardResponse> listWards() {
        return wardRepository.findAll().stream()
                .map(WardResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WardOccupancyResponse getWardOccupancy(UUID wardId) {
        WardEntity ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));

        List<RoomEntity> rooms = roomRepository.findByWardId(wardId);
        List<RoomOccupancyResponse> roomOccupancyResponses = new ArrayList<>();
        int totalBeds = 0;
        int occupiedBeds = 0;

        for (RoomEntity room : rooms) {
            List<BedEntity> beds = bedRepository.findByRoomId(room.getId());
            List<BedResponse> bedResponses = beds.stream()
                    .map(BedResponse::fromEntity)
                    .toList();

            for (BedEntity bed : beds) {
                totalBeds++;
                if (bed.getStatus() == BedStatus.OCCUPIED) {
                    occupiedBeds++;
                }
            }

            roomOccupancyResponses.add(new RoomOccupancyResponse(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getCapacity(),
                    room.getComfortLevel(),
                    bedResponses
            ));
        }

        return new WardOccupancyResponse(
                ward.getId(),
                ward.getName(),
                roomOccupancyResponses,
                totalBeds,
                occupiedBeds
        );
    }

    @Transactional
    public BedResponse updateBedStatus(UUID bedId, BedStatus newStatus) {
        BedEntity bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lit introuvable"));

        // Si le lit passe à FREE, s'assurer de clore d'éventuels assignments actifs
        if (newStatus == BedStatus.FREE) {
            bedAssignmentRepository.findActiveByBedId(bedId).ifPresent(assignment -> {
                assignment.setReleasedAt(Instant.now());
                bedAssignmentRepository.save(assignment);
            });
        }

        bed.setStatus(newStatus);
        BedEntity saved = bedRepository.save(bed);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    null,
                    "SPATIAL",
                    saved.getId(),
                    "UPDATE_BED_STATUS",
                    "Statut du lit " + saved.getBedNumber() + " changé à " + newStatus
            );
        }

        return BedResponse.fromEntity(saved);
    }

    @Transactional
    public BedAssignmentResponse transferPatient(UUID hospitalizationId, UUID newBedId) {
        HospitalizationEntity hospitalization = hospitalizationRepository.findById(hospitalizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospitalisation introuvable"));

        if (!"EN_COURS".equals(hospitalization.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'hospitalisation n'est pas active.");
        }

        BedEntity newBed = bedRepository.findById(newBedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nouveau lit introuvable"));

        if (newBed.getStatus() != BedStatus.FREE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le lit demandé n'est pas libre.");
        }

        // 1. Libérer l'ancien lit et clore l'assignment actif
        bedAssignmentRepository.findActiveByHospitalizationId(hospitalizationId).ifPresent(oldAssignment -> {
            oldAssignment.setReleasedAt(Instant.now());
            bedAssignmentRepository.save(oldAssignment);

            BedEntity oldBed = oldAssignment.getBed();
            oldBed.setStatus(BedStatus.CLEANING); // Passe par défaut en nettoyage
            bedRepository.save(oldBed);
        });

        // 2. Assigner le nouveau lit
        newBed.setStatus(BedStatus.OCCUPIED);
        BedEntity occupiedBed = bedRepository.save(newBed);

        // 3. Mettre à jour l'hospitalisation avec les coordonnées physiques du nouveau lit
        hospitalization.setServiceName(occupiedBed.getRoom().getWard().getName());
        hospitalization.setRoomNumber(occupiedBed.getRoom().getRoomNumber());
        hospitalization.setBedNumber(occupiedBed.getBedNumber());
        hospitalizationRepository.save(hospitalization);

        // 4. Créer l'assignation
        BedAssignmentEntity assignment = new BedAssignmentEntity(hospitalizationId, occupiedBed);
        BedAssignmentEntity savedAssignment = bedAssignmentRepository.save(assignment);

        UserAccountEntity actor = getCurrentUser();
        if (actor != null) {
            auditService.logSuccess(
                    actor.getId(),
                    actor.getOrganizationId(),
                    hospitalization.getPatientId(),
                    "HOSPITALIZATION",
                    hospitalizationId,
                    "TRANSFER",
                    "Patient transféré vers Lit : " + occupiedBed.getBedNumber() + " | Chambre : " + occupiedBed.getRoom().getRoomNumber() + " | Service : " + occupiedBed.getRoom().getWard().getName()
            );
        }

        return BedAssignmentResponse.fromEntity(savedAssignment);
    }

    private UserAccountEntity getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }
}
