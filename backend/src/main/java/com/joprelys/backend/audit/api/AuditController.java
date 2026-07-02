package com.joprelys.backend.audit.api;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

	private final AuditService auditService;
	private final UserAccountRepository userAccountRepository;
	private final PatientRepository patientRepository;

	public AuditController(AuditService auditService,
						   UserAccountRepository userAccountRepository,
						   PatientRepository patientRepository) {
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
		this.patientRepository = patientRepository;
	}

	@GetMapping("/patients/{patientId}")
	@PreAuthorize("hasAnyRole('AUDITEUR', 'ADMIN_CLINIQUE', 'MEDECIN')")
	public List<AuditLogResponse> getPatientLogs(@PathVariable UUID patientId, Authentication authentication) {
		var user = userAccountRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé."));

		// Enforce tenant isolation unless user is AUDITEUR
		if (!"AUDITEUR".equals(user.getRole())) {
			var patient = patientRepository.findById(patientId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient non trouvé."));

			if (user.getOrganizationId() == null || !user.getOrganizationId().equals(patient.getOrganizationId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : ce patient n'appartient pas à votre clinique.");
			}
		}

		// Log that patient logs are accessed
		auditService.logSuccess(
				user.getId(),
				user.getOrganizationId(),
				patientId,
				"AUDIT_LOG",
				patientId,
				"READ_AUDIT",
				"Consultation de l'historique d'audit du patient"
		);

		return auditService.getPatientLogs(patientId).stream()
				.map(entity -> {
					String actorName = entity.getActorUserId() != null 
							? userAccountRepository.findById(entity.getActorUserId())
									.map(UserAccountEntity::getDisplayName)
									.orElse("Utilisateur inconnu")
							: "Système";
					return AuditLogResponse.fromEntity(entity, actorName);
				})
				.toList();
	}

	@GetMapping("/organizations/{organizationId}")
	@PreAuthorize("hasAnyRole('AUDITEUR', 'ADMIN_CLINIQUE')")
	public List<AuditLogResponse> getOrganizationLogs(@PathVariable UUID organizationId, Authentication authentication) {
		var user = userAccountRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé."));

		// Enforce tenant isolation unless user is AUDITEUR
		if (!"AUDITEUR".equals(user.getRole())) {
			if (user.getOrganizationId() == null || !user.getOrganizationId().equals(organizationId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé : vous ne pouvez consulter que les logs de votre propre clinique.");
			}
		}

		return auditService.getOrganizationLogs(organizationId).stream()
				.map(entity -> {
					String actorName = entity.getActorUserId() != null 
							? userAccountRepository.findById(entity.getActorUserId())
									.map(UserAccountEntity::getDisplayName)
									.orElse("Utilisateur inconnu")
							: "Système";
					return AuditLogResponse.fromEntity(entity, actorName);
				})
				.toList();
	}
}
