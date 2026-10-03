package com.joprelys.backend.visit.application;

import com.joprelys.backend.auth.application.StaffProfileAssignmentService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActiveVisitQueueService implements ActiveVisitQueueUseCase {

	private final VisitRepository visitRepository;
	private final UserAccountRepository userAccountRepository;
	private final StaffProfileAssignmentService staffProfileAssignmentService;

	public ActiveVisitQueueService(
			VisitRepository visitRepository,
			UserAccountRepository userAccountRepository,
			StaffProfileAssignmentService staffProfileAssignmentService) {
		this.visitRepository = visitRepository;
		this.userAccountRepository = userAccountRepository;
		this.staffProfileAssignmentService = staffProfileAssignmentService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<VisitEntity> getActiveVisits(Scope scope, Authentication authentication) {
		List<VisitEntity> activeVisits = visitRepository.findActiveVisits();
		return switch (scope == null ? Scope.ALL : scope) {
			case ALL -> activeVisits;
			case MINE -> {
				UUID userId = currentUserId(authentication);
				yield activeVisits.stream()
						.filter(visit -> userId.equals(visit.getMainPractitionerId())
								|| userId.equals(visit.getConsultingPractitionerId()))
						.toList();
			}
			case SERVICE -> {
				Set<String> unitNames = currentUnitNames(authentication);
				yield activeVisits.stream()
						.filter(visit -> visit.getService() != null
								&& unitNames.contains(normalize(visit.getService())))
						.toList();
			}
		};
	}

	private UUID currentUserId(Authentication authentication) {
		return userAccountRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
				.map(UserAccountEntity::getId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouvé."));
	}

	private Set<String> currentUnitNames(Authentication authentication) {
		return staffProfileAssignmentService.getOwnActiveStructure(authentication).unitAssignments().stream()
				.flatMap(unit -> Stream.of(unit.nameFr(), unit.nameEn(), unit.unitCode()))
				.filter(Objects::nonNull)
				.map(ActiveVisitQueueService::normalize)
				.collect(Collectors.toSet());
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
