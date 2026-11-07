package com.joprelys.backend.eco.application;

import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoConsentRepository;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientLinkedAppsService implements PatientLinkedAppsQuery {

    private final EcoPatientLinkRepository linkRepository;
    private final EcoConsentRepository consentRepository;

    public PatientLinkedAppsService(EcoPatientLinkRepository linkRepository, EcoConsentRepository consentRepository) {
        this.linkRepository = linkRepository;
        this.consentRepository = consentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedAppView> activeLinks(UUID patientId) {
        List<EcoPatientLinkEntity> links = linkRepository
                .findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, EcoStatus.ACTIVE);
        if (links.isEmpty()) {
            return List.of();
        }
        // Deux requêtes au total : les consentements actifs de toutes les liaisons sont lus d'un coup.
        Map<UUID, EcoConsentEntity> consentsByLink = consentRepository
                .findByPatientLinkIdInAndStatus(links.stream().map(EcoPatientLinkEntity::getId).toList(),
                        EcoStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(EcoConsentEntity::getPatientLinkId, Function.identity(),
                        (first, ignored) -> first));
        return links.stream().map(link -> toView(link, consentsByLink.get(link.getId()))).toList();
    }

    private static LinkedAppView toView(EcoPatientLinkEntity link, EcoConsentEntity consent) {
        Set<ConsentScope> scopes = consent == null ? Set.of() : consent.scopeSet();
        return new LinkedAppView(link.getId(), link.getApplication(), scopes, link.getCreatedAt(),
                consent == null ? null : consent.getPeriodEnd());
    }
}
