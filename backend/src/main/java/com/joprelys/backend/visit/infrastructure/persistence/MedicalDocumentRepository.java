package com.joprelys.backend.visit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocumentEntity, UUID> {

    @Query("SELECT d FROM MedicalDocumentEntity d WHERE d.visit.id = :visitId AND d.documentType = 'SYNTHESE'")
    Optional<MedicalDocumentEntity> findByVisitId(@Param("visitId") UUID visitId);

    @Query("SELECT d FROM MedicalDocumentEntity d WHERE d.visit.id = :visitId AND d.documentType = 'ORDONNANCE'")
    Optional<MedicalDocumentEntity> findPrescriptionDocumentByVisitId(@Param("visitId") UUID visitId);

    Optional<MedicalDocumentEntity> findByDocumentNumber(String documentNumber);

    /**
     * Charge le document avec sa VisitEntity et le PatientEntity en une seule requête JOIN FETCH.
     * À utiliser partout où on accède à doc.getVisit().getPatient() pour éviter LazyInitializationException.
     */
    @Query("""
        SELECT d FROM MedicalDocumentEntity d
        JOIN FETCH d.visit v
        JOIN FETCH v.patient
        WHERE v.id = :visitId AND d.documentType = 'SYNTHESE'
    """)
    Optional<MedicalDocumentEntity> findByVisitIdWithVisitAndPatient(@Param("visitId") UUID visitId);

    /**
     * Charge le document par son propre ID avec sa VisitEntity et le PatientEntity.
     * À utiliser dans revokeDocument et cancelDocument.
     */
    @Query("""
        SELECT d FROM MedicalDocumentEntity d
        JOIN FETCH d.visit v
        JOIN FETCH v.patient
        WHERE d.id = :documentId
    """)
    Optional<MedicalDocumentEntity> findByIdWithVisitAndPatient(@Param("documentId") UUID documentId);
}
