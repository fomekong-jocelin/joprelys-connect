package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.application.StaffProfileAssignmentService;
import com.joprelys.backend.auth.api.StaffProfileAssignmentDtos.ActiveStructureResponse;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("hasAuthority('STAFF_PROFILE_ACCESS')")
public class ProfileController {

    private final UserAccountRepository userAccountRepository;
    private final StaffProfileAssignmentService profileAssignmentService;

    public ProfileController(
            UserAccountRepository userAccountRepository,
            StaffProfileAssignmentService profileAssignmentService) {
        this.userAccountRepository = userAccountRepository;
        this.profileAssignmentService = profileAssignmentService;
    }

    @GetMapping
    public StaffResponse getProfile(Authentication authentication) {
        UserAccountEntity user = currentUser(authentication);
        return toStaffResponse(user);
    }

    @GetMapping("/assignments")
    public ActiveStructureResponse getActiveAssignments(Authentication authentication) {
        return profileAssignmentService.getOwnActiveStructure(authentication);
    }

    @PutMapping
    public StaffResponse updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        UserAccountEntity user = currentUser(authentication);

        user.setDisplayName(request.displayName().trim());
        user.setPhone(request.phone());
        user.setPhotoPath(request.photoPath());
        user.setBio(request.bio());

        if (user.hasRole("MEDECIN")) {
            user.setSignaturePath(request.signaturePath());
            user.setStampPath(request.stampPath());
            user.setRegistrationNumber(request.registrationNumber());
        } else if ((request.signaturePath() != null && !request.signaturePath().isBlank())
                || (request.stampPath() != null && !request.stampPath().isBlank())
                || (request.registrationNumber() != null && !request.registrationNumber().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seuls les médecins peuvent configurer leur signature, cachet ou numéro d'ordre.");
        }

        UserAccountEntity saved = userAccountRepository.save(user);
        return toStaffResponse(saved);
    }

    private UserAccountEntity currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'authentification est requise.");
        }
        return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé ou désactivé."));
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
                entity.getRegistrationNumber(),
                entity.getBio());
    }

    public record ProfileUpdateRequest(
            @NotBlank @Size(min = 3, max = 160) String displayName,
            String phone,
            String photoPath,
            String signaturePath,
            String stampPath,
            String registrationNumber,
            String bio
    ) {
    }
}
