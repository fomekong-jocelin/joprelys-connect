package com.joprelys.backend.emergency.medicolegal.application;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyInformationSourceType;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyThirdPartyQuality;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyThirdPartyEntity;
import com.joprelys.backend.emergency.medicolegal.infrastructure.persistence.EmergencyThirdPartyRepository;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmergencyArrivalThirdPartyService {

    private final EmergencyThirdPartyRepository thirdPartyRepository;

    public EmergencyArrivalThirdPartyService(EmergencyThirdPartyRepository thirdPartyRepository) {
        this.thirdPartyRepository = thirdPartyRepository;
    }

    public void capture(EmergencyEntity emergency, UUID actorUserId) {
        if (emergency.getThirdPartyName() == null || emergency.getThirdPartyName().isBlank()) {
            return;
        }

        UUID organizationId = TenantContext.getTenantId();
        if (organizationId == null) {
            throw new IllegalStateException("ORGANIZATION_REQUIRED");
        }

        thirdPartyRepository.save(new EmergencyThirdPartyEntity(
                organizationId,
                emergency,
                emergency.getThirdPartyName(),
                emergency.getThirdPartyPhone(),
                null,
                emergency.getThirdPartyIdDocument(),
                emergency.getThirdPartyRelationship(),
                emergency.getThirdPartyCircumstances(),
                emergency.isThirdPartyConsentToContact(),
                false,
                EmergencyInformationSourceType.ACCOMPANYING_PERSON,
                IdentityConfidenceLevel.LOW,
                null,
                Set.of(EmergencyThirdPartyQuality.ACCOMPANYING_PERSON),
                actorUserId));
    }
}
