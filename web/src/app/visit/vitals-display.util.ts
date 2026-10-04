import { Visit, VisitCareStage, VitalAlert } from './visit.models';

/** Classes de la pastille IMC (dénutrition, normal, surpoids, obésité). */
export function bmiClass(bmi?: number): string {
  if (!bmi) return 'bg-[var(--app-surface-muted)] text-[var(--text-secondary)]';
  if (bmi < 18.5) return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
  if (bmi < 25) return 'bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
  if (bmi < 30) return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
  return 'bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]';
}

/** Étape de prise en charge ; les visites antérieures à l'étape sont déduites des constantes. */
export function careStageOf(visit: Visit): VisitCareStage {
  return visit.careStage ?? (visit.vitals ? 'PRET_MEDECIN' : 'ATTENTE_CONSTANTES');
}

export function careStageClass(stage: VisitCareStage): string {
  switch (stage) {
    case 'ATTENTE_CONSTANTES':
      return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    case 'PRET_MEDECIN':
      return 'bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    case 'EN_CONSULTATION':
      return 'bg-[var(--brand-info-subtle)] text-[var(--brand-info-text)]';
  }
}

export function hasCriticalAlert(alerts?: readonly VitalAlert[]): boolean {
  return Boolean(alerts?.some((alert) => alert.severity === 'CRITICAL'));
}
