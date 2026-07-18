/**
 * Modèles de la page « Mes disponibilités » (STORY-2602).
 * Alignés sur le contrat `docs/features/doctor-availability-appointments/API-CONTRACT.md` §2.
 */

/** Règle de disponibilité hebdomadaire récurrente (`AvailabilityRuleResponse`). */
export interface AvailabilityRule {
  readonly id: string;
  readonly doctorId: string;
  /** Jour de la semaine ISO-8601 : 1 = lundi … 7 = dimanche. */
  readonly weekday: number;
  /** Heure de début au format `HH:mm`. */
  readonly startTime: string;
  /** Heure de fin au format `HH:mm`. */
  readonly endTime: string;
  /** Début de validité au format `yyyy-MM-dd`. */
  readonly validFrom: string;
  /** Fin de validité au format `yyyy-MM-dd` (null = sans fin). */
  readonly validTo: string | null;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** Exception d'indisponibilité (`AvailabilityExceptionResponse`). */
export interface AvailabilityException {
  readonly id: string;
  readonly doctorId: string;
  /** Début de l'indisponibilité (Instant ISO-8601 UTC). */
  readonly startAt: string;
  /** Fin de l'indisponibilité (Instant ISO-8601 UTC). */
  readonly endAt: string;
  readonly reason: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** Corps de création / modification d'une règle (`UpsertAvailabilityRuleRequest`). */
export interface UpsertAvailabilityRuleRequest {
  /** Omis = médecin connecté (ciblage admin d'un autre médecin hors MVP). */
  readonly doctorId?: string;
  /** Jour de la semaine ISO-8601 : 1 = lundi … 7 = dimanche. */
  readonly weekday: number;
  readonly startTime: string;
  readonly endTime: string;
  readonly validFrom: string;
  readonly validTo?: string | null;
}

/** Corps de création d'une indisponibilité (`CreateAvailabilityExceptionRequest`). */
export interface CreateAvailabilityExceptionRequest {
  /** Omis = médecin connecté. */
  readonly doctorId?: string;
  /** Instant ISO-8601 UTC. */
  readonly startAt: string;
  /** Instant ISO-8601 UTC. */
  readonly endAt: string;
  readonly reason?: string | null;
}
