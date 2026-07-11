package com.joprelys.backend.emergency.medicolegal.application;

import com.joprelys.backend.emergency.medicolegal.api.EmergencyMedicoLegalResponse;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyBelongingEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyBelongingTransferEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyCapacityEventEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyIdentityStatementEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyLegalBasisEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyThirdPartyEntity;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EmergencyMedicoLegalMapper {

    public EmergencyMedicoLegalResponse toResponse(
            UUID emergencyId,
            List<EmergencyThirdPartyEntity> thirdParties,
            List<EmergencyIdentityStatementEntity> statements,
            List<EmergencyCapacityEventEntity> capacityEvents,
            List<EmergencyLegalBasisEntity> legalBases,
            List<EmergencyBelongingEntity> belongings,
            List<EmergencyBelongingTransferEntity> transfers) {
        Instant now = Instant.now();
        Map<UUID, List<EmergencyBelongingTransferEntity>> transfersByBelonging = transfers.stream()
                .collect(Collectors.groupingBy(item -> item.getBelonging().getId()));

        List<EmergencyMedicoLegalResponse.CapacityEvent> capacityHistory = capacityEvents.stream()
                .map(this::toCapacityEvent)
                .toList();
        EmergencyMedicoLegalResponse.CapacityEvent currentCapacity = capacityEvents.stream()
                .max(Comparator.comparing(EmergencyCapacityEventEntity::getEffectiveAt))
                .map(this::toCapacityEvent)
                .orElse(null);

        List<EmergencyMedicoLegalResponse.LegalBasis> legalBasisResponses = legalBases.stream()
                .map(item -> toLegalBasis(item, now))
                .toList();
        EmergencyMedicoLegalResponse.LegalBasis activeLegalBasis = legalBases.stream()
                .filter(item -> item.isActiveAt(now))
                .max(Comparator.comparing(EmergencyLegalBasisEntity::getStartsAt))
                .map(item -> toLegalBasis(item, now))
                .orElse(null);

        return new EmergencyMedicoLegalResponse(
                emergencyId,
                thirdParties.stream().map(this::toThirdParty).toList(),
                statements.stream().map(this::toIdentityStatement).toList(),
                capacityHistory,
                currentCapacity,
                legalBasisResponses,
                activeLegalBasis,
                belongings.stream()
                        .map(item -> toBelonging(item, transfersByBelonging.getOrDefault(item.getId(), List.of())))
                        .toList());
    }

    public EmergencyMedicoLegalResponse.ThirdParty toThirdParty(EmergencyThirdPartyEntity entity) {
        return new EmergencyMedicoLegalResponse.ThirdParty(
                entity.getId(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getIdDocument(),
                entity.getRelationshipToPatient(),
                entity.getCircumstances(),
                entity.isConsentToContact(),
                entity.isLegalRepresentativeClaimed(),
                entity.getSourceType(),
                entity.getConfidenceLevel(),
                entity.getProofReference(),
                entity.getQualities(),
                entity.getCreatedByUserId(),
                entity.getCreatedAt());
    }

    public EmergencyMedicoLegalResponse.CapacityEvent toCapacityEvent(EmergencyCapacityEventEntity entity) {
        return new EmergencyMedicoLegalResponse.CapacityEvent(
                entity.getId(),
                entity.getCapacityStatus(),
                entity.getConsciousnessLevel(),
                entity.getClinicalReason(),
                entity.getEffectiveAt(),
                entity.getRecordedByUserId());
    }

    public EmergencyMedicoLegalResponse.LegalBasis toLegalBasis(
            EmergencyLegalBasisEntity entity,
            Instant reference) {
        return new EmergencyMedicoLegalResponse.LegalBasis(
                entity.getId(),
                entity.getBasisType(),
                entity.getJustification(),
                entity.getStartsAt(),
                entity.getExpiresAt(),
                entity.getClosedAt(),
                entity.getClosureReason(),
                entity.getCoveredActs(),
                entity.isActiveAt(reference),
                entity.getCreatedByUserId());
    }

    private EmergencyMedicoLegalResponse.IdentityStatement toIdentityStatement(
            EmergencyIdentityStatementEntity entity) {
        return new EmergencyMedicoLegalResponse.IdentityStatement(
                entity.getId(),
                entity.getThirdParty() == null ? null : entity.getThirdParty().getId(),
                entity.getFieldName(),
                entity.getDeclaredValue(),
                entity.getConfidenceLevel(),
                entity.getProofReference(),
                entity.getDeclaredAt(),
                entity.getCreatedByUserId());
    }

    private EmergencyMedicoLegalResponse.Belonging toBelonging(
            EmergencyBelongingEntity entity,
            List<EmergencyBelongingTransferEntity> transfers) {
        return new EmergencyMedicoLegalResponse.Belonging(
                entity.getId(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getItemCondition(),
                entity.getSealNumber(),
                entity.getCustodyStatus(),
                entity.getDepositedByName(),
                entity.getReceivedByUserId(),
                entity.getCreatedAt(),
                transfers.stream()
                        .sorted(Comparator.comparing(EmergencyBelongingTransferEntity::getOccurredAt))
                        .map(this::toBelongingTransfer)
                        .toList());
    }

    private EmergencyMedicoLegalResponse.BelongingTransfer toBelongingTransfer(
            EmergencyBelongingTransferEntity entity) {
        return new EmergencyMedicoLegalResponse.BelongingTransfer(
                entity.getId(),
                entity.getActionType(),
                entity.getFromCustodian(),
                entity.getRecipientName(),
                entity.getRecipientIdDocument(),
                entity.getNotes(),
                entity.getOccurredAt(),
                entity.getPerformedByUserId());
    }
}
