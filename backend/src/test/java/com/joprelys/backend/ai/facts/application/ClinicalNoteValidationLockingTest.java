package com.joprelys.backend.ai.facts.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteEntryView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteSectionCode;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteSectionView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.ValidateProjectionRequest;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationFactRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ClinicalNoteValidationLockingTest {

    @Test
    void shouldLockVisitBeforeRecomputingProjectionForValidation() {
        ClinicalNoteProjectionService projectionService = mock(ClinicalNoteProjectionService.class);
        ClinicalNoteValidationRepository validationRepository = mock(ClinicalNoteValidationRepository.class);
        ClinicalNoteValidationFactRepository validationFactRepository = mock(ClinicalNoteValidationFactRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        ClinicalNoteValidationService service = new ClinicalNoteValidationService(
                projectionService,
                validationRepository,
                validationFactRepository,
                visitRepository);

        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String version = "clinical-note-projection-v1:locked";

        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(validationRepository.findByVisitIdAndValidationRequestId(visitId, requestId))
                .thenReturn(Optional.empty());

        NoteEntryView entry = new NoteEntryView(
                UUID.randomUUID(),
                1,
                "ASSESSMENT",
                "CLINICIAN_DECISION",
                "ASSESSMENT_CODE",
                "assessment",
                "POSITIVE",
                null,
                null,
                null,
                null,
                "UNSPECIFIED",
                null,
                null,
                List.of());
        NoteProjectionView projection = new NoteProjectionView(
                visitId,
                version,
                1,
                List.of(new NoteSectionView(NoteSectionCode.ASSESSMENT, List.of(entry))));
        when(projectionService.project(visitId, organizationId)).thenReturn(projection);

        ClinicalNoteValidationEntity existing = new ClinicalNoteValidationEntity(
                organizationId,
                visitId,
                UUID.randomUUID(),
                version,
                ClinicalNoteProjectionService.PROJECTION_SCHEMA_VERSION,
                1,
                userId);
        when(validationRepository.findByVisitIdAndProjectionVersionAndValidatedByUserId(
                visitId, version, userId))
                .thenReturn(Optional.of(existing));
        when(validationFactRepository.findByValidationIdOrderBySectionCodeAscPositionNoAsc(
                existing.getId()))
                .thenReturn(List.of());

        service.validate(
                visitId,
                userId,
                organizationId,
                new ValidateProjectionRequest(requestId, version));

        InOrder order = inOrder(visitRepository, projectionService);
        order.verify(visitRepository).findByIdForUpdate(visitId);
        order.verify(projectionService).project(visitId, organizationId);
    }
}
