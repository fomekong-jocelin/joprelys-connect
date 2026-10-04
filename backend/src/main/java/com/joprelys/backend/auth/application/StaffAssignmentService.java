package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.StaffAssignmentDtos.AssignmentRoleResponse;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.CloseAssignmentRequest;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.SpecialtyAssignmentRequest;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.SpecialtyAssignmentResponse;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.StructureResponse;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.UnitAssignmentRequest;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.UnitAssignmentResponse;
import com.joprelys.backend.auth.infrastructure.persistence.StaffAssignmentRoleCatalogEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffAssignmentRoleCatalogRepository;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffAssignmentService {

    private static final Set<String> NON_ASSIGNABLE_BASE_ROLES = Set.of(
            "SUPER_ADMIN",
            "ADMIN_JOPRELYS",
            "PATIENT");

    private static final Set<String> CLINICAL_PRACTITIONER_ROLES = Set.of(
            "MEDECIN",
            "INFIRMIER",
            "SAGE_FEMME",
            "PHARMACIEN",
            "BIOLOGISTE",
            "AIDE_SOIGNANT",
            "KINESITHERAPEUTE",
            "CHIRURGIEN");

    private final UserAccountRepository userAccountRepository;
    private final StaffSpecialtyAssignmentRepository specialtyAssignmentRepository;
    private final StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository;
    private final StaffAssignmentRoleCatalogRepository assignmentRoleCatalogRepository;
    private final MedicalSpecialtyCatalogRepository medicalSpecialtyCatalogRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;

    public StaffAssignmentService(
            UserAccountRepository userAccountRepository,
            StaffSpecialtyAssignmentRepository specialtyAssignmentRepository,
            StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository,
            StaffAssignmentRoleCatalogRepository assignmentRoleCatalogRepository,
            MedicalSpecialtyCatalogRepository medicalSpecialtyCatalogRepository,
            OrganizationalUnitRepository organizationalUnitRepository) {
        this.userAccountRepository = userAccountRepository;
        this.specialtyAssignmentRepository = specialtyAssignmentRepository;
        this.unitAssignmentRepository = unitAssignmentRepository;
        this.assignmentRoleCatalogRepository = assignmentRoleCatalogRepository;
        this.medicalSpecialtyCatalogRepository = medicalSpecialtyCatalogRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
    }

    @Transactional(readOnly = true)
    public List<AssignmentRoleResponse> listAssignmentRoles(Authentication authentication) {
        currentAdmin(authentication);
        return assignmentRoleCatalogRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StructureResponse getStructure(UUID staffId, Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        Instant now = Instant.now();
        return new StructureResponse(
                specialtyAssignmentRepository
                        .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(admin.getOrganizationId(), staff.getId())
                        .stream()
                        .map(item -> toSpecialtyResponse(item, now))
                        .toList(),
                unitAssignmentRepository
                        .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(admin.getOrganizationId(), staff.getId())
                        .stream()
                        .map(item -> toUnitResponse(item, now))
                        .toList());
    }

    @Transactional
    public SpecialtyAssignmentResponse createSpecialty(
            UUID staffId,
            SpecialtyAssignmentRequest request,
            Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        validatePeriod(request.validFrom(), request.validTo());
        String specialtyCode = requireActiveSpecialty(request.specialtyCode());
        validateSpecialtyOverlap(
                admin.getOrganizationId(), staff.getId(), specialtyCode, request.primary(), request.validFrom(), request.validTo(), null);

        StaffSpecialtyAssignmentEntity saved = specialtyAssignmentRepository.save(new StaffSpecialtyAssignmentEntity(
                admin.getOrganizationId(),
                staff.getId(),
                specialtyCode,
                request.primary(),
                request.validFrom(),
                request.validTo()));
        return toSpecialtyResponse(saved, Instant.now());
    }

    @Transactional
    public SpecialtyAssignmentResponse updateSpecialty(
            UUID staffId,
            UUID assignmentId,
            SpecialtyAssignmentRequest request,
            Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        StaffSpecialtyAssignmentEntity assignment = specialtyAssignment(assignmentId, staff, admin);
        validatePeriod(request.validFrom(), request.validTo());
        String specialtyCode = requireActiveSpecialty(request.specialtyCode());
        validateSpecialtyOverlap(
                admin.getOrganizationId(), staff.getId(), specialtyCode, request.primary(), request.validFrom(), request.validTo(), assignment.getId());
        assignment.update(specialtyCode, request.primary(), request.validFrom(), request.validTo());
        return toSpecialtyResponse(specialtyAssignmentRepository.save(assignment), Instant.now());
    }

    @Transactional
    public SpecialtyAssignmentResponse closeSpecialty(
            UUID staffId,
            UUID assignmentId,
            CloseAssignmentRequest request,
            Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        StaffSpecialtyAssignmentEntity assignment = specialtyAssignment(assignmentId, staff, admin);
        closeSpecialtyAssignment(assignment, request.closedAt());
        return toSpecialtyResponse(specialtyAssignmentRepository.save(assignment), Instant.now());
    }

    @Transactional
    public UnitAssignmentResponse createUnit(
            UUID staffId,
            UnitAssignmentRequest request,
            Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        validatePeriod(request.validFrom(), request.validTo());
        UUID unitId = requireActiveUnit(request.organizationalUnitId(), admin.getOrganizationId());
        String roleCode = requireActiveAssignmentRole(request.assignmentRoleCode());
        validateResponsibility(staff, roleCode);
        validateUnitOverlap(
                admin.getOrganizationId(), staff.getId(), unitId, request.primary(), request.validFrom(), request.validTo(), null);

        StaffOrganizationalUnitAssignmentEntity saved = unitAssignmentRepository.save(
                new StaffOrganizationalUnitAssignmentEntity(
                        admin.getOrganizationId(),
                        staff.getId(),
                        unitId,
                        roleCode,
                        request.primary(),
                        request.validFrom(),
                        request.validTo()));
        return toUnitResponse(saved, Instant.now());
    }

    @Transactional
    public UnitAssignmentResponse updateUnit(
            UUID staffId,
            UUID assignmentId,
            UnitAssignmentRequest request,
            Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        StaffOrganizationalUnitAssignmentEntity assignment = unitAssignment(assignmentId, staff, admin);
        validatePeriod(request.validFrom(), request.validTo());
        UUID unitId = requireActiveUnit(request.organizationalUnitId(), admin.getOrganizationId());
        String roleCode = requireActiveAssignmentRole(request.assignmentRoleCode());
        validateResponsibility(staff, roleCode);
        validateUnitOverlap(
                admin.getOrganizationId(), staff.getId(), unitId, request.primary(), request.validFrom(), request.validTo(), assignment.getId());
        assignment.update(unitId, roleCode, request.primary(), request.validFrom(), request.validTo());
        return toUnitResponse(unitAssignmentRepository.save(assignment), Instant.now());
    }

    @Transactional
    public UnitAssignmentResponse closeUnit(
            UUID staffId,
            UUID assignmentId,
            CloseAssignmentRequest request,
            Authentication authentication) {
        UserAccountEntity admin = currentAdmin(authentication);
        UserAccountEntity staff = managedStaff(staffId, admin);
        StaffOrganizationalUnitAssignmentEntity assignment = unitAssignment(assignmentId, staff, admin);
        closeUnitAssignment(assignment, request.closedAt());
        return toUnitResponse(unitAssignmentRepository.save(assignment), Instant.now());
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

    private UserAccountEntity managedStaff(UUID staffId, UserAccountEntity admin) {
        UserAccountEntity staff = userAccountRepository.findByIdAndOrganizationId(staffId, admin.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborateur non trouvé."));
        if (NON_ASSIGNABLE_BASE_ROLES.stream().anyMatch(staff::hasRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte ne peut pas recevoir d'affectation hospitalière.");
        }
        if (staff.hasRole("ADMIN_CLINIQUE") && CLINICAL_PRACTITIONER_ROLES.stream().noneMatch(staff::hasRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte ne peut pas recevoir d'affectation hospitalière.");
        }
        return staff;
    }

    private StaffSpecialtyAssignmentEntity specialtyAssignment(
            UUID assignmentId,
            UserAccountEntity staff,
            UserAccountEntity admin) {
        StaffSpecialtyAssignmentEntity assignment = specialtyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation de spécialité introuvable."));
        if (!admin.getOrganizationId().equals(assignment.getOrganizationId()) || !staff.getId().equals(assignment.getStaffId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation de spécialité introuvable.");
        }
        return assignment;
    }

    private StaffOrganizationalUnitAssignmentEntity unitAssignment(
            UUID assignmentId,
            UserAccountEntity staff,
            UserAccountEntity admin) {
        StaffOrganizationalUnitAssignmentEntity assignment = unitAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation d'unité introuvable."));
        if (!admin.getOrganizationId().equals(assignment.getOrganizationId()) || !staff.getId().equals(assignment.getStaffId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Affectation d'unité introuvable.");
        }
        return assignment;
    }

    private String requireActiveSpecialty(String rawCode) {
        String code = normalizeCode(rawCode);
        MedicalSpecialtyCatalogEntity specialty = medicalSpecialtyCatalogRepository.findById(code)
                .filter(MedicalSpecialtyCatalogEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spécialité inactive ou inconnue : " + code));
        return specialty.getCode();
    }

    private UUID requireActiveUnit(UUID unitId, UUID organizationId) {
        OrganizationalUnitEntity unit = organizationalUnitRepository.findByIdAndOrganizationId(unitId, organizationId)
                .filter(OrganizationalUnitEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unité organisationnelle inactive ou inconnue."));
        return unit.getId();
    }

    private void validateResponsibility(UserAccountEntity staff, String roleCode) {
        java.util.Set<String> roles = java.util.Arrays.stream(staff.getRole().split(","))
                .map(String::trim).collect(java.util.stream.Collectors.toSet());
        if (("MEDICAL_HEAD".equals(roleCode) && !roles.contains("MEDECIN"))
                || ("NURSE_MANAGER".equals(roleCode) && !roles.contains("INFIRMIER") && !roles.contains("SAGE_FEMME"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle professionnel ne permet pas cette responsabilité.");
        }
    }

    private String requireActiveAssignmentRole(String rawCode) {
        String code = normalizeCode(rawCode);
        StaffAssignmentRoleCatalogEntity role = assignmentRoleCatalogRepository.findById(code)
                .filter(StaffAssignmentRoleCatalogEntity::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle d'affectation inactif ou inconnu : " + code));
        return role.getCode();
    }

    private void validateSpecialtyOverlap(
            UUID organizationId,
            UUID staffId,
            String specialtyCode,
            boolean primary,
            Instant validFrom,
            Instant validTo,
            UUID excludedId) {
        List<StaffSpecialtyAssignmentEntity> existing = specialtyAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, staffId);
        boolean sameSpecialtyOverlap = existing.stream()
                .filter(item -> item.getSpecialtyCode().equals(specialtyCode))
                .anyMatch(item -> item.overlaps(validFrom, validTo, excludedId));
        if (sameSpecialtyOverlap) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette spécialité chevauche une affectation existante.");
        }
        if (primary) {
            boolean primaryOverlap = existing.stream()
                    .filter(StaffSpecialtyAssignmentEntity::isPrimary)
                    .anyMatch(item -> item.overlaps(validFrom, validTo, excludedId));
            if (primaryOverlap) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une autre spécialité principale couvre déjà cette période.");
            }
        }
    }

    private void validateUnitOverlap(
            UUID organizationId,
            UUID staffId,
            UUID unitId,
            boolean primary,
            Instant validFrom,
            Instant validTo,
            UUID excludedId) {
        List<StaffOrganizationalUnitAssignmentEntity> existing = unitAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, staffId);
        boolean sameUnitOverlap = existing.stream()
                .filter(item -> item.getOrganizationalUnitId().equals(unitId))
                .anyMatch(item -> item.overlaps(validFrom, validTo, excludedId));
        if (sameUnitOverlap) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette unité chevauche une affectation existante.");
        }
        if (primary) {
            boolean primaryOverlap = existing.stream()
                    .filter(StaffOrganizationalUnitAssignmentEntity::isPrimary)
                    .anyMatch(item -> item.overlaps(validFrom, validTo, excludedId));
            if (primaryOverlap) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une autre unité principale couvre déjà cette période.");
            }
        }
    }

    private void closeSpecialtyAssignment(StaffSpecialtyAssignmentEntity assignment, Instant closedAt) {
        try {
            assignment.closeAt(closedAt);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private void closeUnitAssignment(StaffOrganizationalUnitAssignmentEntity assignment, Instant closedAt) {
        try {
            assignment.closeAt(closedAt);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private static void validatePeriod(Instant validFrom, Instant validTo) {
        if (validFrom == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de début est obligatoire.");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit être postérieure au début de l'affectation.");
        }
    }

    private AssignmentRoleResponse toRoleResponse(StaffAssignmentRoleCatalogEntity entity) {
        return new AssignmentRoleResponse(entity.getCode(), entity.getNameFr(), entity.getNameEn());
    }

    private static SpecialtyAssignmentResponse toSpecialtyResponse(StaffSpecialtyAssignmentEntity entity, Instant now) {
        return new SpecialtyAssignmentResponse(
                entity.getId(),
                entity.getSpecialtyCode(),
                entity.isPrimary(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.activeAt(now));
    }

    private static UnitAssignmentResponse toUnitResponse(StaffOrganizationalUnitAssignmentEntity entity, Instant now) {
        return new UnitAssignmentResponse(
                entity.getId(),
                entity.getOrganizationalUnitId(),
                entity.getAssignmentRoleCode(),
                entity.isPrimary(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.activeAt(now));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code est obligatoire.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
