/** Périmètres de partage, en valeurs de contrat (eco-contracts v1). */
export type LinkedAppScope =
  | 'prescriptions'
  | 'lab-results'
  | 'immunizations'
  | 'appointments'
  | 'emergency-summary';

/** Application de l'écosystème reliée au dossier du patient connecté. */
export interface LinkedApp {
  readonly id: string;
  readonly application: string;
  readonly scopes: readonly LinkedAppScope[];
  readonly linkedAt: string;
  readonly validUntil: string | null;
}
