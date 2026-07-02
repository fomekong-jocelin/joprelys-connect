package com.joprelys.backend.clinic.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/organizations")
@PreAuthorize("hasRole('ADMIN_JOPRELYS')")
public class OrganizationController {

	private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int TEMPORARY_PASSWORD_LENGTH = 6;

	private final OrganizationRepository organizationRepository;
	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	public OrganizationController(
			OrganizationRepository organizationRepository,
			UserAccountRepository userAccountRepository,
			PasswordEncoder passwordEncoder) {
		this.organizationRepository = organizationRepository;
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
		if (organizationRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "L'organisation avec cet email existe déjà.");
		}

		var entity = new OrganizationEntity(
				request.name(),
				request.email(),
				request.phone(),
				request.address(),
				request.city()
		);
		var saved = organizationRepository.save(entity);
		return mapToResponse(saved);
	}

	@GetMapping
	public List<OrganizationResponse> list() {
		return organizationRepository.findAll().stream()
				.map(this::mapToResponse)
				.toList();
	}

	@PutMapping("/{id}/status")
	public OrganizationResponse updateStatus(@PathVariable UUID id, @RequestBody String status) {
		String cleanStatus = status.replace("\"", "").trim().toUpperCase();
		if (!cleanStatus.equals("ACTIVE") && !cleanStatus.equals("INACTIVE")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide.");
		}

		var entity = organizationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));
		
		entity.setStatus(cleanStatus);
		var saved = organizationRepository.save(entity);
		return mapToResponse(saved);
	}

	@PostMapping("/{id}/admin")
	@ResponseStatus(HttpStatus.CREATED)
	public CreateClinicAdminResponse createClinicAdmin(
			@PathVariable UUID id,
			@Valid @RequestBody CreateClinicAdminRequest request) {

		OrganizationEntity org = organizationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));

		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (userAccountRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet e-mail existe déjà.");
		}

		String temporaryPassword = generateTemporaryPassword();
		UserAccountEntity admin = new UserAccountEntity(
				email,
				request.displayName().trim(),
				"ADMIN_CLINIQUE",
				passwordEncoder.encode(temporaryPassword));
		admin.setOrganizationId(org.getId());
		UserAccountEntity saved = userAccountRepository.save(admin);

		return new CreateClinicAdminResponse(
				saved.getId(),
				saved.getEmail(),
				saved.getDisplayName(),
				saved.getRole(),
				saved.isEnabled(),
				temporaryPassword,
				org.getId(),
				saved.getCreatedAt());
	}

	@PutMapping("/{id}")
	public OrganizationResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateOrganizationRequest request) {

		OrganizationEntity entity = organizationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));

		if (!entity.getEmail().equalsIgnoreCase(request.email()) && organizationRepository.existsByEmail(request.email())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "L'organisation avec cet email existe déjà.");
		}

		entity.setName(request.name());
		entity.setEmail(request.email());
		entity.setPhone(request.phone());
		entity.setAddress(request.address());
		entity.setCity(request.city());

		OrganizationEntity saved = organizationRepository.save(entity);
		return mapToResponse(saved);
	}

	private OrganizationResponse mapToResponse(OrganizationEntity entity) {
		UserAccountEntity admin = userAccountRepository.findAllByOrganizationId(entity.getId()).stream()
				.filter(u -> "ADMIN_CLINIQUE".equals(u.getRole()))
				.findFirst()
				.orElse(null);
		String adminEmail = admin != null ? admin.getEmail() : null;
		String adminDisplayName = admin != null ? admin.getDisplayName() : null;

		return new OrganizationResponse(
				entity.getId(),
				entity.getName(),
				entity.getEmail(),
				entity.getPhone(),
				entity.getAddress(),
				entity.getCity(),
				entity.getLogoPath(),
				entity.getStatus(),
				entity.getCreatedAt(),
				adminEmail,
				adminDisplayName
		);
	}

	private String generateTemporaryPassword() {
		StringBuilder suffix = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
		for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
			suffix.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
		}
		return "Jop-" + suffix;
	}
}
