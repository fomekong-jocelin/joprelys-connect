package com.joprelys.backend.eco.api;

import com.joprelys.backend.eco.api.EcoDtos.ConsentResource;
import com.joprelys.backend.eco.api.EcoDtos.Grantee;
import com.joprelys.backend.eco.api.EcoDtos.Meta;
import com.joprelys.backend.eco.api.EcoDtos.PatientLinkResource;
import com.joprelys.backend.eco.api.EcoDtos.PatientLinkResponse;
import com.joprelys.backend.eco.api.EcoDtos.Period;
import com.joprelys.backend.eco.api.EcoDtos.Revocation;
import com.joprelys.backend.eco.application.PatientLinkView;
import com.joprelys.backend.eco.config.EcoProperties;
import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Projette liaison et consentement vers les ressources contractuelles ; aucune donnée d'identité exposée. */
@Component
public class EcoResourceMapper {

    private final EcoProperties properties;

    public EcoResourceMapper(EcoProperties properties) {
        this.properties = properties;
    }

    public PatientLinkResponse toResponse(PatientLinkView view) {
        return new PatientLinkResponse(toResource(view.link(), view.consent()), toResource(view.consent(), view.link()));
    }

    private PatientLinkResource toResource(EcoPatientLinkEntity link, EcoConsentEntity consent) {
        Instant lastUpdated = link.getRevokedAt() != null ? link.getRevokedAt() : link.getCreatedAt();
        return new PatientLinkResource(
                "PatientLink",
                link.getId().toString(),
                new Meta(properties.source(), link.getVersionId(), iso(lastUpdated)),
                link.getExternalProfileId().toString(),
                link.getDpu(),
                link.getIssuer(),
                link.getRelationship().contractValue(),
                "Consent/" + consent.getId(),
                link.getStatus().contractValue(),
                iso(link.getCreatedAt()));
    }

    private ConsentResource toResource(EcoConsentEntity consent, EcoPatientLinkEntity link) {
        Revocation revocation = consent.getStatus() == EcoStatus.REVOKED
                ? new Revocation(iso(consent.getRevokedAt()), consent.getRevokedBy().contractValue(),
                        consent.getRevocationReason())
                : null;
        return new ConsentResource(
                "Consent",
                consent.getId().toString(),
                new Meta(properties.source(), consent.getVersionId(), null),
                consent.getStatus().contractValue(),
                consent.scopeSet().stream().map(ConsentScope::contractValue).toList(),
                "PatientLink/" + link.getId(),
                new Grantee(consent.getGranteeType().contractValue(), consent.getGranteeReference()),
                new Period(iso(consent.getPeriodStart()), consent.getPeriodEnd() == null ? null : iso(consent.getPeriodEnd())),
                iso(consent.getGrantedAt()),
                revocation);
    }

    private static String iso(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
