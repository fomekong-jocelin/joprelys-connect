/**
 * Interprétation d'un résultat de biologie, alignée sur le backend (`LabInterpretation`).
 * Une valeur absente ou inconnue n'est jamais présentée comme normale (relecture clinique du 2026-11-04).
 */
export const LAB_INTERPRETATIONS = ['NORMAL', 'LOW', 'HIGH', 'ABNORMAL', 'CRITICAL', 'NOT_INTERPRETED'] as const;

export type LabInterpretation = (typeof LAB_INTERPRETATIONS)[number];

/** Gravité d'affichage : `unknown` reste neutre, jamais vert. */
export type LabInterpretationTone = 'normal' | 'outside' | 'critical' | 'unknown';

export function toLabInterpretation(value: string | null | undefined): LabInterpretation {
  const normalized = value?.trim().toUpperCase();
  return LAB_INTERPRETATIONS.find((code) => code === normalized) ?? 'NOT_INTERPRETED';
}

export function labInterpretationKey(value: string | null | undefined): string {
  return `lab.interpretation.${toLabInterpretation(value)}`;
}

export function labInterpretationTone(value: string | null | undefined): LabInterpretationTone {
  switch (toLabInterpretation(value)) {
    case 'NORMAL':
      return 'normal';
    case 'LOW':
    case 'HIGH':
    case 'ABNORMAL':
      return 'outside';
    case 'CRITICAL':
      return 'critical';
    default:
      return 'unknown';
  }
}
