package com.joprelys.backend.patient.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.api.CreatePatientRequest;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class PatientService {

	private final PatientRepository patientRepository;
	private final PatientAccessPolicyService accessPolicy;
	private final PatientNumberGenerator patientNumberGenerator;
	private final AuditService auditService;
	private final UserAccountRepository userAccountRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository patientConsentRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository externalAccessRequestRepository;
	private final com.joprelys.backend.notification.application.NotificationService notificationService;

	// WT2 (PDF) dependencies
	private final com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository;
	private final com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository vitalsRepository;
	private final com.joprelys.backend.visit.application.PdfGeneratorService pdfGeneratorService;
	private final com.joprelys.backend.visit.application.QrCodeGeneratorService qrCodeGeneratorService;

	// WT3 (DUPLICATES) dependencies
	private final PatientSimilarityService patientSimilarityService;
	private final com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateRepository duplicateCandidateRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.PatientMergedHistoryRepository mergedHistoryRepository;
	private final com.joprelys.backend.visit.infrastructure.persistence.VisitRepository visitRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyRepository patientAllergyRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryRepository patientMedicalHistoryRepository;
	private final com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository labOrderRepository;
	private final com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository labResultRepository;
	private final com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository hospitalizationRepository;
	private final com.joprelys.backend.notification.infrastructure.persistence.NotificationRepository notificationRepository;
	private final com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository auditLogRepository;


	public PatientService(PatientRepository patientRepository,
						  PatientAccessPolicyService accessPolicy,
						  PatientNumberGenerator patientNumberGenerator,
						  AuditService auditService,
						  UserAccountRepository userAccountRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository patientConsentRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.ExternalAccessRequestRepository externalAccessRequestRepository,
						  com.joprelys.backend.notification.application.NotificationService notificationService,
						  com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository organizationRepository,
						  com.joprelys.backend.visit.infrastructure.persistence.VitalsRepository vitalsRepository,
						  com.joprelys.backend.visit.application.PdfGeneratorService pdfGeneratorService,
						  com.joprelys.backend.visit.application.QrCodeGeneratorService qrCodeGeneratorService,
						  PatientSimilarityService patientSimilarityService,
						  com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateRepository duplicateCandidateRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.PatientMergedHistoryRepository mergedHistoryRepository,
						  com.joprelys.backend.visit.infrastructure.persistence.VisitRepository visitRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyRepository patientAllergyRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryRepository patientMedicalHistoryRepository,
						  com.joprelys.backend.lab.infrastructure.persistence.LabOrderRepository labOrderRepository,
						  com.joprelys.backend.lab.infrastructure.persistence.LabResultRepository labResultRepository,
						  com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationRepository hospitalizationRepository,
						  com.joprelys.backend.notification.infrastructure.persistence.NotificationRepository notificationRepository,
						  com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository auditLogRepository) {
		this.patientRepository = patientRepository;
		this.accessPolicy = accessPolicy;
		this.patientNumberGenerator = patientNumberGenerator;
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
		this.patientConsentRepository = patientConsentRepository;
		this.emergencyAccessAuthorizationRepository = emergencyAccessAuthorizationRepository;
		this.externalAccessRequestRepository = externalAccessRequestRepository;
		this.notificationService = notificationService;
		this.organizationRepository = organizationRepository;
		this.vitalsRepository = vitalsRepository;
		this.pdfGeneratorService = pdfGeneratorService;
		this.qrCodeGeneratorService = qrCodeGeneratorService;
		this.patientSimilarityService = patientSimilarityService;
		this.duplicateCandidateRepository = duplicateCandidateRepository;
		this.mergedHistoryRepository = mergedHistoryRepository;
		this.visitRepository = visitRepository;
		this.patientAllergyRepository = patientAllergyRepository;
		this.patientMedicalHistoryRepository = patientMedicalHistoryRepository;
		this.labOrderRepository = labOrderRepository;
		this.labResultRepository = labResultRepository;
		this.hospitalizationRepository = hospitalizationRepository;
		this.notificationRepository = notificationRepository;
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public PatientEntity createPatient(CreatePatientRequest request) {
		PatientNumberGenerator.GeneratedNumbers numbers = patientNumberGenerator.generateNextNumbers();

		var patient = new PatientEntity(
				numbers.globalNumber(),
				numbers.localNumber(),
				request.fullName(),
				request.gender(),
				request.birthDate(),
				request.phone(),
				request.city(),
				request.district(),
				request.address(),
				request.emergencyContactName(),
				request.emergencyContactPhone(),
				request.allergies(),
				request.medicalHistory()
		);
		patient.setBloodGroup(request.bloodGroup());
		patient.setEmail(request.email());

		var saved = patientRepository.save(patient);

		if (request.allergies() != null && !request.allergies().isBlank()) {
			var allergy = new com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyEntity(
					saved.getId(),
					request.allergies().trim(),
					"MEDIUM",
					null,
					"ACTIVE",
					null,
					"Généré à la création"
			);
			patientAllergyRepository.save(allergy);
		}
		if (request.medicalHistory() != null && !request.medicalHistory().isBlank()) {
			var history = new com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryEntity(
					saved.getId(),
					"MEDICAL",
					request.medicalHistory().trim(),
					null,
					true,
					"Généré à la création",
					false
			);
			patientMedicalHistoryRepository.save(history);
		}

		// WT3: Trigger automatic duplicate check
		patientSimilarityService.checkForDuplicates(saved);

		var actor = getCurrentUser();
		if (actor != null) {
			auditService.logSuccess(
					actor.getId(),
					actor.getOrganizationId(),
					saved.getId(),
					"PATIENT",
					saved.getId(),
					"CREATE_PATIENT",
					"Création de la fiche d'identité du patient : " + saved.getFullName()
			);
		}

		return saved;
	}

	@Transactional(readOnly = true)
	public List<PatientEntity> searchPatients(String query) {
		var actor = getCurrentUser();
		if (actor == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
		}
		UUID organizationId = actor.getOrganizationId();

		if (query == null || query.isBlank()) {
			return patientRepository.findAll();
		}

		String cleanQuery = query.trim();
		List<PatientEntity> results = new java.util.ArrayList<>(patientRepository.searchPatients(cleanQuery));

		List<PatientEntity> globalPatients = patientRepository.searchPatientsGlobally(cleanQuery);
		for (PatientEntity p : globalPatients) {
			boolean alreadyInResults = results.stream().anyMatch(local -> local.getId().equals(p.getId()));
			if (!alreadyInResults) {
				if (checkConsent(p.getId(), organizationId) || checkEmergencyAccess(p.getId(), organizationId)) {
					results.add(p);
				}
			}
		}

		return results;
	}

	@Transactional(readOnly = true)
	public PatientEntity getPatientById(UUID id) {
		accessPolicy.validateAccess(id, "medical_records");
		var patient = patientRepository.findByIdGlobally(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));

		var actor = getCurrentUser();
		if (actor != null) {
			auditService.logSuccess(
					actor.getId(),
					actor.getOrganizationId(),
					patient.getId(),
					"PATIENT_RECORD",
					patient.getId(),
					"CONSULTATION",
					"Accès à la fiche d'identité du patient : " + patient.getFullName()
			);
		}

		return patient;
	}

	/**
	 * Legacy entry point kept for callers that have not yet migrated. It delegates to
	 * the single access-policy truth and can no longer implement its own scope logic.
	 */
	public void validateAccess(UUID patientId, String requiredScope) {
		accessPolicy.validateAccess(patientId, requiredScope);
	}

	/**
	 * Historical sub-resource entry point. Access failures are intentionally no longer
	 * rewritten as 404: callers receive the explicit 403 remediation from the policy.
	 */
	public void validateAccessForSubResource(UUID patientId, String requiredScope, String notFoundMessage) {
		accessPolicy.validateAccess(patientId, requiredScope);
	}

	private boolean checkConsent(UUID patientId, UUID organizationId) {
		var consent = patientConsentRepository.findByPatientIdAndOrganizationId(patientId, organizationId);
		if (consent.isPresent()) {
			var c = consent.get();
			boolean isActive = "ACTIVE".equals(c.getStatus()) || "APPROVED".equals(c.getStatus());
			boolean notExpired = c.getExpiresAt() == null || c.getExpiresAt().isAfter(java.time.Instant.now());
			return isActive && notExpired;
		}
		var patient = patientRepository.findByIdGlobally(patientId).orElse(null);
		if (patient != null && patient.getOrganizationId() != null && patient.getOrganizationId().equals(organizationId)) {
			return true;
		}
		return checkExternalAccess(patientId, organizationId);
	}

	private boolean checkExternalAccess(UUID patientId, UUID organizationId) {
		return externalAccessRequestRepository.findByPatientId(patientId).stream()
				.anyMatch(r -> organizationId.equals(r.getRequesterOrganizationId()) &&
						"APPROUVEE".equals(r.getStatus()) &&
						r.getExpiresAt() != null &&
						r.getExpiresAt().isAfter(java.time.Instant.now()));
	}

	private boolean checkEmergencyAccess(UUID patientId, UUID organizationId) {
		return emergencyAccessAuthorizationRepository
				.findByPatientIdAndOrganizationIdAndExpiresAtAfter(patientId, organizationId, java.time.Instant.now())
				.isPresent();
	}

	@Transactional
	public void triggerEmergencyAccess(UUID patientId, String reason) {
		var actor = getCurrentUser();
		if (actor == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié.");
		}

		var patient = patientRepository.findByIdGlobally(patientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));

		UUID organizationId = actor.getOrganizationId();
		String doctorEmail = actor.getEmail();

		var auth = new com.joprelys.backend.patient.infrastructure.persistence.EmergencyAccessAuthorizationEntity(
				patientId,
				organizationId,
				doctorEmail,
				reason,
				java.time.Instant.now().plus(java.time.Duration.ofMinutes(15))
		);
		emergencyAccessAuthorizationRepository.save(auth);

		auditService.log(
				actor.getId(),
				organizationId,
				patientId,
				"PATIENT_RECORD",
				patientId,
				"EMERGENCY_DPU_ACCESS",
				"Accès d'urgence Brise-Glace activé. Motif : " + reason,
				null, null, "SUCCESS"
		);

		notificationService.sendNotification(
				patientId,
				"Alerte de sécurité : Accès d'urgence",
				"Le praticien " + doctorEmail + " a accédé à votre dossier en mode d'urgence Brise-Glace pour le motif : " + reason,
				"SECURITY"
		);
	}

	@Transactional(readOnly = true)
	public boolean isEmergencyAccessActiveForCurrentActor(UUID patientId) {
		var actor = getCurrentUser();
		if (actor == null) return false;
		return checkEmergencyAccess(patientId, actor.getOrganizationId());
	}


	// WT3 (DUPLICATES): Récupération des candidats doublons en attente
	@Transactional(readOnly = true)
	public List<com.joprelys.backend.patient.infrastructure.persistence.PatientDuplicateCandidateEntity> getDuplicateCandidates() {
		return duplicateCandidateRepository.findAllPending();
	}

	// WT3 (DUPLICATES): Ignorer un candidat doublon
	@Transactional
	public void ignoreDuplicateCandidate(UUID candidateId) {
		var candidate = duplicateCandidateRepository.findById(candidateId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat doublon non trouvé"));
		candidate.setStatus("IGNORED");
		duplicateCandidateRepository.save(candidate);
	}

	// WT3 (DUPLICATES): Fusion de dossiers patients doublons
	@Transactional
	public void mergePatients(UUID primaryId, UUID secondaryId, UUID actorId) {
		if (primaryId.equals(secondaryId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de fusionner un patient avec lui-même");
		}

		PatientEntity primary = patientRepository.findByIdGlobally(primaryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient principal non trouvé"));

		PatientEntity secondary = patientRepository.findByIdGlobally(secondaryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient secondaire non trouvé"));

		if (!"ACTIVE".equals(primary.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le patient principal doit être actif");
		}
		if (!"ACTIVE".equals(secondary.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le patient secondaire doit être actif");
		}

		var visits = visitRepository.findByPatientId(secondaryId);
		for (var v : visits) {
			v.setPatient(primary);
			visitRepository.save(v);
		}

		var consents = patientConsentRepository.findByPatientId(secondaryId);
		for (var c : consents) {
			c.setPatientId(primaryId);
			patientConsentRepository.save(c);
		}

		var emergencyAccesses = emergencyAccessAuthorizationRepository.findByPatientId(secondaryId);
		for (var ea : emergencyAccesses) {
			ea.setPatientId(primaryId);
			emergencyAccessAuthorizationRepository.save(ea);
		}

		var externalRequests = externalAccessRequestRepository.findByPatientId(secondaryId);
		for (var er : externalRequests) {
			er.setPatientId(primaryId);
			externalAccessRequestRepository.save(er);
		}

		var allergies = patientAllergyRepository.findAllByPatientId(secondaryId);
		for (var al : allergies) {
			al.setPatientId(primaryId);
			patientAllergyRepository.save(al);
		}

		var histories = patientMedicalHistoryRepository.findAllByPatientId(secondaryId);
		for (var h : histories) {
			h.setPatientId(primaryId);
			patientMedicalHistoryRepository.save(h);
		}

		var labOrders = labOrderRepository.findByPatientIdOrderByCreatedAtDesc(secondaryId);
		for (var lo : labOrders) {
			lo.setPatient(primary);
			labOrderRepository.save(lo);
		}

		var labResults = labResultRepository.findByPatientIdOrderByCreatedAtDesc(secondaryId);
		for (var lr : labResults) {
			lr.setPatient(primary);
			labResultRepository.save(lr);
		}

		var hospitalizations = hospitalizationRepository.findByPatientIdOrderByAdmittedAtDesc(secondaryId);
		for (var hosp : hospitalizations) {
			hosp.setPatientId(primaryId);
			hospitalizationRepository.save(hosp);
		}

		var notifs = notificationRepository.findByPatientIdOrderByCreatedAtDesc(secondaryId);
		for (var n : notifs) {
			n.setPatientId(primaryId);
			notificationRepository.save(n);
		}

		var audits = auditLogRepository.findByPatientIdOrderByCreatedAtDesc(secondaryId);
		for (var a : audits) {
			a.setPatientId(primaryId);
			auditLogRepository.save(a);
		}

		var candidate = duplicateCandidateRepository.findByPatientPair(primaryId, secondaryId).orElse(null);
		if (candidate != null) {
			candidate.setStatus("RESOLVED");
			duplicateCandidateRepository.save(candidate);
		}

		secondary.setStatus("MERGED");
		patientRepository.save(secondary);

		var mergedHistory = new com.joprelys.backend.patient.infrastructure.persistence.PatientMergedHistoryEntity(
				primary,
				secondary.getId(),
				secondary.getGlobalPatientNumber(),
				actorId
		);
		mergedHistoryRepository.save(mergedHistory);

		auditService.logSuccess(
				actorId,
				primary.getOrganizationId(),
				primary.getId(),
				"PATIENT_RECORD",
				primary.getId(),
				"MERGE_PATIENTS",
				"Fusion du dossier patient " + secondary.getFullName() + " (DPU: " + secondary.getGlobalPatientNumber() + ") vers " + primary.getFullName() + " (DPU: " + primary.getGlobalPatientNumber() + ")"
		);
	}

	private UserAccountEntity getCurrentUser() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
			return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
		}
		return null;
	}

	public static UUID convertToUuid(Object obj) {
		if (obj == null) return null;
		if (obj instanceof UUID) return (UUID) obj;
		if (obj instanceof byte[]) {
			byte[] bytes = (byte[]) obj;
			if (bytes.length == 16) {
				java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(bytes);
				long high = bb.getLong();
				long low = bb.getLong();
				return new UUID(high, low);
			}
		}
		if (obj instanceof String) {
			return UUID.fromString((String) obj);
		}
		throw new IllegalArgumentException("Cannot convert object of type " + obj.getClass().getName() + " to UUID");
	}
}
