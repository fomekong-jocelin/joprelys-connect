/** Identité visuelle d'une application partenaire de l'écosystème (configuration publique, sans secret). */
export interface LinkedAppIdentity {
  /** Nom de marque, identique dans toutes les langues ; `null` pour une application inconnue. */
  readonly name: string | null;
  readonly logoUrl: string | null;
}

const KNOWN_APPS: Readonly<Record<string, LinkedAppIdentity>> = {
  allopharma: { name: 'AllôPharma', logoUrl: '/assets/partners/allopharma.png' },
};

const UNKNOWN_APP: LinkedAppIdentity = { name: null, logoUrl: null };

export function linkedAppIdentity(code: string): LinkedAppIdentity {
  return KNOWN_APPS[code] ?? UNKNOWN_APP;
}
