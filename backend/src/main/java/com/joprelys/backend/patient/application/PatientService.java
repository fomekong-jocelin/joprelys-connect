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

	public PatientService(PatientRepository patientRepository,
						  PatientNumberGenerator patientNumberGenerator,
						  AuditService auditService,
						  UserAccountRepository userAccountRepository) {
		this.patientRepository = patientRepository;
		this.patientNumberGenerator = patientNumberGenerator;
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
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
		var patient = patientRepository.findById(id)
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

	private UserAccountEntity getCurrentUser() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
			return userAccountRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
		}
		return null;
	}
}
