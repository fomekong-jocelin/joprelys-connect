package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientCanonicalLinkRepository extends JpaRepository<PatientCanonicalLinkEntity, UUID> {

    Optional<PatientCanonicalLinkEntity> findBySourcePatient_Id(UUID sourcePatientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PatientCanonicalLinkEntity> findForUpdateBySourcePatient_Id(UUID sourcePatientId);

    List<PatientCanonicalLinkEntity> findAllByCanonicalPatient_Id(UUID canonicalPatientId);
}
