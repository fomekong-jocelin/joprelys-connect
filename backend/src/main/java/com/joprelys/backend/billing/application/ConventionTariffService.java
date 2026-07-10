package com.joprelys.backend.billing.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.billing.api.InsuranceConventionDto;
import com.joprelys.backend.billing.infrastructure.persistence.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion des conventions d'assurance et de la grille tarifaire.
 */
@Service
public class ConventionTariffService {

    private final InsuranceConventionRepository insuranceConventionRepository;
    private final TariffGridRepository tariffGridRepository;
    private final UserAccountRepository userAccountRepository;

    public ConventionTariffService(InsuranceConventionRepository insuranceConventionRepository,
                                   TariffGridRepository tariffGridRepository,
                                   UserAccountRepository userAccountRepository) {
        this.insuranceConventionRepository = insuranceConventionRepository;
        this.tariffGridRepository = tariffGridRepository;
        this.userAccountRepository = userAccountRepository;
    }

    private UserAccountEntity getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userAccountRepository.findByEmail(email).orElse(null);
    }

    /**
     * Retourne la valeur unitaire d'une lettre-clé tarifaire, ou la valeur par défaut si absente.
     */
    public BigDecimal getTariff(String keyLetter, BigDecimal defaultValue) {
        return tariffGridRepository.findByKeyLetter(keyLetter)
                .map(TariffGridEntity::getUnitValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public List<InsuranceConventionDto> listConventions() {
        return insuranceConventionRepository.findAll().stream()
                .map(InsuranceConventionDto::fromEntity)
                .toList();
    }

    @Transactional
    public InsuranceConventionDto createConvention(String name, BigDecimal coveragePercentage) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;
        InsuranceConventionEntity entity = new InsuranceConventionEntity(name, coveragePercentage);
        entity.setOrganizationId(orgId);
        return InsuranceConventionDto.fromEntity(insuranceConventionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<TariffGridEntity> listTariffs() {
        return tariffGridRepository.findAll();
    }

    @Transactional
    public TariffGridEntity createOrUpdateTariff(String keyLetter, BigDecimal unitValue) {
        UserAccountEntity actor = getCurrentUser();
        UUID orgId = actor != null ? actor.getOrganizationId() : null;
        Optional<TariffGridEntity> existing = tariffGridRepository.findByKeyLetter(keyLetter);
        TariffGridEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setUnitValue(unitValue);
        } else {
            entity = new TariffGridEntity(keyLetter, unitValue);
            entity.setOrganizationId(orgId);
        }
        return tariffGridRepository.save(entity);
    }
}
