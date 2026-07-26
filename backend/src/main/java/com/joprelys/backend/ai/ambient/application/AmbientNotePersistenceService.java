package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientNoteContract.NoteRevisionView;
import com.joprelys.backend.ai.ambient.application.AmbientNoteContract.NoteStatementView;
import com.joprelys.backend.ai.ambient.application.AmbientNoteFactualityGuard.GroundedNote;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteStatus;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientNoteRevisionEntity;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientNoteRevisionRepository;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientNoteStatementEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AmbientNotePersistenceService {

    private final AmbientNoteRevisionRepository noteRepository;
    private final VisitRepository visitRepository;

    public AmbientNotePersistenceService(
            AmbientNoteRevisionRepository noteRepository,
            VisitRepository visitRepository) {
        this.noteRepository = noteRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public NoteRevisionView persistGenerated(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            AmbientNoteTemplate template,
            String locale,
            long transcriptMaxSequence,
            String model,
            Integer tokensUsed,
            GroundedNote note) {
        lockAuthorizedVisit(visitId, organizationId);
        Optional<AmbientNoteRevisionEntity> latest = noteRepository.findFirstByVisitIdOrderByRevisionNoDesc(visitId);
        long revision = noteRepository.findMaximumRevision(visitId) + 1;
        AmbientNoteRevisionEntity entity = new AmbientNoteRevisionEntity(
                organizationId,
                visitId,
                revision,
                template,
                locale,
                transcriptMaxSequence,
                model,
                tokensUsed,
                userId,
                latest.map(AmbientNoteRevisionEntity::getId).orElse(null));
        for (var statement : note.statements()) {
            entity.addStatement(new AmbientNoteStatementEntity(
                    statement.section(),
                    statement.order(),
                    statement.text(),
                    statement.critical(),
                    statement.evidenceItemIds()));
        }
        return view(noteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public Optional<NoteRevisionView> latest(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        return noteRepository.findFirstByVisitIdOrderByRevisionNoDesc(visitId).map(this::view);
    }

    @Transactional(readOnly = true)
    public Optional<AmbientNoteRevisionEntity> latestEntity(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        Optional<AmbientNoteRevisionEntity> latest = noteRepository.findFirstByVisitIdOrderByRevisionNoDesc(visitId);
        latest.ifPresent(entity -> entity.getStatements().forEach(statement -> statement.getEvidenceItemIds().size()));
        return latest;
    }

    @Transactional
    public NoteRevisionView decide(
            UUID visitId,
            UUID noteId,
            UUID userId,
            UUID organizationId,
            String decision) {
        lockAuthorizedVisit(visitId, organizationId);
        AmbientNoteRevisionEntity entity = noteRepository.findByIdAndVisitId(noteId, visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "AI_AMBIENT_NOTE_NOT_FOUND"));
        AmbientNoteStatus target = switch (decision == null ? "" : decision.trim().toUpperCase()) {
            case "ACCEPT" -> AmbientNoteStatus.ACCEPTED;
            case "REJECT" -> AmbientNoteStatus.REJECTED;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_AMBIENT_NOTE_DECISION_INVALID");
        };
        try {
            entity.decide(target, userId, Instant.now());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return view(noteRepository.save(entity));
    }

    private VisitEntity lockAuthorizedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
        return visit;
    }

    private void requireAuthorizedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private NoteRevisionView view(AmbientNoteRevisionEntity entity) {
        List<String> sections = entity.getTemplateCode().sections();
        List<AmbientNoteStatementEntity> sorted = new ArrayList<>(entity.getStatements());
        sorted.sort(Comparator
                .comparingInt((AmbientNoteStatementEntity statement) -> {
                    int index = sections.indexOf(statement.getSectionCode());
                    return index < 0 ? Integer.MAX_VALUE : index;
                })
                .thenComparingInt(AmbientNoteStatementEntity::getStatementOrder));
        List<NoteStatementView> statements = sorted.stream()
                .map(statement -> new NoteStatementView(
                        statement.getId(),
                        statement.getSectionCode(),
                        statement.getStatementOrder(),
                        statement.getStatementText(),
                        statement.isCritical(),
                        List.copyOf(statement.getEvidenceItemIds())))
                .toList();
        return new NoteRevisionView(
                entity.getId(),
                entity.getVisitId(),
                entity.getRevisionNo(),
                entity.getTemplateCode().name(),
                entity.getLocale(),
                entity.getStatus().name(),
                entity.getTranscriptMaxSequence(),
                entity.getModelName(),
                entity.getTokensUsed(),
                entity.getSupersedesNoteId(),
                entity.getCreatedAt(),
                entity.getDecidedAt(),
                entity.getDecidedByUserId(),
                statements);
    }
}
