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

@Service
public class PatientService {

	private final PatientRepository patientRepository;
	private final PatientNumberGenerator patientNumberGenerator;
	private final AuditService auditService;
	private final UserAccountRepository userAccountRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository patientConsentRepository;
	private final com.joprelys.backend.patient.infrastructure.persistence.EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository;

	public PatientService(PatientRepository patientRepository,
						  PatientNumberGenerator patientNumberGenerator,
						  AuditService auditService,
						  UserAccountRepository userAccountRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.PatientConsentRepository patientConsentRepository,
						  com.joprelys.backend.patient.infrastructure.persistence.EmergencyAccessAuthorizationRepository emergencyAccessAuthorizationRepository) {
		this.patientRepository = patientRepository;
		this.patientNumberGenerator = patientNumberGenerator;
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
		this.patientConsentRepository = patientConsentRepository;
		this.emergencyAccessAuthorizationRepository = emergencyAccessAuthorizationRepository;
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

		var saved = patientRepository.save(patient);

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
		if (query == null || query.isBlank()) {
			return patientRepository.findAll();
		}
		return patientRepository.searchPatients(query.trim());
	}

	@Transactional(readOnly = true)
	public PatientEntity getPatientById(UUID id) {
		var patient = patientRepository.findByIdGlobally(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé"));

		var actor = getCurrentUser();
		if (actor != null) {
			String role = actor.getRole();
			boolean isClinicalRole = "MEDECIN".equals(role) || "INFIRMIER".equals(role) || "AGENT_ACCUEIL".equals(role) || "ADMIN_CLINIQUE".equals(role);
			if (isClinicalRole) {
				UUID organizationId = actor.getOrganizationId();
				boolean hasConsent = checkConsent(patient.getId(), organizationId);
				if (!hasConsent) {
					boolean hasEmergencyAccess = checkEmergencyAccess(patient.getId(), organizationId);
					if (!hasEmergencyAccess) {
						throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CONSENT_REQUIRED");
					}
				}
			}

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

	private boolean checkConsent(UUID patientId, UUID organizationId) {
		var consent = patientConsentRepository.findByPatientIdAndOrganizationId(patientId, organizationId);
		if (consent.isPresent()) {
			return "ACTIVE".equals(consent.get().getStatus());
		}
		var patient = patientRepository.findByIdGlobally(patientId).orElse(null);
		return patient != null && patient.getOrganizationId() != null && patient.getOrganizationId().equals(organizationId);
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
				"EMERGENCY_ACCESS",
				"Accès d'urgence Brise-Glace activé. Motif : " + reason,
				null, null, "SUCCESS"
		);
	}

	@Transactional(readOnly = true)
	public boolean isEmergencyAccessActiveForCurrentActor(UUID patientId) {
		var actor = getCurrentUser();
		if (actor == null) return false;
		return checkEmergencyAccess(patientId, actor.getOrganizationId());
	}

	private UserAccountEntity getCurrentUser() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
			return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
		}
		return null;
	}
}
