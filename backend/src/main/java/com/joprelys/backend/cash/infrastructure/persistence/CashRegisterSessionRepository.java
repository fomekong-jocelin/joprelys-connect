package com.joprelys.backend.cash.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterSessionRepository extends JpaRepository<CashRegisterSessionEntity, UUID> {
    Optional<CashRegisterSessionEntity> findByOpenedByUserIdAndStatus(UUID openedByUserId, String status);
    List<CashRegisterSessionEntity> findByStatus(String status);
    List<CashRegisterSessionEntity> findByCashRegisterIdOrderByOpenedAtDesc(UUID cashRegisterId);
    List<CashRegisterSessionEntity> findAllByOrderByOpenedAtDesc();
    List<CashRegisterSessionEntity> findTop20ByOpenedByUserIdOrderByOpenedAtDesc(UUID openedByUserId);
}