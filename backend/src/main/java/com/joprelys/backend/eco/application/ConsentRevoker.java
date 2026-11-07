package com.joprelys.backend.eco.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.eco.domain.RevokedBy;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Révocation d'un consentement et de sa liaison, commune à l'API écosystème et au portail patient. */
@Component
public class ConsentRevoker {

    private final AuditService auditService;

    public ConsentRevoker(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * @param channel canal de la révocation, journalisé ({@code api} ou {@code portal})
     * @return {@code true} si le consentement était actif et vient d'être révoqué
     */
    public boolean revoke(EcoConsentEntity consent, EcoPatientLinkEntity link, RevokedBy revokedBy, String reason,
                          Instant when, String channel) {
        boolean changed = consent.revoke(when, revokedBy, reason);
        link.revoke(when);
        if (changed) {
            auditService.logSuccess(null, null, link.getPatientId(), "ECO_CONSENT", consent.getId(), "REVOKE",
                    "application=" + link.getApplication() + " revokedBy=" + revokedBy.contractValue()
                            + " channel=" + channel);
        }
        return changed;
    }
}
