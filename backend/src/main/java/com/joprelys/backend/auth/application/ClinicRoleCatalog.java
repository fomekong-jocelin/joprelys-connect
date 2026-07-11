package com.joprelys.backend.auth.application;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catalogue central des rôles attribuables par un administrateur clinique.
 *
 * <p>Les rôles plateforme et patient sont volontairement exclus : une clinique
 * ne doit jamais pouvoir attribuer {@code ADMIN_JOPRELYS} ou {@code PATIENT}.</p>
 */
public final class ClinicRoleCatalog {

	private static final List<RoleDefinition> DEFINITIONS = List.of(
			new RoleDefinition("ADMIN_CLINIQUE", "staff.roles.ADMIN_CLINIQUE", "staff.roleDescriptions.ADMIN_CLINIQUE", "GOVERNANCE", true),
			new RoleDefinition("DAF", "staff.roles.DAF", "staff.roleDescriptions.DAF", "FINANCE", true),
			new RoleDefinition("SECRETAIRE_COMPTABLE", "staff.roles.SECRETAIRE_COMPTABLE", "staff.roleDescriptions.SECRETAIRE_COMPTABLE", "FINANCE", false),
			new RoleDefinition("CAISSIER", "staff.roles.CAISSIER", "staff.roleDescriptions.CAISSIER", "FINANCE", false),
			new RoleDefinition("AUDITEUR", "staff.roles.AUDITEUR", "staff.roleDescriptions.AUDITEUR", "GOVERNANCE", true),
			new RoleDefinition("MEDECIN", "staff.roles.MEDECIN", "staff.roleDescriptions.MEDECIN", "CLINICAL", false),
			new RoleDefinition("INFIRMIER", "staff.roles.INFIRMIER", "staff.roleDescriptions.INFIRMIER", "CLINICAL", false),
			new RoleDefinition("AGENT_ACCUEIL", "staff.roles.AGENT_ACCUEIL", "staff.roleDescriptions.AGENT_ACCUEIL", "OPERATIONS", false),
			new RoleDefinition("PHARMACIEN", "staff.roles.PHARMACIEN", "staff.roleDescriptions.PHARMACIEN", "CLINICAL", false),
			new RoleDefinition("BIOLOGISTE", "staff.roles.BIOLOGISTE", "staff.roleDescriptions.BIOLOGISTE", "CLINICAL", false));

	private static final Map<String, RoleDefinition> BY_CODE = DEFINITIONS.stream()
			.collect(Collectors.toUnmodifiableMap(RoleDefinition::code, Function.identity()));

	private ClinicRoleCatalog() {
	}

	public static List<RoleDefinition> definitions() {
		return DEFINITIONS;
	}

	public static Set<String> codes() {
		return BY_CODE.keySet();
	}

	public static boolean isManageable(String roleCode) {
		return roleCode != null && BY_CODE.containsKey(roleCode.trim().toUpperCase(Locale.ROOT));
	}

	public static boolean hasRole(String roles, String expectedRole) {
		if (roles == null || expectedRole == null) {
			return false;
		}
		return Arrays.stream(roles.split(","))
				.map(String::trim)
				.anyMatch(expectedRole::equalsIgnoreCase);
	}

	public static boolean hasAnyManageableRole(String roles) {
		if (roles == null || roles.isBlank()) {
			return false;
		}
		return Arrays.stream(roles.split(","))
				.map(String::trim)
				.anyMatch(ClinicRoleCatalog::isManageable);
	}

	public static String normalizeRoles(String rawRoles) {
		if (rawRoles == null || rawRoles.isBlank()) {
			throw new IllegalArgumentException("Au moins un rôle doit être sélectionné.");
		}

		LinkedHashSet<String> requested = Arrays.stream(rawRoles.split(","))
				.map(String::trim)
				.filter(role -> !role.isBlank())
				.map(role -> role.toUpperCase(Locale.ROOT))
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (requested.isEmpty()) {
			throw new IllegalArgumentException("Au moins un rôle doit être sélectionné.");
		}

		List<String> unknownRoles = requested.stream()
				.filter(role -> !BY_CODE.containsKey(role))
				.toList();
		if (!unknownRoles.isEmpty()) {
			throw new IllegalArgumentException("Rôle clinique invalide : " + String.join(", ", unknownRoles));
		}

		return DEFINITIONS.stream()
				.map(RoleDefinition::code)
				.filter(requested::contains)
				.collect(Collectors.joining(","));
	}

	public record RoleDefinition(
			String code,
			String labelKey,
			String descriptionKey,
			String category,
			boolean sensitive) {
	}
}
