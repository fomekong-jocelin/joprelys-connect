import { I18nService } from '../core/i18n/i18n.service';

/** Codes d'orientation d'une visite, communs à toutes les admissions. */
export const VISIT_ORIENTATION_CODES = [
  'CONSULTATION',
  'SPECIALIZED_CONSULTATION',
  'HOSPITALIZATION',
  'AMBULATORY',
  'DAY_CARE',
  'CHECKUP',
  'OTHER',
] as const;

/**
 * Libellé traduit d'une orientation. Les visites antérieures à l'harmonisation
 * contiennent un texte libre (ex. « Médecine générale ») : il est affiché tel quel.
 */
export function orientationLabel(orientation: string | undefined, i18n: Pick<I18nService, 't'>): string {
  if (!orientation) return '—';
  const key = `admission.orientationOption.${orientation}`;
  const translated = i18n.t(key);
  return translated === key ? orientation : translated;
}
