package com.joprelys.backend.clinic.api;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationApiKeyEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationApiKeyRepository;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@PreAuthorize("hasAnyRole('ADMIN_JOPRELYS', 'SUPER_ADMIN')")
public class OrganizationController {

	private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int TEMPORARY_PASSWORD_LENGTH = 6;

	private final OrganizationRepository organizationRepository;
	private final UserAccountRepository userAccountRepository;
	private final OrganizationApiKeyRepository apiKeyRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	public OrganizationController(
			OrganizationRepository organizationRepository,
			UserAccountRepository userAccountRepository,
			OrganizationApiKeyRepository apiKeyRepository,
			PasswordEncoder passwordEncoder) {
		this.organizationRepository = organizationRepository;
		this.userAccountRepository = userAccountRepository;
		this.apiKeyRepository = apiKeyRepository;
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
				request.city(),
				request.country(),
				request.type(),
				request.responsibleName(),
				request.apiEnabled() != null ? request.apiEnabled() : true
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
		entity.setCountry(request.country());
		entity.setType(request.type());
		entity.setResponsibleName(request.responsibleName());
		entity.setApiEnabled(request.apiEnabled() != null ? request.apiEnabled() : true);
		entity.setLogoPath(request.logoPath());

		OrganizationEntity saved = organizationRepository.save(entity);
		return mapToResponse(saved);
	}

	@PostMapping("/{id}/api-keys")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiKeyResponse generateApiKey(
			@PathVariable UUID id,
			@Valid @RequestBody CreateApiKeyRequest request) {

		OrganizationEntity org = organizationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));

		String randomStr = generateRandomString(32);
		String rawKey = "jop_live_" + randomStr;
		String hashedKey = hashKey(rawKey);
		String prefix = "jop_live_" + randomStr.substring(0, 4) + "...";

		var keyEntity = new OrganizationApiKeyEntity(
				org.getId(),
				hashedKey,
				prefix,
				request.name().trim()
		);

		OrganizationApiKeyEntity saved = apiKeyRepository.save(keyEntity);

		return new ApiKeyResponse(
				saved.getId(),
				saved.getName(),
				saved.getPrefix(),
				rawKey,
				saved.getStatus(),
				saved.getCreatedAt(),
				saved.getRevokedAt()
		);
	}

	@GetMapping("/{id}/api-keys")
	public List<ApiKeyResponse> listApiKeys(@PathVariable UUID id) {
		OrganizationEntity org = organizationRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));

		return apiKeyRepository.findAllByOrganizationId(org.getId()).stream()
				.map(key -> new ApiKeyResponse(
						key.getId(),
						key.getName(),
						key.getPrefix(),
						null,
						key.getStatus(),
						key.getCreatedAt(),
						key.getRevokedAt()
				))
				.toList();
	}

	@DeleteMapping("/{orgId}/api-keys/{keyId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeApiKey(@PathVariable UUID orgId, @PathVariable UUID keyId) {
		organizationRepository.findById(orgId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation non trouvée."));

		OrganizationApiKeyEntity key = apiKeyRepository.findById(keyId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clé API non trouvée."));

		if (!key.getOrganizationId().equals(orgId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clé API n'appartient pas à cette organisation.");
		}

		key.setStatus("REVOKED");
		key.setRevokedAt(java.time.Instant.now());
		apiKeyRepository.save(key);
	}

	private OrganizationResponse mapToResponse(OrganizationEntity entity) {
		UserAccountEntity admin = userAccountRepository.findAllByOrganizationId(entity.getId()).stream()
				.filter(u -> u.hasRole("ADMIN_CLINIQUE"))
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
				adminDisplayName,
				entity.getCountry(),
				entity.getType(),
				entity.getResponsibleName(),
				entity.isApiEnabled()
		);
	}

	private String generateTemporaryPassword() {
		StringBuilder suffix = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
		for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
			suffix.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
		}
		return "Jop-" + suffix;
	}

	private String generateRandomString(int length) {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder sb = new StringBuilder(length);
		for (int index = 0; index < length; index++) {
			sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
		}
		return sb.toString();
	}

	private String hashKey(String rawKey) {
		try {
			var digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			var hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			throw new RuntimeException("Erreur lors du hachage de la clé API", e);
		}
	}
}
