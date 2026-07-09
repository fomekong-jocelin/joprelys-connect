package com.joprelys.backend.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TariffGridRepository extends JpaRepository<TariffGridEntity, UUID> {
    Optional<TariffGridEntity> findByKeyLetter(String keyLetter);
}
