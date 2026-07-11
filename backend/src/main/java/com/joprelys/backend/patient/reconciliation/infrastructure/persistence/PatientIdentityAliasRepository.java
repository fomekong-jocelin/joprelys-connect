package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import com.joprelys.backend.patient.reconciliation.domain.PatientAliasType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientIdentityAliasRepository extends JpaRepository<PatientIdentityAliasEntity, UUID> {

    Optional<PatientIdentityAliasEntity> findByAliasTypeAndAliasValue(
            PatientAliasType aliasType,
            String aliasValue);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PatientIdentityAliasEntity> findForUpdateByAliasTypeAndAliasValue(
            PatientAliasType aliasType,
            String aliasValue);

    List<PatientIdentityAliasEntity> findAllByOriginPatient_Id(UUID originPatientId);

    List<PatientIdentityAliasEntity> findAllByCanonicalPatient_Id(UUID canonicalPatientId);
}
