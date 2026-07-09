package com.joprelys.backend.cash.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, UUID> {
    Optional<CashRegisterEntity> findByCode(String code);
}
