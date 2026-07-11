package com.joprelys.backend.emergency.medicolegal.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyRepository;
import com.joprelys.backend.emergency.medicolegal.api.CreateEmergencyBelongingRequest;
import com.joprelys.backend.emergency.medicolegal.api.CreateEmergencyLegalBasisRequest;
import com.joprelys.backend.emergency.medicolegal.api.CreateEmergencyThirdPartyRequest;
import com.joprelys.backend.emergency.medicolegal.api.EmergencyMedicoLegalResponse;
import com.joprelys.backend.emergency.medicolegal.api.RecordEmergencyCapacityRequest;
import com.joprelys.backend.emergency.medicolegal.api.TransferEmergencyBelongingRequest;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingStatus;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingTransferAction;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyCapacityStatus;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyLegalBasisType;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyThirdPartyQuality;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyBelongingEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyBelongingRepository;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyBelongingTransferEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyBelongingTransferRepository;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyCapacityEventEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyCapacityEventRepository;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyIdentityStatementEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyIdentityStatementRepository;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyLegalBasisEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyLegalBasisRepository;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyThirdPartyEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyThirdPartyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmergencyMedicoLegalService {

    private final EmergencyRepository emergencyRepository;
    private final EmergencyThirdPartyRepository thirdPartyRepository;
    private final EmergencyIdentityStatementRepository identityStatementRepository;
    private final EmergencyCapacityEventRepository capacityEventRepository;
    private final EmergencyLegalBasisRepository legalBasisRepository;
    private final EmergencyBelongingRepository belongingRepository;
    private final EmergencyBelongingTransferRepository transferRepository;
    private final EmergencyMedicoLegalMapper mapper;
    private final AuditService auditService;

    public EmergencyMedicoLegalService(
            EmergencyRepository emergencyRepository,
            EmergencyThirdPartyRepository thirdPartyRepository,
            EmergencyIdentityStatementRepository identityStatementRepository,
            EmergencyCapacityEventRepository capacityEventRepository,
            EmergencyLegalBasisRepository legalBasisRepository,
            EmergencyBelongingRepository belongingRepository,
            EmergencyBelongingTransferRepository transferRepository,
            EmergencyMedicoLegalMapper mapper,
            AuditService auditService) {
        this.emergencyRepository = emergencyRepository;
        this.thirdPartyRepository = thirdPartyRepository;
        this.identityStatementRepository = identityStatementRepository;
        this.capacityEventRepository = capacityEventRepository;
        this.legalBasisRepository = legalBasisRepository;
        this.belongingRepository = belongingRepository;
        this.transferRepository = transferRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public EmergencyMedicoLegalResponse getDossier(UUID emergencyId) {
        requireEmergency(emergencyId);
        return loadDossier(emergencyId);
    }

    @Transactional
    public EmergencyMedicoLegalResponse addThirdParty(
            UUID emergencyId,
            CreateEmergencyThirdPartyRequest request,
            UUID actorUserId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        UUID organizationId = requireOrganizationId();
        validateRepresentativeClaim(request);

        EmergencyThirdPartyEntity thirdParty = thirdPartyRepository.save(new EmergencyThirdPartyEntity(
                organizationId,
                emergency,
                request.fullName(),
                request.phone(),
                request.email(),
                request.idDocument(),
                request.relationshipToPatient(),
                request.circumstances(),
                request.consentToContact(),
                request.legalRepresentativeClaimed(),
                request.sourceType(),
                request.confidenceLevel(),
                request.proofReference(),
                request.qualities(),
                actorUserId));

        List<EmergencyIdentityStatementEntity> statements = safeList(request.identityStatements()).stream()
                .map(item -> new EmergencyIdentityStatementEntity(
                        organizationId,
                        emergency,
                        thirdParty,
                        item.fieldName(),
                        item.value(),
                        item.confidenceLevel(),
                        item.proofReference(),
                        item.declaredAt(),
                        actorUserId))
                .toList();
        identityStatementRepository.saveAll(statements);

        audit(actorUserId, emergency, thirdParty.getId(), "ADD_EMERGENCY_THIRD_PARTY",
                "Added structured third party with qualities " + request.qualities());
        return loadDossier(emergencyId);
    }

    @Transactional
    public EmergencyMedicoLegalResponse recordCapacity(
            UUID emergencyId,
            RecordEmergencyCapacityRequest request,
            UUID actorUserId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        UUID organizationId = requireOrganizationId();
        Instant effectiveAt = request.effectiveAt() == null ? Instant.now() : request.effectiveAt();

        EmergencyCapacityEventEntity event = capacityEventRepository.save(new EmergencyCapacityEventEntity(
                organizationId,
                emergency,
                request.status(),
                request.consciousnessLevel(),
                request.clinicalReason(),
                effectiveAt,
                actorUserId));

        if (request.status() == EmergencyCapacityStatus.CAPABLE) {
            closeActiveLegalBases(emergencyId, effectiveAt, "CAPACITY_RESTORED");
        }

        audit(actorUserId, emergency, event.getId(), "RECORD_EMERGENCY_CAPACITY",
                "Recorded capacity status " + request.status());
        return loadDossier(emergencyId);
    }

    @Transactional
    public EmergencyMedicoLegalResponse addLegalBasis(
            UUID emergencyId,
            CreateEmergencyLegalBasisRequest request,
            UUID actorUserId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        UUID organizationId = requireOrganizationId();
        validateCapacityForEmergencyBasis(emergencyId, request.basisType());

        Instant startsAt = request.startsAt() == null ? Instant.now() : request.startsAt();
        closeActiveLegalBases(emergencyId, startsAt, "SUPERSEDED_BY_NEW_LEGAL_BASIS");

        EmergencyLegalBasisEntity legalBasis = legalBasisRepository.save(new EmergencyLegalBasisEntity(
                organizationId,
                emergency,
                request.basisType(),
                request.justification(),
                startsAt,
                request.expiresAt(),
                request.coveredActs(),
                actorUserId));

        audit(actorUserId, emergency, legalBasis.getId(), "ADD_EMERGENCY_LEGAL_BASIS",
                "Added legal basis " + request.basisType());
        return loadDossier(emergencyId);
    }

    @Transactional
    public EmergencyMedicoLegalResponse addBelonging(
            UUID emergencyId,
            CreateEmergencyBelongingRequest request,
            UUID actorUserId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        UUID organizationId = requireOrganizationId();

        EmergencyBelongingEntity belonging = belongingRepository.save(new EmergencyBelongingEntity(
                organizationId,
                emergency,
                request.category(),
                request.description(),
                request.quantity(),
                request.itemCondition(),
                request.sealNumber(),
                request.depositedByName(),
                actorUserId));

        EmergencyBelongingTransferEntity deposit = transferRepository.save(new EmergencyBelongingTransferEntity(
                organizationId,
                belonging,
                EmergencyBelongingTransferAction.DEPOSITED,
                request.depositedByName(),
                null,
                null,
                "INITIAL_INVENTORY",
                Instant.now(),
                actorUserId));

        audit(actorUserId, emergency, belonging.getId(), "ADD_EMERGENCY_BELONGING",
                "Inventoried belonging and created custody event " + deposit.getId());
        return loadDossier(emergencyId);
    }

    @Transactional
    public EmergencyMedicoLegalResponse transferBelonging(
            UUID emergencyId,
            UUID belongingId,
            TransferEmergencyBelongingRequest request,
            UUID actorUserId) {
        EmergencyEntity emergency = requireEmergency(emergencyId);
        EmergencyBelongingEntity belonging = belongingRepository.findByIdForUpdate(belongingId)
                .orElseThrow(() -> notFound("EMERGENCY_BELONGING_NOT_FOUND"));
        if (!belonging.getEmergency().getId().equals(emergencyId)) {
            throw notFound("EMERGENCY_BELONGING_NOT_FOUND");
        }

        EmergencyBelongingStatus targetStatus = targetStatus(request.action());
        try {
            belonging.applyTransferStatus(targetStatus);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }

        EmergencyBelongingTransferEntity transfer = transferRepository.save(new EmergencyBelongingTransferEntity(
                requireOrganizationId(),
                belonging,
                request.action(),
                request.fromCustodian(),
                request.recipientName(),
                request.recipientIdDocument(),
                request.notes(),
                request.occurredAt(),
                actorUserId));
        belongingRepository.save(belonging);

        audit(actorUserId, emergency, belonging.getId(), "TRANSFER_EMERGENCY_BELONGING",
                "Recorded custody action " + transfer.getActionType());
        return loadDossier(emergencyId);
    }

    private EmergencyMedicoLegalResponse loadDossier(UUID emergencyId) {
        return mapper.toResponse(
                emergencyId,
                thirdPartyRepository.findByEmergencyIdOrderByCreatedAtAsc(emergencyId),
                identityStatementRepository.findByEmergencyIdOrderByDeclaredAtAsc(emergencyId),
                capacityEventRepository.findByEmergencyIdOrderByEffectiveAtAsc(emergencyId),
                legalBasisRepository.findByEmergencyIdOrderByStartsAtAsc(emergencyId),
                belongingRepository.findByEmergencyIdOrderByCreatedAtAsc(emergencyId),
                transferRepository.findByBelongingEmergencyIdOrderByOccurredAtAsc(emergencyId));
    }

    private EmergencyEntity requireEmergency(UUID emergencyId) {
        return emergencyRepository.findByIdWithPatientAndLogs(emergencyId)
                .orElseThrow(() -> notFound("EMERGENCY_NOT_FOUND"));
    }

    private UUID requireOrganizationId() {
        UUID organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }
        return organizationId;
    }

    private void validateRepresentativeClaim(CreateEmergencyThirdPartyRequest request) {
        if (request.legalRepresentativeClaimed()
                && !request.qualities().contains(EmergencyThirdPartyQuality.PRESUMED_REPRESENTATIVE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EMERGENCY_LEGAL_REPRESENTATIVE_QUALITY_REQUIRED");
        }
    }

    private void validateCapacityForEmergencyBasis(UUID emergencyId, EmergencyLegalBasisType basisType) {
        if (basisType != EmergencyLegalBasisType.VITAL_EMERGENCY
                && basisType != EmergencyLegalBasisType.PRESUMED_CONSENT) {
            return;
        }
        EmergencyCapacityStatus currentStatus = capacityEventRepository
                .findFirstByEmergencyIdOrderByEffectiveAtDesc(emergencyId)
                .map(EmergencyCapacityEventEntity::getCapacityStatus)
                .orElse(null);
        if (currentStatus != EmergencyCapacityStatus.INCAPABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "EMERGENCY_INCAPACITY_REQUIRED_FOR_LEGAL_BASIS");
        }
    }

    private void closeActiveLegalBases(UUID emergencyId, Instant closedAt, String reason) {
        legalBasisRepository.findByEmergencyIdOrderByStartsAtAsc(emergencyId).stream()
                .filter(item -> item.isActiveAt(closedAt))
                .forEach(item -> item.close(closedAt, reason));
    }

    private EmergencyBelongingStatus targetStatus(EmergencyBelongingTransferAction action) {
        return switch (action) {
            case DEPOSITED, SEALED -> EmergencyBelongingStatus.IN_CUSTODY;
            case TRANSFERRED -> EmergencyBelongingStatus.TRANSFERRED;
            case RELEASED, RETURNED -> EmergencyBelongingStatus.RELEASED;
            case DISPOSED -> EmergencyBelongingStatus.DISPOSED;
        };
    }

    private void audit(
            UUID actorUserId,
            EmergencyEntity emergency,
            UUID resourceId,
            String action,
            String reason) {
        auditService.logSuccess(
                actorUserId,
                requireOrganizationId(),
                emergency.getPatient().getId(),
                "EMERGENCY_MEDICO_LEGAL",
                resourceId,
                action,
                reason);
    }

    private ResponseStatusException notFound(String code) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, code);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
