/** Même règle que le serveur (`DefaultHospitalOrganizationService.CODE_PATTERN`) : il reste l'autorité. */
const CODE_PATTERN = /^[A-Z][A-Z0-9_]{2,63}$/;
const CODE_MAX_LENGTH = 64;
const SUFFIX_RESERVE = 4;

/** Normalisation identique à celle du serveur : majuscules, espaces et tirets convertis en `_`. */
export function normalizeUnitCode(raw: string): string {
  return raw.trim().toUpperCase().replace(/[ -]/g, '_');
}

export function isValidUnitCode(raw: string): boolean {
  return CODE_PATTERN.test(normalizeUnitCode(raw));
}

/**
 * Propose un code lisible et libre à partir d'un nom (« Médecine interne » → `MEDECINE_INTERNE`),
 * suffixé `_2`, `_3`… s'il est déjà pris parmi les unités connues de l'écran.
 */
export function suggestUnitCode(source: string, takenCodes: ReadonlySet<string>): string {
  const stem = codeStem(source);
  if (!stem) return '';
  if (!takenCodes.has(stem)) return stem;
  for (let index = 2; ; index += 1) {
    const candidate = `${stem}_${index}`;
    if (!takenCodes.has(candidate)) return candidate;
  }
}

function codeStem(source: string): string {
  const ascii = source
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
  if (!ascii) return '';
  const startsWithLetter = /^[A-Z]/.test(ascii) ? ascii : `U_${ascii}`;
  const longEnough = startsWithLetter.length >= 3 ? startsWithLetter : `${startsWithLetter}_UNIT`;
  return longEnough.slice(0, CODE_MAX_LENGTH - SUFFIX_RESERVE).replace(/_+$/, '');
}
