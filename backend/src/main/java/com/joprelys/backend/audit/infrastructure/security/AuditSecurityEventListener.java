package com.joprelys.backend.audit.infrastructure.security;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuditSecurityEventListener {

	private final AuditService auditService;
	private final UserAccountRepository userAccountRepository;

	public AuditSecurityEventListener(AuditService auditService,
									  UserAccountRepository userAccountRepository) {
		this.auditService = auditService;
		this.userAccountRepository = userAccountRepository;
	}

	@EventListener
	public void onSuccess(AuthenticationSuccessEvent event) {
		String email = event.getAuthentication().getName();
		userAccountRepository.findByEmail(email).ifPresent(user -> {
			auditService.logSuccess(
					user.getId(),
					user.getOrganizationId(),
					null,
					"USER",
					user.getId(),
					"LOGIN",
					"Connexion réussie"
			);
		});
	}

	@EventListener
	public void onFailure(AbstractAuthenticationFailureEvent event) {
		String email = event.getAuthentication().getName();
		var userOpt = userAccountRepository.findByEmail(email);
		if (userOpt.isPresent()) {
			var user = userOpt.get();
			auditService.logDenied(
					user.getId(),
					user.getOrganizationId(),
					null,
					"USER",
					user.getId(),
					"LOGIN",
					"Échec de connexion : " + event.getException().getMessage()
			);
		} else {
			auditService.log(
					null,
					null,
					null,
					"USER",
					null,
					"LOGIN",
					"Échec de connexion (Email inconnu: " + email + ") : " + event.getException().getMessage(),
					null,
					null,
					"DENIED"
			);
		}
	}
}
