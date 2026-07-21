package com.joprelys.backend.visit.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocumentEntity, UUID> {

    @Query("SELECT d FROM MedicalDocumentEntity d WHERE d.visit.id = :visitId AND d.documentType = com.joprelys.backend.visit.infrastructure.persistence.DocumentType.COMPTE_RENDU_CONSULTATION AND d.status = com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus.VALID")
    Optional<MedicalDocumentEntity> findByVisitId(@Param("visitId") UUID visitId);

    @Query("SELECT d FROM MedicalDocumentEntity d WHERE d.visit.id = :visitId AND d.documentType = com.joprelys.backend.visit.infrastructure.persistence.DocumentType.ORDONNANCE AND d.status = com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus.VALID")
    Optional<MedicalDocumentEntity> findPrescriptionDocumentByVisitId(@Param("visitId") UUID visitId);

    Optional<MedicalDocumentEntity> findByDocumentNumber(String documentNumber);

    @Query("""
        SELECT d FROM MedicalDocumentEntity d
        JOIN FETCH d.visit v
        JOIN FETCH v.patient
        WHERE v.id = :visitId AND d.documentType = com.joprelys.backend.visit.infrastructure.persistence.DocumentType.COMPTE_RENDU_CONSULTATION AND d.status = com.joprelys.backend.visit.infrastructure.persistence.DocumentStatus.VALID
    """)
    Optional<MedicalDocumentEntity> findByVisitIdWithVisitAndPatient(@Param("visitId") UUID visitId);

    @Query("""
        SELECT d FROM MedicalDocumentEntity d
        JOIN FETCH d.visit v
        JOIN FETCH v.patient
        WHERE d.id = :documentId
    """)
    Optional<MedicalDocumentEntity> findByIdWithVisitAndPatient(@Param("documentId") UUID documentId);

    @Query("SELECT d FROM MedicalDocumentEntity d WHERE d.visit.id = :visitId AND d.documentType = :documentType ORDER BY d.version DESC")
    List<MedicalDocumentEntity> findAllByVisitIdAndDocumentTypeOrderByVersionDesc(
            @Param("visitId") UUID visitId,
            @Param("documentType") DocumentType documentType);

    @Query("""
        SELECT d FROM MedicalDocumentEntity d
        JOIN FETCH d.visit v
        JOIN FETCH v.patient p
        WHERE p.id IN :patientIds
        ORDER BY d.createdAt DESC
    """)
    List<MedicalDocumentEntity> findByPatientIdsWithVisitAndPatient(
            @Param("patientIds") Collection<UUID> patientIds);
}
