package com.joprelys.backend.visit.application;

import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalorganization.application.OrganizationalUnitNamesService;
import com.joprelys.backend.hospitalorganization.application.OrganizationalUnitNamesService.UnitDirectory;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitAdmissionOptionsService implements VisitAdmissionOptionsUseCase {

	private static final List<String> CLINICIAN_ROLES = List.of("MEDECIN", "INFIRMIER");

	/**
	 * Services du catalogue où l'on n'ouvre pas de visite depuis l'accueil (support ou accès
	 * uniquement par hospitalisation). Laboratoire et imagerie restent proposés : un patient
	 * peut venir pour un examen seul.
	 */
	private static final Set<String> NON_ADMISSION_SERVICE_CODES =
			Set.of("PHARMACY", "OPERATING_THEATRE", "ANESTHESIA", "INTENSIVE_CARE");

	private final UserAccountRepository userAccountRepository;
	private final HospitalServiceCatalogRepository serviceCatalogRepository;
	private final StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository;
	private final OrganizationalUnitNamesService unitNames;

	public VisitAdmissionOptionsService(
			UserAccountRepository userAccountRepository,
			HospitalServiceCatalogRepository serviceCatalogRepository,
			StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository,
			OrganizationalUnitNamesService unitNames) {
		this.userAccountRepository = userAccountRepository;
		this.serviceCatalogRepository = serviceCatalogRepository;
		this.unitAssignmentRepository = unitAssignmentRepository;
		this.unitNames = unitNames;
	}

	@Override
	@Transactional(readOnly = true)
	public AdmissionOptions getAdmissionOptions(Authentication authentication) {
		UUID organizationId = currentOrganizationId(authentication);
		UnitDirectory directory = unitNames.directory(organizationId);

		// Les unités configurées font foi ; sans structure, le catalogue des services sert de repli.
		List<String> unitServices = directory.units().stream()
				.filter(unit -> isAdmissionTarget(unit, directory.units()))
				.filter(unit -> !isNonAdmissionService(unit, directory))
				.map(directory::name)
				.distinct()
				.sorted()
				.toList();
		List<String> services = unitServices.isEmpty()
				? serviceCatalogRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
						.filter(service -> !NON_ADMISSION_SERVICE_CODES.contains(service.getCode()))
						.map(HospitalServiceCatalogEntity::getNameFr)
						.toList()
				: unitServices;

		Instant now = Instant.now();
		List<PractitionerOption> practitioners = userAccountRepository
				.findAllByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(organizationId).stream()
				.filter(user -> CLINICIAN_ROLES.stream().anyMatch(user::hasRole))
				.map(user -> new PractitionerOption(
						user.getId(),
						user.getDisplayName(),
						user.hasRole("MEDECIN") ? "MEDECIN" : "INFIRMIER",
						List.copyOf(directory.namesWithAncestors(unitAssignmentRepository
								.findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, user.getId())
								.stream()
								.filter(assignment -> assignment.activeAt(now))
								.map(StaffOrganizationalUnitAssignmentEntity::getOrganizationalUnitId)
								.toList()))))
				.toList();

		return new AdmissionOptions(services, practitioners);
	}

	private UUID currentOrganizationId(Authentication authentication) {
		return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
				.map(UserAccountEntity::getOrganizationId)
				.filter(Objects::nonNull)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Collaborateur non rattaché à un établissement."));
	}

	/** Une unité de soins, ou un service sans unité de soins rattachée (ex. imagerie, laboratoire). */
	private static boolean isAdmissionTarget(OrganizationalUnitEntity unit, List<OrganizationalUnitEntity> allUnits) {
		return switch (unit.getUnitType()) {
			case CARE_UNIT -> true;
			case SERVICE -> allUnits.stream().noneMatch(child -> unit.getId().equals(child.getParentId())
					&& child.getUnitType() == OrganizationalUnitType.CARE_UNIT);
			default -> false;
		};
	}

	private static boolean isNonAdmissionService(OrganizationalUnitEntity unit, UnitDirectory directory) {
		OrganizationalUnitEntity current = unit;
		int guard = 0;
		while (current != null && guard++ < 10) {
			String code = current.getServiceCatalogCode();
			if (code != null && NON_ADMISSION_SERVICE_CODES.contains(code)) return true;
			current = current.getParentId() != null ? directory.get(current.getParentId()) : null;
		}
		return false;
	}
}
