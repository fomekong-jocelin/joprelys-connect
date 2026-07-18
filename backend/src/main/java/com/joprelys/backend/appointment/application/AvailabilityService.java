package com.joprelys.backend.appointment.application;

import com.joprelys.backend.appointment.api.AvailabilityExceptionResponse;
import com.joprelys.backend.appointment.api.AvailabilityRuleResponse;
import com.joprelys.backend.appointment.api.CreateAvailabilityExceptionRequest;
import com.joprelys.backend.appointment.api.UpsertAvailabilityRuleRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrat du service de gestion des disponibilités médecin (STORY-2602, contrat API §2).
 *
 * <p>Sécurité : un médecin ({@code clinicAdmin = false}) ne consulte et ne modifie que ses
 * propres règles et indisponibilités ; un administrateur de clinique gère tous les médecins de
 * son tenant. {@code doctorId} omis = appelant. L'isolation multi-tenant est assurée par le
 * filtre Hibernate {@code @TenantId} : une ressource d'un autre tenant répond 404.
 */
public interface AvailabilityService {

	List<AvailabilityRuleResponse> listRules(UUID requestedDoctorId, UUID callerId, boolean clinicAdmin);

	AvailabilityRuleResponse createRule(
			UpsertAvailabilityRuleRequest request, UUID callerId, UUID organizationId, boolean clinicAdmin);

	AvailabilityRuleResponse updateRule(
			UUID ruleId, UpsertAvailabilityRuleRequest request, UUID callerId, UUID organizationId, boolean clinicAdmin);

	/** Désactivation logique (RM-07) : la règle est conservée avec {@code active = false}. */
	AvailabilityRuleResponse deactivateRule(UUID ruleId, UUID callerId, boolean clinicAdmin);

	List<AvailabilityExceptionResponse> listExceptions(
			UUID requestedDoctorId, Instant from, Instant to, UUID callerId, boolean clinicAdmin);

	AvailabilityExceptionResponse createException(
			CreateAvailabilityExceptionRequest request, UUID callerId, UUID organizationId, boolean clinicAdmin);

	/** Suppression physique d'une indisponibilité (contrairement aux règles, voir contrat). */
	void deleteException(UUID exceptionId, UUID callerId, boolean clinicAdmin);

	/** Créneaux libres du médecin : règles actives − indisponibilités − créneaux réservés. */
	List<AppointmentSlotGenerator.Slot> generateSlots(UUID doctorId, Instant from, Instant to);
}
