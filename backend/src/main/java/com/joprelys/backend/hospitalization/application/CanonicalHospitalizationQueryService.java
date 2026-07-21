package com.joprelys.backend.hospitalization.application;

import com.joprelys.backend.hospitalization.api.HospitalizationResponse;
import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository;
import com.joprelys.backend.patient.reconciliation.application.PatientCanonicalResolver;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanonicalHospitalizationQueryService {

    private final HospitalizationRepository hospitalizationRepository;
    private final PatientCanonicalResolver canonicalResolver;

    public CanonicalHospitalizationQueryService(
            HospitalizationRepository hospitalizationRepository,
            PatientCanonicalResolver canonicalResolver) {
        this.hospitalizationRepository = hospitalizationRepository;
        this.canonicalResolver = canonicalResolver;
    }

    @Transactional(readOnly = true)
    public List<HospitalizationResponse> list(UUID patientId) {
        var context = canonicalResolver.resolve(patientId);
        return hospitalizationRepository
                .findByPatientIdsOrderByAdmittedAtDesc(context.contributingPatientIds())
                .stream()
                .map(HospitalizationResponse::fromEntity)
                .toList();
    }
}
