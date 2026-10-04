package com.joprelys.backend.visit.application;

import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.List;
import org.springframework.security.core.Authentication;

/**
 * File des visites actives, filtrable selon le point de vue du praticien connecté.
 */
public interface ActiveVisitQueueUseCase {

	enum Scope {
		/** Toutes les visites actives de l'établissement. */
		ALL,
		/** Visites dont le praticien connecté est le praticien principal ou qu'il a en consultation. */
		MINE,
		/** Visites orientées vers une unité à laquelle le praticien connecté est affecté. */
		SERVICE
	}

	List<VisitEntity> getActiveVisits(Scope scope, Authentication authentication);
}
