package com.joprelys.backend.patient.reconciliation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.patient.application.PatientSimilarityService;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PatientReconciliationCandidateServiceTest {

    private PatientRepository patientRepository;
    private PatientReconciliationEventRepository eventRepository;
    private PatientReconciliationCandidateService candidateService;
    private PatientEntity source;
    private PatientEntity unconfirmedUrgTemp;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        eventRepository = mock(PatientReconciliationEventRepository.class);
        PatientDuplicateCandidateRepository duplicateRepository = mock(PatientDuplicateCandidateRepository.class);
        PatientSimilarityService similarityService = new PatientSimilarityService(
                patientRepository,
                duplicateRepository);
        candidateService = new PatientReconciliationCandidateService(
                patientRepository,
                similarityService,
                eventRepository);

        source = verifiedUrgTemp("URG-TEMP-20260712-100001", "DPU-SOURCE", "PAT-SOURCE");
        unconfirmedUrgTemp = verifiedUrgTemp("URG-TEMP-20260712-100002", "DPU-TARGET", "PAT-TARGET");

        when(patientRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(patientRepository.findById(unconfirmedUrgTemp.getId())).thenReturn(Optional.of(unconfirmedUrgTemp));
        when(patientRepository.findAll(
                any(Specification.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unconfirmedUrgTemp)));
    }

    @Test
    void shouldExcludeVerifiedUrgTempUntilItIsExplicitlyConfirmedAsNewDpu() {
        when(eventRepository.findFirstBySourcePatient_IdOrderByCreatedAtDesc(unconfirmedUrgTemp.getId()))
                .thenReturn(Optional.empty());

        assertEquals(0, candidateService.findCandidates(source.getId()).size());
    }

    @Test
    void shouldRejectDirectLinkToUnconfirmedUrgTemp() {
        when(eventRepository.findFirstBySourcePatient_IdOrderByCreatedAtDesc(unconfirmedUrgTemp.getId()))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> candidateService.scoreCandidate(source.getId(), unconfirmedUrgTemp.getId()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PATIENT_RECONCILIATION_TARGET_NOT_ELIGIBLE", exception.getReason());
    }

    @Test
    void shouldAllowUrgTempExplicitlyConfirmedAsNewDpu() {
        PatientReconciliationEventEntity event = mock(PatientReconciliationEventEntity.class);
        when(event.getDecision()).thenReturn(PatientReconciliationDecision.CREATE_NEW_DPU);
        when(eventRepository.findFirstBySourcePatient_IdOrderByCreatedAtDesc(unconfirmedUrgTemp.getId()))
                .thenReturn(Optional.of(event));

        assertEquals(
                unconfirmedUrgTemp.getId(),
                candidateService.findCandidates(source.getId()).getFirst().patientId());
    }

    @Test
    void shouldBoundDatabasePreselectionBeforeJavaScoring() {
        when(eventRepository.findFirstBySourcePatient_IdOrderByCreatedAtDesc(unconfirmedUrgTemp.getId()))
                .thenReturn(Optional.empty());

        candidateService.findCandidates(source.getId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).findAll(
                any(Specification.class),
                pageableCaptor.capture());
        assertEquals(250, pageableCaptor.getValue().getPageSize());
        assertEquals("updatedAt: DESC", pageableCaptor.getValue().getSort().toString());
    }

    private static PatientEntity verifiedUrgTemp(
            String temporaryNumber,
            String globalNumber,
            String localNumber) {
        PatientEntity patient = PatientEntity.provisionalEmergency(
                globalNumber,
                localNumber,
                temporaryNumber,
                "FEMININ",
                "30-40",
                "Patiente consciente",
                Instant.parse("2026-07-12T06:00:00Z"),
                "Douala",
                IdentityConfidenceLevel.HIGH);
        patient.setFullName("Nadège Maffock");
        patient.setGender("FEMININ");
        patient.setBirthDate(LocalDate.of(1994, 5, 10));
        patient.setPhone("+237699000111");
        patient.setCity("Douala");
        patient.transitionIdentityStatus(PatientIdentityStatus.VERIFIED);
        return patient;
    }
}
