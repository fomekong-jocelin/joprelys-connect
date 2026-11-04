package com.joprelys.backend.visit.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.common.api.ApiStatusException;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitOrganizationClinicalWriteScopePolicy implements ClinicalWriteScopePolicy {

    static final String OUT_OF_SCOPE_MESSAGE =
            "Cette visite relève d'un autre établissement : vous pouvez consulter le dossier partagé, "
                    + "mais pas y enregistrer de données cliniques.";

    private final VisitRepository visitRepository;
    private final UserAccountRepository userAccountRepository;

    public VisitOrganizationClinicalWriteScopePolicy(
            VisitRepository visitRepository,
            UserAccountRepository userAccountRepository) {
        this.visitRepository = visitRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireActorInVisitOrganization(UUID visitId, String actorEmail) {
        UUID visitOrganizationId = visitRepository.findOrganizationIdByVisitId(visitId)
                .map(PatientService::convertToUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visite introuvable."));
        UserAccountEntity actor = userAccountRepository.findByEmail(normalize(actorEmail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Praticien introuvable."));
        requireSameOrganization(visitOrganizationId, actor.getOrganizationId());
    }

    @Override
    public void requireSameOrganization(VisitEntity visit, UserAccountEntity actor) {
        requireSameOrganization(visit.getOrganizationId(), actor.getOrganizationId());
    }

    private static void requireSameOrganization(UUID visitOrganizationId, UUID actorOrganizationId) {
        if (visitOrganizationId == null || !Objects.equals(visitOrganizationId, actorOrganizationId)) {
            throw new ApiStatusException(HttpStatus.FORBIDDEN, OUT_OF_SCOPE_CODE, OUT_OF_SCOPE_MESSAGE);
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
