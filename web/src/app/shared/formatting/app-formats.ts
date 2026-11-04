/**
 * Formats d'affichage centralisés (contexte Cameroun / zone CEMAC).
 * Les écrans ne doivent pas utiliser les formats dépendants de la locale (`short`, `medium`…)
 * qui produisent des dates à l'américaine selon la configuration du navigateur.
 */
export const APP_DATE_FORMAT = 'dd/MM/yyyy';
export const APP_DATE_TIME_FORMAT = 'dd/MM/yyyy HH:mm';

/** Espace insécable : sépare les milliers et le montant de sa devise sans retour à la ligne. */
export const NO_BREAK_SPACE = ' ';
export const XAF_CURRENCY_LABEL = 'FCFA';
export const EMPTY_DISPLAY_VALUE = '—';

/** Formate un montant en francs CFA : entier arrondi, milliers séparés par une espace insécable. */
export function formatXaf(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return EMPTY_DISPLAY_VALUE;
  const amount = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(amount)) return EMPTY_DISPLAY_VALUE;

  const rounded = Math.round(amount);
  const sign = rounded < 0 ? '-' : '';
  const digits = Math.abs(rounded).toString().replace(/\B(?=(\d{3})+(?!\d))/g, NO_BREAK_SPACE);
  return `${sign}${digits}${NO_BREAK_SPACE}${XAF_CURRENCY_LABEL}`;
}
