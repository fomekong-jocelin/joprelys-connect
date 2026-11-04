/** Valeurs acceptées dans un message traduit paramétré (`{nom}`). */
export type InterpolationParams = Readonly<Record<string, string | number>>;

/**
 * Remplace les jetons `{nom}` d'un message traduit par les valeurs fournies.
 * Un jeton sans valeur correspondante est conservé tel quel pour rester visible en recette.
 */
export function interpolate(template: string, params: InterpolationParams): string {
  return template.replace(/\{(\w+)\}/g, (token, name: string) =>
    Object.prototype.hasOwnProperty.call(params, name) ? String(params[name]) : token,
  );
}
