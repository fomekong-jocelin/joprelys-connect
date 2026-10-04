package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.StaffProfileAssignmentDtos.ActiveSpecialtyResponse;
import com.joprelys.backend.auth.api.StaffProfileAssignmentDtos.ActiveStructureResponse;
import com.joprelys.backend.auth.api.StaffProfileAssignmentDtos.ActiveUnitResponse;
import com.joprelys.backend.auth.infrastructure.persistence.StaffAssignmentRoleCatalogEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffAssignmentRoleCatalogRepository;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.MedicalSpecialtyCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffProfileAssignmentService {

    private final UserAccountRepository userAccountRepository;
    private final StaffSpecialtyAssignmentRepository specialtyAssignmentRepository;
    private final StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository;
    private final StaffAssignmentRoleCatalogRepository assignmentRoleCatalogRepository;
    private final MedicalSpecialtyCatalogRepository medicalSpecialtyCatalogRepository;
    private final OrganizationalUnitRepository organizationalUnitRepository;
    private final HospitalServiceCatalogRepository hospitalServiceCatalogRepository;

    public StaffProfileAssignmentService(
            UserAccountRepository userAccountRepository,
            StaffSpecialtyAssignmentRepository specialtyAssignmentRepository,
            StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository,
            StaffAssignmentRoleCatalogRepository assignmentRoleCatalogRepository,
            MedicalSpecialtyCatalogRepository medicalSpecialtyCatalogRepository,
            OrganizationalUnitRepository organizationalUnitRepository,
            HospitalServiceCatalogRepository hospitalServiceCatalogRepository) {
        this.userAccountRepository = userAccountRepository;
        this.specialtyAssignmentRepository = specialtyAssignmentRepository;
        this.unitAssignmentRepository = unitAssignmentRepository;
        this.assignmentRoleCatalogRepository = assignmentRoleCatalogRepository;
        this.medicalSpecialtyCatalogRepository = medicalSpecialtyCatalogRepository;
        this.organizationalUnitRepository = organizationalUnitRepository;
        this.hospitalServiceCatalogRepository = hospitalServiceCatalogRepository;
    }

    @Transactional(readOnly = true)
    public ActiveStructureResponse getOwnActiveStructure(Authentication authentication) {
        UserAccountEntity user = currentUser(authentication);
        if (user.getOrganizationId() == null) return new ActiveStructureResponse(List.of(), List.of());
        Instant now = Instant.now();

        List<ActiveSpecialtyResponse> specialties = specialtyAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(user.getOrganizationId(), user.getId())
                .stream()
                .filter(item -> item.activeAt(now))
                .map(this::toSpecialtyResponse)
                .sorted(Comparator
                        .comparing(ActiveSpecialtyResponse::primary).reversed()
                        .thenComparing(ActiveSpecialtyResponse::nameFr))
                .toList();

        List<ActiveUnitResponse> units = unitAssignmentRepository
                .findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(user.getOrganizationId(), user.getId())
                .stream()
                .filter(item -> item.activeAt(now))
                .map(item -> toUnitResponse(item, user.getOrganizationId()))
                .sorted(Comparator
                        .comparing(ActiveUnitResponse::primary).reversed()
                        .thenComparing(ActiveUnitResponse::nameFr))
                .toList();

        return new ActiveStructureResponse(specialties, units);
    }

    private ActiveSpecialtyResponse toSpecialtyResponse(StaffSpecialtyAssignmentEntity assignment) {
        MedicalSpecialtyCatalogEntity specialty = medicalSpecialtyCatalogRepository.findById(assignment.getSpecialtyCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Référentiel de spécialité incohérent."));
        return new ActiveSpecialtyResponse(
                assignment.getSpecialtyCode(),
                specialty.getNameFr(),
                specialty.getNameEn(),
                assignment.isPrimary(),
                assignment.getValidFrom(),
                assignment.getValidTo());
    }

    private ActiveUnitResponse toUnitResponse(
            StaffOrganizationalUnitAssignmentEntity assignment,
            java.util.UUID organizationId) {
        OrganizationalUnitEntity unit = organizationalUnitRepository
                .findByIdAndOrganizationId(assignment.getOrganizationalUnitId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unité organisationnelle incohérente."));
        StaffAssignmentRoleCatalogEntity assignmentRole = assignmentRoleCatalogRepository
                .findById(assignment.getAssignmentRoleCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Rôle d'affectation incohérent."));

        String nameFr = unit.getName();
        String nameEn = unit.getName();
        if ((unit.getName() == null || unit.getName().isBlank()) && unit.getServiceCatalogCode() != null) {
            HospitalServiceCatalogEntity service = hospitalServiceCatalogRepository
                    .findById(unit.getServiceCatalogCode())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Référentiel de service incohérent."));
            nameFr = service.getNameFr();
            nameEn = service.getNameEn();
        }
        if (nameFr == null || nameFr.isBlank()) nameFr = unit.getCode();
        if (nameEn == null || nameEn.isBlank()) nameEn = unit.getCode();

        return new ActiveUnitResponse(
                unit.getId(),
                unit.getCode(),
                nameFr,
                nameEn,
                assignment.getAssignmentRoleCode(),
                assignmentRole.getNameFr(),
                assignmentRole.getNameEn(),
                assignment.isPrimary(),
                assignment.getValidFrom(),
                assignment.getValidTo());
    }

    private UserAccountEntity currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "L'authentification est requise.");
        }
        UserAccountEntity user = userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(UserAccountEntity::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé ou désactivé."));

        return user;
    }
}
