package com.joprelys.backend.visit.application;

import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.HospitalServiceCatalogRepository;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisitAdmissionOptionsService implements VisitAdmissionOptionsUseCase {

	private static final List<String> CLINICIAN_ROLES = List.of("MEDECIN", "INFIRMIER");

	private final UserAccountRepository userAccountRepository;
	private final OrganizationalUnitRepository organizationalUnitRepository;
	private final HospitalServiceCatalogRepository serviceCatalogRepository;
	private final StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository;

	public VisitAdmissionOptionsService(
			UserAccountRepository userAccountRepository,
			OrganizationalUnitRepository organizationalUnitRepository,
			HospitalServiceCatalogRepository serviceCatalogRepository,
			StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository) {
		this.userAccountRepository = userAccountRepository;
		this.organizationalUnitRepository = organizationalUnitRepository;
		this.serviceCatalogRepository = serviceCatalogRepository;
		this.unitAssignmentRepository = unitAssignmentRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AdmissionOptions getAdmissionOptions(Authentication authentication) {
		UUID organizationId = currentOrganizationId(authentication);
		List<OrganizationalUnitEntity> units =
				organizationalUnitRepository.findAllByOrganizationIdAndActiveTrueOrderByCodeAsc(organizationId);
		Map<UUID, String> unitNames = units.stream()
				.collect(Collectors.toMap(OrganizationalUnitEntity::getId, VisitAdmissionOptionsService::unitName, (a, b) -> a));

		// Les unités configurées font foi ; sans structure, le catalogue national des services sert de repli.
		List<String> services = units.isEmpty()
				? serviceCatalogRepository.findAllByActiveTrueOrderByNameFrAsc().stream()
						.map(HospitalServiceCatalogEntity::getNameFr)
						.toList()
				: unitNames.values().stream().distinct().sorted().toList();

		Instant now = Instant.now();
		List<PractitionerOption> practitioners = userAccountRepository
				.findAllByOrganizationIdAndEnabledTrueOrderByDisplayNameAsc(organizationId).stream()
				.filter(user -> CLINICIAN_ROLES.stream().anyMatch(user::hasRole))
				.map(user -> new PractitionerOption(
						user.getId(),
						user.getDisplayName(),
						user.hasRole("MEDECIN") ? "MEDECIN" : "INFIRMIER",
						unitAssignmentRepository
								.findAllByOrganizationIdAndStaffIdOrderByValidFromDesc(organizationId, user.getId())
								.stream()
								.filter(assignment -> assignment.activeAt(now))
								.map(assignment -> unitNames.get(assignment.getOrganizationalUnitId()))
								.filter(Objects::nonNull)
								.distinct()
								.toList()))
				.toList();

		return new AdmissionOptions(services, practitioners);
	}

	private UUID currentOrganizationId(Authentication authentication) {
		return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
				.map(UserAccountEntity::getOrganizationId)
				.filter(Objects::nonNull)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Collaborateur non rattaché à un établissement."));
	}

	private static String unitName(OrganizationalUnitEntity unit) {
		return unit.getName() != null && !unit.getName().isBlank() ? unit.getName() : unit.getCode();
	}
}
