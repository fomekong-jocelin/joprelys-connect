package com.joprelys.backend.prescription.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.application.DocumentService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PrescriptionDraftCompletenessTest {

    @Test
    void shouldRejectFinalizationWhenAnExistingDraftItemHasNoDosage() {
        PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
        ConsultationEntity consultation = mock(ConsultationEntity.class);
        PrescriptionEntity prescription = new PrescriptionEntity(consultation);
        prescription.setStatus("DRAFT");
        prescription.getItems().add(new PrescriptionItemEntity(
                prescription,
                "Inhalateur",
                "",
                null,
                null,
                null,
                null,
                0));
        when(prescriptionRepository.findByIdWithConsultationAndItems(prescription.getId()))
                .thenReturn(Optional.of(prescription));

        DocumentService documentService = mock(DocumentService.class);
        PrescriptionService service = new PrescriptionService(
                prescriptionRepository,
                mock(ConsultationRepository.class),
                mock(PrescriptionNumberGenerator.class),
                mock(AlloPharmaClient.class),
                mock(AuditService.class),
                mock(UserAccountRepository.class),
                mock(PatientRepository.class),
                documentService,
                mock(com.joprelys.backend.consultation.application.MedicalSigningPolicy.class),
                mock(com.joprelys.backend.visit.infrastructure.persistence.VisitRepository.class));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.finalizePrescription(prescription.getId(), UUID.randomUUID()));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(prescriptionRepository, never()).save(prescription);
        verifyNoInteractions(documentService);
    }
}
