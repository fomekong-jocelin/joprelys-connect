package com.joprelys.backend.eco.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.eco.application.EcoBusinessException.EcoError;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentRevocationService implements ConsentRevocationUseCase {

    private final EcoConsentRepository consentRepository;
    private final EcoPatientLinkRepository linkRepository;
    private final AuditService auditService;
    private final Clock clock;

    public ConsentRevocationService(EcoConsentRepository consentRepository, EcoPatientLinkRepository linkRepository,
                                    AuditService auditService, Clock clock) {
        this.consentRepository = consentRepository;
        this.linkRepository = linkRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PatientLinkView revoke(RevokeConsentCommand command) {
        // Contrôle d'accès par ressource : une application ne voit que les consentements dont elle est bénéficiaire.
        // Un consentement d'une autre application répond comme un consentement inexistant.
        EcoConsentEntity consent = consentRepository.findById(command.consentId())
                .filter(candidate -> candidate.getGranteeReference().equals(command.caller().application()))
                .orElseThrow(() -> new EcoBusinessException(EcoError.CONSENT_NOT_FOUND));
        EcoPatientLinkEntity link = linkRepository.findById(consent.getPatientLinkId())
                .orElseThrow(() -> new IllegalStateException("Consentement sans liaison : " + consent.getId()));

        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        boolean changed = consent.revoke(now, command.revokedBy(), command.reason().orElse(null));
        link.revoke(now);
        if (changed) {
            auditService.logSuccess(null, null, link.getPatientId(), "ECO_CONSENT", consent.getId(), "REVOKE",
                    "application=" + command.caller().application() + " revokedBy=" + command.revokedBy().contractValue());
        }
        return new PatientLinkView(link, consent, false);
    }
}
