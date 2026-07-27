package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClinicalNoteValidationServiceTest {

    private ClinicalNoteProjectionService projectionService;
    private ClinicalNoteValidationRepository validationRepository;
    private ClinicalNoteValidationFactRepository validationFactRepository;
    private VisitRepository visitRepository;
    private ClinicalNoteValidationService service;

    @BeforeEach
    void setUp() {
        projectionService = mock(ClinicalNoteProjectionService.class);
        validationRepository = mock(ClinicalNoteValidationRepository.class);
        validationFactRepository = mock(ClinicalNoteValidationFactRepository.class);
        visitRepository = mock(VisitRepository.class);
        service = new ClinicalNoteValidationService(
                projectionService,
                validationRepository,
                validationFactRepository,
                visitRepository);
    }

    @Test
    void shouldPersistMatchingProjectionAndExactFactReferences() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID symptomFactId = UUID.randomUUID();
        UUID assessmentFactId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);

        NoteProjectionView projection = projection(
                visitId,
                "clinical-note-projection-v1:abc",
                List.of(
                        new NoteSectionView(
                                NoteSectionCode.HISTORY_OF_PRESENT_ILLNESS,
                                List.of(entry(symptomFactId, 2, "SYMPTOM"))),
                        new NoteSectionView(
                                NoteSectionCode.ASSESSMENT,
                                List.of(entry(assessmentFactId, 7, "ASSESSMENT")))));
        when(projectionService.project(visitId, organizationId)).thenReturn(projection);
        when(validationRepository.findByVisitIdAndValidationRequestId(visitId, requestId))
                .thenReturn(Optional.empty());
        when(validationRepository.findByVisitIdAndProjectionVersionAndValidatedByUserId(
                visitId, projection.projectionVersion(), userId))
                .thenReturn(Optional.empty());
        when(validationRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.validate(
                visitId,
                userId,
                organizationId,
                new ValidateProjectionRequest(requestId, projection.projectionVersion()));

        assertThat(result.validationId()).isEqualTo(requestId);
        assertThat(result.projectionVersion()).isEqualTo(projection.projectionVersion());
        assertThat(result.maxFactSequence()).isEqualTo(7);
        assertThat(result.facts()).hasSize(2);
        assertThat(result.facts().get(0).factId()).isEqualTo(symptomFactId);
        assertThat(result.facts().get(0).sectionCode()).isEqualTo("HISTORY_OF_PRESENT_ILLNESS");
        assertThat(result.facts().get(0).position()).isZero();
        assertThat(result.facts().get(1).factId()).isEqualTo(assessmentFactId);
        assertThat(result.facts().get(1).sectionCode()).isEqualTo("ASSESSMENT");
        assertThat(result.facts().get(1).position()).isZero();
        verify(validationFactRepository).saveAllAndFlush(any());
    }

    @Test
    void shouldRejectStaleProjectionBeforeAnyValidationWrite() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);

        NoteProjectionView current = projection(
                visitId,
                "clinical-note-projection-v1:new",
                List.of(new NoteSectionView(
                        NoteSectionCode.ASSESSMENT,
                        List.of(entry(UUID.randomUUID(), 3, "ASSESSMENT")))));
        when(projectionService.project(visitId, organizationId)).thenReturn(current);
        when(validationRepository.findByVisitIdAndValidationRequestId(visitId, requestId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(
                visitId,
                userId,
                organizationId,
                new ValidateProjectionRequest(requestId, "clinical-note-projection-v1:old")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_NOTE_PROJECTION_STALE");

        verify(validationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(validationFactRepository);
    }

    @Test
    void shouldReturnSuccessfulValidationOnStrictIdempotentRetryWithoutReprojection() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String version = "clinical-note-projection-v1:stable";
        prepareLockedVisit(visitId, organizationId);

        ClinicalNoteValidationEntity existing = new ClinicalNoteValidationEntity(
                organizationId,
                visitId,
                requestId,
                version,
                ClinicalNoteProjectionService.PROJECTION_SCHEMA_VERSION,
                9,
                userId);
        when(validationRepository.findByVisitIdAndValidationRequestId(visitId, requestId))
                .thenReturn(Optional.of(existing));
        when(validationFactRepository.findByValidationIdOrderBySectionCodeAscPositionNoAsc(
                existing.getId()))
                .thenReturn(List.of());

        var result = service.validate(
                visitId,
                userId,
                organizationId,
                new ValidateProjectionRequest(requestId, version));

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.projectionVersion()).isEqualTo(version);
        verifyNoInteractions(projectionService);
        verify(validationRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectValidationIdReusedForAnotherProjection() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);

        ClinicalNoteValidationEntity existing = new ClinicalNoteValidationEntity(
                organizationId,
                visitId,
                requestId,
                "clinical-note-projection-v1:first",
                ClinicalNoteProjectionService.PROJECTION_SCHEMA_VERSION,
                4,
                userId);
        when(validationRepository.findByVisitIdAndValidationRequestId(visitId, requestId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.validate(
                visitId,
                userId,
                organizationId,
                new ValidateProjectionRequest(requestId, "clinical-note-projection-v1:second")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_NOTE_VALIDATION_ID_REUSED");
        verifyNoInteractions(projectionService);
    }

    @Test
    void shouldRefuseAnEmptyProjection() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        prepareLockedVisit(visitId, organizationId);

        NoteProjectionView empty = new NoteProjectionView(
                visitId,
                "clinical-note-projection-v1:empty",
                0,
                List.of());
        when(projectionService.project(visitId, organizationId)).thenReturn(empty);
        when(validationRepository.findByVisitIdAndValidationRequestId(visitId, requestId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(
                visitId,
                userId,
                organizationId,
                new ValidateProjectionRequest(requestId, empty.projectionVersion())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_NOTE_PROJECTION_EMPTY");

        verify(validationRepository, never()).saveAndFlush(any());
    }

    private void prepareLockedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
    }

    private NoteProjectionView projection(
            UUID visitId,
            String version,
            List<NoteSectionView> sections) {
        long maxSequence = sections.stream()
                .flatMap(section -> section.entries().stream())
                .mapToLong(NoteEntryView::factSequence)
                .max()
                .orElse(0);
        return new NoteProjectionView(visitId, version, maxSequence, sections);
    }

    private NoteEntryView entry(UUID factId, long sequence, String factType) {
        return new NoteEntryView(
                factId,
                sequence,
                factType,
                "CLINICIAN_DECISION",
                factType + "_CODE",
                factType.toLowerCase(),
                "POSITIVE",
                null,
                null,
                null,
                null,
                "UNSPECIFIED",
                null,
                null,
                List.of());
    }
}
