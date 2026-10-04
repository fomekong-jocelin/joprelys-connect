package com.joprelys.backend.prescription.application;
import com.joprelys.backend.prescription.api.PrescriptionResponse;
import com.joprelys.backend.prescription.api.SavePrescriptionRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
public interface PrescriptionUseCase {
    PrescriptionResponse save(UUID consultationId, SavePrescriptionRequest request);
    Optional<PrescriptionResponse> get(UUID consultationId);
    PrescriptionResponse transmit(UUID id, Authentication actor);
    PrescriptionResponse finalizePrescription(UUID id, Authentication actor);
    PrescriptionResponse cancel(UUID id, Authentication actor);
}
