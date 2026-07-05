package com.joprelys.backend.patient.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PatientDuplicateCandidateRepository extends JpaRepository<PatientDuplicateCandidateEntity, UUID> {
    
    @Query("SELECT c FROM PatientDuplicateCandidateEntity c JOIN FETCH c.sourcePatient JOIN FETCH c.targetPatient WHERE c.status = 'PENDING'")
    List<PatientDuplicateCandidateEntity> findAllPending();

    @Query("SELECT c FROM PatientDuplicateCandidateEntity c WHERE " +
           "(c.sourcePatient.id = :p1 AND c.targetPatient.id = :p2) OR " +
           "(c.sourcePatient.id = :p2 AND c.targetPatient.id = :p1)")
    java.util.Optional<PatientDuplicateCandidateEntity> findByPatientPair(UUID p1, UUID p2);
}