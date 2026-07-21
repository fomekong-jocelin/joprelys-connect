package com.joprelys.backend.emergency.document;

import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanonicalPatientDocumentService {

    private final PatientCanonicalResolver canonicalResolver;
    private final MedicalDocumentRepository documentRepository;

    public CanonicalPatientDocumentService(
            PatientCanonicalResolver canonicalResolver,
            MedicalDocumentRepository documentRepository) {
        this.canonicalResolver = canonicalResolver;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<EmergencyDocumentResponse> list(UUID patientId) {
        var context = canonicalResolver.resolve(patientId);
        return documentRepository
                .findByPatientIdsWithVisitAndPatient(context.contributingPatientIds())
                .stream()
                .map(EmergencyDocumentResponse::fromEntity)
                .toList();
    }
}
