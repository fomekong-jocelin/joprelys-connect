package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingStatus;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingTransferAction;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyCapacityStatus;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyInformationSourceType;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyLegalBasisType;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyThirdPartyQuality;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record EmergencyMedicoLegalResponse(
        UUID emergencyId,
        List<ThirdParty> thirdParties,
        List<IdentityStatement> identityStatements,
        List<CapacityEvent> capacityHistory,
        CapacityEvent currentCapacity,
        List<LegalBasis> legalBases,
        LegalBasis activeLegalBasis,
        List<Belonging> belongings) {

    public record ThirdParty(
            UUID id,
            String fullName,
            String phone,
            String email,
            String idDocument,
            String relationshipToPatient,
            String circumstances,
            boolean consentToContact,
            boolean legalRepresentativeClaimed,
            EmergencyInformationSourceType sourceType,
            IdentityConfidenceLevel confidenceLevel,
            String proofReference,
            Set<EmergencyThirdPartyQuality> qualities,
            UUID createdByUserId,
            Instant createdAt) {
    }

    public record IdentityStatement(
            UUID id,
            UUID thirdPartyId,
            String fieldName,
            String value,
            IdentityConfidenceLevel confidenceLevel,
            String proofReference,
            Instant declaredAt,
            UUID createdByUserId) {
    }

    public record CapacityEvent(
            UUID id,
            EmergencyCapacityStatus status,
            String consciousnessLevel,
            String clinicalReason,
            Instant effectiveAt,
            UUID recordedByUserId) {
    }

    public record LegalBasis(
            UUID id,
            EmergencyLegalBasisType basisType,
            String justification,
            Instant startsAt,
            Instant expiresAt,
            Instant closedAt,
            String closureReason,
            Set<String> coveredActs,
            boolean active,
            UUID createdByUserId) {
    }

    public record Belonging(
            UUID id,
            String category,
            String description,
            int quantity,
            String itemCondition,
            String sealNumber,
            EmergencyBelongingStatus custodyStatus,
            String depositedByName,
            UUID receivedByUserId,
            Instant createdAt,
            List<BelongingTransfer> transfers) {
    }

    public record BelongingTransfer(
            UUID id,
            EmergencyBelongingTransferAction action,
            String fromCustodian,
            String recipientName,
            String recipientIdDocument,
            String notes,
            Instant occurredAt,
            UUID performedByUserId) {
    }
}
