import { AvailabilityException, AvailabilityRule } from './availability.models';

/**
 * Durée de créneau affichée dans l'aperçu : défaut RM-01
 * (`joprelys.appointments.default-slot-duration-minutes` côté backend).
 * Aucun endpoint créneaux n'existe encore — calcul purement indicatif côté client.
 */
export const DEFAULT_SLOT_DURATION_MINUTES = 30;

/** Fenêtre de l'aperçu client : les 7 prochains jours. */
export const SLOT_PREVIEW_DAYS = 7;

export interface AvailabilitySlotView {
  readonly start: Date;
  readonly end: Date;
}

export interface AvailabilityDaySlots {
  readonly date: Date;
  readonly slots: readonly AvailabilitySlotView[];
}

/**
 * Calcule les créneaux libres des `days` prochains jours :
 * règles actives du jour (fenêtre de validité incluse) découpées en créneaux de
 * `slotMinutes`, moins les créneaux passés et ceux recouverts par une exception.
 */
export function computeUpcomingSlots(
  rules: readonly AvailabilityRule[],
  exceptions: readonly AvailabilityException[],
  now: Date = new Date(),
  days: number = SLOT_PREVIEW_DAYS,
  slotMinutes: number = DEFAULT_SLOT_DURATION_MINUTES,
): AvailabilityDaySlots[] {
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const result: AvailabilityDaySlots[] = [];

  for (let offset = 0; offset < days; offset += 1) {
    const day = new Date(startOfToday.getFullYear(), startOfToday.getMonth(), startOfToday.getDate() + offset);
    const dateKey = toLocalDateKey(day);
    const isoWeekday = ((day.getDay() + 6) % 7) + 1; // JS 0 = dimanche → ISO 1 = lundi … 7 = dimanche

    const dayRules = rules.filter((rule) =>
      rule.active
      && rule.weekday === isoWeekday
      && rule.validFrom <= dateKey
      && (!rule.validTo || rule.validTo >= dateKey));

    const slots: AvailabilitySlotView[] = [];
    for (const rule of dayRules) {
      const startMinutes = parseTimeToMinutes(rule.startTime);
      const endMinutes = parseTimeToMinutes(rule.endTime);
      if (startMinutes === null || endMinutes === null || endMinutes <= startMinutes) {
        continue;
      }
      for (let cursor = startMinutes; cursor + slotMinutes <= endMinutes; cursor += slotMinutes) {
        const start = new Date(day.getFullYear(), day.getMonth(), day.getDate(), Math.floor(cursor / 60), cursor % 60);
        const end = new Date(start.getTime() + slotMinutes * 60_000);
        if (start.getTime() < now.getTime()) {
          continue; // pas de créneau dans le passé
        }
        if (isCoveredByException(start, end, exceptions)) {
          continue;
        }
        slots.push({ start, end });
      }
    }

    slots.sort((left, right) => left.start.getTime() - right.start.getTime());
    result.push({ date: day, slots });
  }

  return result;
}

/** Vrai si le créneau [start, end[ chevauche au moins une exception d'indisponibilité. */
function isCoveredByException(start: Date, end: Date, exceptions: readonly AvailabilityException[]): boolean {
  return exceptions.some((exception) => {
    const exceptionStart = new Date(exception.startAt).getTime();
    const exceptionEnd = new Date(exception.endAt).getTime();
    return Number.isFinite(exceptionStart)
      && Number.isFinite(exceptionEnd)
      && exceptionStart < end.getTime()
      && exceptionEnd > start.getTime();
  });
}

/** Parse une heure `HH:mm` en minutes depuis minuit (null si invalide). */
function parseTimeToMinutes(value: string): number | null {
  const match = /^(\d{2}):(\d{2})/.exec(value);
  if (!match) {
    return null;
  }
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) {
    return null;
  }
  return hours * 60 + minutes;
}

/** Clé `yyyy-MM-dd` en heure locale (comparaison lexicographique avec validFrom/validTo). */
export function toLocalDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}
