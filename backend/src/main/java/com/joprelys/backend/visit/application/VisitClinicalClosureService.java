package com.joprelys.backend.visit.application;
import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.consultation.application.MedicalSigningPolicy;
import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationRepository;
import com.joprelys.backend.prescription.application.PrescriptionService;
import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class VisitClinicalClosureService {
    private final ConsultationRepository consultations;
    private final PrescriptionRepository prescriptions;
    private final UserAccountRepository users;
    private final PrescriptionService prescriptionService;
    private final MedicalSigningPolicy signingPolicy;
    private final AuditService auditService;
    public VisitClinicalClosureService(ConsultationRepository consultations, PrescriptionRepository prescriptions,
            UserAccountRepository users, PrescriptionService prescriptionService,
            MedicalSigningPolicy signingPolicy, AuditService auditService) {
        this.consultations = consultations; this.prescriptions = prescriptions; this.users = users;
        this.prescriptionService = prescriptionService; this.signingPolicy = signingPolicy; this.auditService = auditService;
    }
    // The caller holds the visit's pessimistic lock and transaction.
    public void signClinicalActs(VisitEntity visit) {
        consultations.findByVisitId(visit.getId()).ifPresent(consultation -> {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            UUID actorId = users.findByEmail(auth.getName().trim().toLowerCase())
                    .map(UserAccountEntity::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Médecin introuvable."));
            signingPolicy.requirePhysician(actorId);
            if (consultation.getSignedAt() == null) {
                consultation.seal(actorId, Instant.now(),
                        com.joprelys.backend.consultation.application.ClinicalContentHash.of(
                                consultation.getSymptoms(), consultation.getClinicalExam(), consultation.getDiagnosis(),
                                consultation.getConclusion(), consultation.getAdvice(), consultation.getFollowUp()));
                consultation.setStatus("VALIDEE");
                consultations.save(consultation);
                auditService.logSuccess(actorId, visit.getOrganizationId(), visit.getPatient().getId(),
                        "CONSULTATION", consultation.getId(), "CLINICAL_SIGN", consultation.getSignedContentHash());
            }
            prescriptions.findByConsultationId(consultation.getId()).ifPresent(prescription -> {
                if ("DRAFT".equals(prescription.getStatus())) {
                    boolean canSign = auth.getAuthorities().stream()
                            .anyMatch(authority -> "PRESCRIPTION_SIGN".equals(authority.getAuthority()));
                    if (!canSign) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Signature de prescription non autorisée.");
                    prescriptionService.finalizePrescription(prescription.getId(), actorId);
                }
            });
        });
    }
}
