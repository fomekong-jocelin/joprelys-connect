package com.joprelys.backend.eco.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.eco.application.EcoBusinessException.EcoError;
import com.joprelys.backend.eco.config.EcoProperties;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.domain.GranteeType;
import com.joprelys.backend.eco.domain.LinkRelationship;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientLinkService implements PatientLinkUseCase {

    static final Duration DEFAULT_CONSENT_DURATION = Duration.ofDays(365);
    static final Duration MAX_CONSENT_DURATION = Duration.ofDays(730);

    private final PatientRepository patientRepository;
    private final EcoPatientLinkRepository linkRepository;
    private final EcoConsentRepository consentRepository;
    private final AuditService auditService;
    private final EcoProperties properties;
    private final Clock clock;

    public PatientLinkService(PatientRepository patientRepository, EcoPatientLinkRepository linkRepository,
                              EcoConsentRepository consentRepository, AuditService auditService,
                              EcoProperties properties, Clock clock) {
        this.patientRepository = patientRepository;
        this.linkRepository = linkRepository;
        this.consentRepository = consentRepository;
        this.auditService = auditService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PatientLinkView link(CreatePatientLinkCommand command) {
        EcoCaller caller = command.caller();
        String dpu = caller.dpu().orElseThrow(() -> new EcoBusinessException(EcoError.MISSING_DPU));
        if (command.relationship() != LinkRelationship.SELF) {
            // Relier l'enfant ou l'adulte dépendant suppose une preuve de tutelle : story ultérieure.
            throw new EcoBusinessException(EcoError.UNSUPPORTED_RELATIONSHIP);
        }
        PatientEntity patient = patientRepository.findByGlobalPatientNumber(dpu)
                .orElseThrow(() -> new EcoBusinessException(EcoError.UNKNOWN_DPU));
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant periodEnd = resolvePeriodEnd(command, now);

        List<EcoPatientLinkEntity> active = linkRepository.findByApplicationAndExternalProfileIdAndStatus(
                caller.application(), command.externalProfileId(), EcoStatus.ACTIVE);
        for (EcoPatientLinkEntity existing : active) {
            if (!existing.getPatientId().equals(patient.getId())) {
                throw new EcoBusinessException(EcoError.PROFILE_LINKED_ELSEWHERE);
            }
            EcoConsentEntity consent = consentRepository.findFirstByPatientLinkIdAndStatus(existing.getId(), EcoStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalStateException("Liaison active sans consentement actif : " + existing.getId()));
            return new PatientLinkView(existing, consent, false);
        }

        EcoPatientLinkEntity link = linkRepository.save(new EcoPatientLinkEntity(
                patient.getId(), dpu, properties.source(), caller.application(), command.externalProfileId(),
                command.relationship(), caller.subject(), caller.clientId(), now));
        EcoConsentEntity consent = consentRepository.save(new EcoConsentEntity(
                link.getId(), command.scopes(), GranteeType.APPLICATION, caller.application(), now, periodEnd));
        auditService.logSuccess(null, patient.getOrganizationId(), patient.getId(), "ECO_PATIENT_LINK", link.getId(),
                "CREATE", "application=" + caller.application() + " client=" + caller.clientId());
        return new PatientLinkView(link, consent, true);
    }

    private static Instant resolvePeriodEnd(CreatePatientLinkCommand command, Instant now) {
        Instant end = command.periodEnd().orElse(now.plus(DEFAULT_CONSENT_DURATION));
        if (!end.isAfter(now) || end.isAfter(now.plus(MAX_CONSENT_DURATION))) {
            throw new EcoBusinessException(EcoError.INVALID_PERIOD);
        }
        return end.truncatedTo(ChronoUnit.SECONDS);
    }
}
