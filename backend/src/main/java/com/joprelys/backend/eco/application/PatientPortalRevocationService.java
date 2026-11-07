package com.joprelys.backend.eco.application;

import com.joprelys.backend.eco.application.EcoBusinessException.EcoError;
import com.joprelys.backend.eco.application.event.ConsentRevokedEvent;
import com.joprelys.backend.eco.application.event.EcoEventPublisher;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.domain.RevokedBy;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientPortalRevocationService implements PatientConsentRevocationUseCase {

    private final EcoPatientLinkRepository linkRepository;
    private final EcoConsentRepository consentRepository;
    private final ConsentRevoker revoker;
    private final EcoEventPublisher publisher;
    private final Clock clock;

    public PatientPortalRevocationService(EcoPatientLinkRepository linkRepository,
                                          EcoConsentRepository consentRepository, ConsentRevoker revoker,
                                          EcoEventPublisher publisher, Clock clock) {
        this.linkRepository = linkRepository;
        this.consentRepository = consentRepository;
        this.revoker = revoker;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void revokeByPatient(UUID patientId, UUID patientLinkId) {
        // Contrôle d'accès par ressource : la liaison d'un autre patient répond comme une liaison inexistante.
        EcoPatientLinkEntity link = linkRepository.findById(patientLinkId)
                .filter(candidate -> candidate.getPatientId().equals(patientId))
                .orElseThrow(() -> new EcoBusinessException(EcoError.PATIENT_LINK_NOT_FOUND));
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);

        Optional<EcoConsentEntity> active = consentRepository.findFirstByPatientLinkIdAndStatus(
                link.getId(), EcoStatus.ACTIVE);
        if (active.isEmpty()) {
            link.revoke(now);
            return;
        }
        EcoConsentEntity consent = active.get();
        if (revoker.revoke(consent, link, RevokedBy.PATIENT, null, now, "portal")) {
            publisher.publishConsentRevoked(new ConsentRevokedEvent(
                    link.getApplication(), consent.getId(), link.getId(), now, RevokedBy.PATIENT));
        }
    }
}
