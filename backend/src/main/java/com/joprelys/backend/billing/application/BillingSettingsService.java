package com.joprelys.backend.billing.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.BillingSettingsDto;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Paramètres financiers de l'établissement courant (ADR-20261017 D-13, D-17). */
@Service
public class BillingSettingsService {

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public BillingSettingsService(OrganizationRepository organizationRepository, UserAccountRepository userAccountRepository,
                                  AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public BillingSettingsDto get() {
        return BillingSettingsDto.from(organization(currentUser()));
    }

    @Transactional
    public BillingSettingsDto update(BillingSettingsDto request) {
        UserAccountEntity actor = currentUser();
        OrganizationEntity organization = organization(actor);
        BigDecimal threshold = request.creditNoteApprovalThreshold();
        if (threshold == null || threshold.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le seuil d'approbation des avoirs doit être positif ou nul.");
        }
        organization.updateBillingSettings(request.labPaymentBeforeSampling(), threshold);
        organizationRepository.save(organization);
        auditService.logSuccess(actor.getId(), organization.getId(), null, "ORGANIZATION", organization.getId(),
                "UPDATE_BILLING_SETTINGS", "Paiement avant prélèvement : " + request.labPaymentBeforeSampling()
                        + " ; seuil d'approbation des avoirs : " + threshold + " FCFA.");
        return BillingSettingsDto.from(organization);
    }

    private OrganizationEntity organization(UserAccountEntity actor) {
        if (actor.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez un établissement à administrer.");
        }
        return organizationRepository.findById(actor.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Établissement introuvable."));
    }

    private UserAccountEntity currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
    }
}
