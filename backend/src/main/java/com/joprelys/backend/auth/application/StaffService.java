package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.InviteStaffRequest;
import com.joprelys.backend.auth.api.InviteStaffResponse;
import com.joprelys.backend.auth.api.StaffResponse;
import com.joprelys.backend.auth.api.UpdateStaffRequest;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.rbac.RbacStore;
import com.joprelys.backend.notification.application.AccountMailService;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final Set<String> FORBIDDEN_STAFF_ROLE_CODES = Set.of(
            "SUPER_ADMIN",
            "ADMIN_JOPRELYS",
            "ADMIN_CLINIQUE",
            "PATIENT");
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TEMPORARY_PASSWORD_LENGTH = 6;

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RbacStore rbacStore;
    private final SecureRandom secureRandom;
    private final AccountMailService accountMailService;

    public StaffService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            RbacStore rbacStore,
            AccountMailService accountMailService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.rbacStore = rbacStore;
        this.accountMailService = accountMailService;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public List<StaffResponse> listStaff(Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        Set<String> manageableRoleCodes = manageableRoles(admin.getOrganizationId()).stream()
                .map(RbacStore.RoleView::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return userAccountRepository
                .findAllByOrganizationIdAndIdNotOrderByDisplayNameAsc(admin.getOrganizationId(), admin.getId())
                .stream()
                .map(account -> {
                    rbacStore.synchronizeLegacyAssignments(account);
                    RbacStore.EffectiveAccess access = rbacStore.loadEffectiveAccess(
                            account.getId(), account.getOrganizationId());
                    return new StaffWithAccess(account, access.roles());
                })
                .filter(item -> item.roles().stream().anyMatch(manageableRoleCodes::contains))
                .sorted(Comparator.comparing(item -> item.account().getDisplayName()))
                .map(item -> toStaffResponse(item.account(), item.roles()))
                .toList();
    }

    @Transactional
    public InviteStaffResponse inviteStaff(InviteStaffRequest request, Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        String email = normalizeEmail(request.email());
        List<RbacStore.RoleView> roles = resolveManageableRoles(
                admin.getOrganizationId(), request.role(), request.roles());
        String legacyRoles = joinRoleCodes(roles);

        if (userAccountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un utilisateur avec cet email existe déjà.");
        }

        String temporaryPassword = generateTemporaryPassword();
        UserAccountEntity staff = new UserAccountEntity(
                email,
                request.displayName().trim(),
                legacyRoles,
                passwordEncoder.encode(temporaryPassword));
        staff.setOrganizationId(admin.getOrganizationId());

        UserAccountEntity saved = userAccountRepository.saveAndFlush(staff);
        rbacStore.replaceUserRoles(saved.getId(), admin.getOrganizationId(), admin.getId(), roles);
        rbacStore.audit(
                admin.getOrganizationId(),
                admin.getId(),
                "STAFF_CREATED",
                "USER",
                saved.getId().toString(),
                "roles=" + legacyRoles);
        accountMailService.sendTemporaryPassword(saved.getEmail(), saved.getDisplayName(), temporaryPassword);

        return new InviteStaffResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getDisplayName(),
                legacyRoles,
                saved.isEnabled(),
                saved.getCreatedAt());
    }

    @Transactional
    public StaffResponse updateStaff(UUID staffId, UpdateStaffRequest request, Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        List<RbacStore.RoleView> roles = resolveManageableRoles(
                admin.getOrganizationId(), request.role(), request.roles());
        String legacyRoles = joinRoleCodes(roles);

        staff.setDisplayName(request.displayName().trim());
        staff.setRole(legacyRoles);
        staff.setPhotoPath(request.photoPath());
        staff.setSignaturePath(request.signaturePath());
        staff.setStampPath(request.stampPath());
        staff.setPhone(request.phone());
        staff.setRegistrationNumber(request.registrationNumber());
        staff.setBio(request.bio());
        UserAccountEntity saved = userAccountRepository.saveAndFlush(staff);

        rbacStore.replaceUserRoles(saved.getId(), admin.getOrganizationId(), admin.getId(), roles);
        rbacStore.audit(
                admin.getOrganizationId(),
                admin.getId(),
                "STAFF_ROLES_UPDATED",
                "USER",
                saved.getId().toString(),
                "roles=" + legacyRoles);
        return toStaffResponse(saved, roles.stream()
                .map(RbacStore.RoleView::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    @Transactional
    public StaffResponse toggleStatus(UUID staffId, Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        staff.setEnabled(!staff.isEnabled());
        UserAccountEntity saved = userAccountRepository.save(staff);
        RbacStore.EffectiveAccess access = rbacStore.loadEffectiveAccess(saved.getId(), saved.getOrganizationId());
        rbacStore.audit(
                admin.getOrganizationId(),
                admin.getId(),
                saved.isEnabled() ? "STAFF_ENABLED" : "STAFF_DISABLED",
                "USER",
                saved.getId().toString(),
                null);
        return toStaffResponse(saved, access.roles());
    }

    private UserAccountEntity managedStaff(UUID staffId, UserAccountEntity admin) {
        if (admin.getId().equals(staffId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un administrateur ne peut pas se gérer lui-même.");
        }

        UserAccountEntity staff = userAccountRepository.findByIdAndOrganizationId(staffId, admin.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborateur non trouvé."));
        rbacStore.synchronizeLegacyAssignments(staff);
        Set<String> effectiveRoles = rbacStore.loadEffectiveAccess(staff.getId(), staff.getOrganizationId()).roles();
        Set<String> manageableCodes = manageableRoles(admin.getOrganizationId()).stream()
                .map(RbacStore.RoleView::code)
                .collect(java.util.stream.Collectors.toSet());
        if (effectiveRoles.stream().noneMatch(manageableCodes::contains)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborateur non trouvé.");
        }
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

    private List<RbacStore.RoleView> resolveManageableRoles(
            UUID organizationId,
            String legacyRole,
            List<String> requestedRoles) {
        LinkedHashSet<String> requestedCodes = new LinkedHashSet<>();
        if (requestedRoles != null) {
            requestedRoles.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(StaffService::normalizeRoleCode)
                    .forEach(requestedCodes::add);
        }
        if (requestedCodes.isEmpty() && legacyRole != null && !legacyRole.isBlank()) {
            Arrays.stream(legacyRole.split(","))
                    .filter(value -> !value.isBlank())
                    .map(StaffService::normalizeRoleCode)
                    .forEach(requestedCodes::add);
        }
        if (requestedCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez au moins un rôle.");
        }

        Map<String, RbacStore.RoleView> allowedByCode = manageableRoles(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        RbacStore.RoleView::code,
                        role -> role,
                        (left, right) -> left,
                        LinkedHashMap::new));
        List<String> invalidCodes = requestedCodes.stream()
                .filter(code -> !allowedByCode.containsKey(code))
                .toList();
        if (!invalidCodes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rôle non attribuable dans cet établissement : " + String.join(", ", invalidCodes));
        }
        return requestedCodes.stream().map(allowedByCode::get).toList();
    }

    private List<RbacStore.RoleView> manageableRoles(UUID organizationId) {
        return rbacStore.listVisibleRoles(organizationId).stream()
                .filter(RbacStore.RoleView::assignable)
                .filter(RbacStore.RoleView::enabled)
                .filter(role -> !FORBIDDEN_STAFF_ROLE_CODES.contains(role.code()))
                .toList();
    }

    private String generateTemporaryPassword() {
        StringBuilder suffix = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int index = 0; index < TEMPORARY_PASSWORD_LENGTH; index++) {
            suffix.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return "Jop-" + suffix;
    }

    private static StaffResponse toStaffResponse(UserAccountEntity entity, Set<String> roles) {
        String roleCodes = String.join(",", roles);
        return new StaffResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                roleCodes,
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getPhotoPath(),
                entity.getSignaturePath(),
                entity.getStampPath(),
                entity.getPhone(),
                entity.getRegistrationNumber(),
                entity.getBio());
    }

    private static String joinRoleCodes(List<RbacStore.RoleView> roles) {
        return roles.stream().map(RbacStore.RoleView::code).collect(java.util.stream.Collectors.joining(","));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRoleCode(String role) {
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private record StaffWithAccess(UserAccountEntity account, Set<String> roles) {
    }
}
