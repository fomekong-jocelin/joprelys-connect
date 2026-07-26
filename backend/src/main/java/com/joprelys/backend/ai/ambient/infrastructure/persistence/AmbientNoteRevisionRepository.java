package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AmbientNoteRevisionRepository extends JpaRepository<AmbientNoteRevisionEntity, UUID> {

    Optional<AmbientNoteRevisionEntity> findFirstByVisitIdOrderByRevisionNoDesc(UUID visitId);

    Optional<AmbientNoteRevisionEntity> findByIdAndVisitId(UUID id, UUID visitId);

    @Query("select coalesce(max(note.revisionNo), 0) from AmbientNoteRevisionEntity note where note.visitId = :visitId")
    long findMaximumRevision(@Param("visitId") UUID visitId);
}
