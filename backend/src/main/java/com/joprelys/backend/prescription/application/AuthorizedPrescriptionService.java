package com.joprelys.backend.prescription.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.patient.application.PatientAccessPolicyService;
import com.joprelys.backend.patient.application.PatientService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.prescription.api.PrescriptionResponse;
import com.joprelys.backend.prescription.api.SavePrescriptionRequest;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizedPrescriptionService implements PrescriptionUseCase {
    private final PrescriptionService prescriptions;
    private final PatientAccessPolicyService access;
    private final PatientRepository patients;
    private final ConsultationRepository consultations;
    private final PrescriptionRepository repository;
    private final UserAccountRepository users;
    public AuthorizedPrescriptionService(PrescriptionService prescriptions, PatientAccessPolicyService access,
            PatientRepository patients, ConsultationRepository consultations,
            PrescriptionRepository repository, UserAccountRepository users) {
        this.prescriptions = prescriptions; this.access = access; this.patients = patients;
        this.consultations = consultations; this.repository = repository; this.users = users;
    }
    @Override public PrescriptionResponse save(UUID id, SavePrescriptionRequest request) {
        return inScope(consultationPatient(id), () -> PrescriptionResponse.fromEntity(prescriptions.savePrescription(id, request)));
    }
    @Override public Optional<PrescriptionResponse> get(UUID id) {
        return inScope(consultationPatient(id), () -> prescriptions.getPrescription(id).map(PrescriptionResponse::fromEntity));
    }
    @Override public PrescriptionResponse transmit(UUID id, Authentication actor) {
        return inScope(prescriptionPatient(id), () -> PrescriptionResponse.fromEntity(prescriptions.transmitPrescriptionForStaff(id, actor.getName())));
    }
    @Override public PrescriptionResponse finalizePrescription(UUID id, Authentication actor) {
        return inScope(prescriptionPatient(id), () -> PrescriptionResponse.fromEntity(prescriptions.finalizePrescription(id, actorId(actor))));
    }
    @Override public PrescriptionResponse cancel(UUID id, Authentication actor) {
        return inScope(prescriptionPatient(id), () -> PrescriptionResponse.fromEntity(prescriptions.cancelPrescription(id, actorId(actor))));
    }
    private UUID consultationPatient(UUID id) {
        return PatientService.convertToUuid(consultations.findPatientIdByConsultationId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultation introuvable.")));
    }
    private UUID prescriptionPatient(UUID id) {
        return PatientService.convertToUuid(repository.findPatientIdByPrescriptionId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordonnance introuvable.")));
    }
    private <T> T inScope(UUID patientId, Supplier<T> action) {
        access.validateAccess(patientId, "prescriptions");
        var patient = patients.findByIdGlobally(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient introuvable."));
        UUID previous = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(patient.getOrganizationId());
            return action.get();
        } finally { TenantContext.setTenantId(previous); }
    }
    private UUID actorId(Authentication actor) {
        if (actor == null || !actor.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise.");
        return users.findByEmail(actor.getName().trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.isEnabled()).map(user -> user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable."));
    }
}
