package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.InviteStaffRequest;
import com.joprelys.backend.auth.api.InviteStaffResponse;
import com.joprelys.backend.auth.api.StaffResponse;
import com.joprelys.backend.auth.api.UpdateStaffRequest;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffService {

	private static final Set<String> MANAGEABLE_ROLES = Set.of(
			"MEDECIN",
			"INFIRMIER",
			"AGENT_ACCUEIL",
			"PHARMACIEN",
			"BIOLOGISTE");
	private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int TEMPORARY_PASSWORD_LENGTH = 6;

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom;

	public StaffService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.secureRandom = new SecureRandom();
	}

	@Transactional(readOnly = true)
	public List<StaffResponse> listStaff(Authentication authentication) {
		UserAccountEntity admin = currentAdmin(authentication);
		return userAccountRepository
				.findAllByOrganizationIdAndIdNotOrderByDisplayNameAsc(admin.getOrganizationId(), admin.getId())
				.stream()
				.filter(account -> java.util.Arrays.stream(account.getRole().split(","))
						.map(String::trim)
						.anyMatch(MANAGEABLE_ROLES::contains))
				.sorted(Comparator.comparing(UserAccountEntity::getDisplayName))
				.map(StaffService::toStaffResponse)
				.toList();
	}

	@Transactional
	public InviteStaffResponse inviteStaff(InviteStaffRequest request, Authentication authentication) {
		UserAccountEntity admin = currentAdmin(authentication);
		String email = normalizeEmail(request.email());
		String role = normalizeRole(request.role());
		assertManageableRole(role);

		if (userAccountRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un utilisateur avec cet email existe déjà.");
		}

		String temporaryPassword = generateTemporaryPassword();
		UserAccountEntity staff = new UserAccountEntity(
				email,
				request.displayName().trim(),
				role,
				passwordEncoder.encode(temporaryPassword));
		staff.setOrganizationId(admin.getOrganizationId());

		UserAccountEntity saved = userAccountRepository.save(staff);
		return new InviteStaffResponse(
				saved.getId(),
				saved.getEmail(),
				saved.getDisplayName(),
				saved.getRole(),
				saved.isEnabled(),
				temporaryPassword,
				saved.getCreatedAt());
	}

	@Transactional
	public StaffResponse updateStaff(UUID staffId, UpdateStaffRequest request, Authentication authentication) {
		UserAccountEntity staff = managedStaff(staffId, currentAdmin(authentication));
		String role = normalizeRole(request.role());
		assertManageableRole(role);

		staff.setDisplayName(request.displayName().trim());
		staff.setRole(role);
		staff.setPhotoPath(request.photoPath());
		staff.setSignaturePath(request.signaturePath());
		staff.setStampPath(request.stampPath());
		staff.setPhone(request.phone());
		staff.setSpecialty(request.specialty());
		staff.setRegistrationNumber(request.registrationNumber());
		staff.setDepartment(request.department());
		staff.setBio(request.bio());
		return toStaffResponse(userAccountRepository.save(staff));
	}

	@Transactional
	public StaffResponse toggleStatus(UUID staffId, Authentication authentication) {
		UserAccountEntity staff = managedStaff(staffId, currentAdmin(authentication));
		staff.setEnabled(!staff.isEnabled());
		return toStaffResponse(userAccountRepository.save(staff));
	}

	private UserAccountEntity managedStaff(UUID staffId, UserAccountEntity admin) {
		if (admin.getId().equals(staffId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un administrateur ne peut pas se gérer lui-même.");
		}
		UserAccountEntity staff = userAccountRepository.findByIdAndOrganizationId(staffId, admin.getOrganizationId())
				.filter(account -> java.util.Arrays.stream(account.getRole().split(","))
						.map(String::trim)
						.anyMatch(MANAGEABLE_ROLES::contains))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborateur non trouvé."));
		return staff;
	}

	private UserAccountEntity currentAdmin(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
		}
		UserAccountEntity admin = userAccountRepository.findByEmail(normalizeEmail(authentication.getName()))
				.filter(UserAccountEntity::isEnabled)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
		if (admin.getOrganizationId() == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrateur non rattaché à une clinique.");
		}
		return admin;
	}

	private void assertManageableRole(String role) {
		for (String r : role.split(",")) {
			if (!MANAGEABLE_ROLES.contains(r.trim())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle clinique invalide : " + r.trim());
			}
		}
	}

	private String generateTemporaryPassword() {
		StringBuilder suffix = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
		for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
			suffix.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
		}
		return "Jop-" + suffix;
	}

	private static StaffResponse toStaffResponse(UserAccountEntity entity) {
		return new StaffResponse(
				entity.getId(),
				entity.getEmail(),
				entity.getDisplayName(),
				entity.getRole(),
				entity.isEnabled(),
				entity.getCreatedAt(),
				entity.getPhotoPath(),
				entity.getSignaturePath(),
				entity.getStampPath(),
				entity.getPhone(),
				entity.getSpecialty(),
				entity.getRegistrationNumber(),
				entity.getDepartment(),
				entity.getBio());
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeRole(String role) {
		return role.trim().toUpperCase(Locale.ROOT);
	}
}
