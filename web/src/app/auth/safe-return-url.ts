/**
 * N'accepte qu'une URL interne à l'application (`/chemin`), jamais une URL absolue,
 * protocolaire (`//hote`) ou ambiguë (`/\hote`) pouvant mener hors de l'application.
 */
export function safeInternalUrl(candidate: string | null | undefined): string | null {
  const url = candidate?.trim();
  if (!url || !url.startsWith('/')) return null;
  if (url.startsWith('//') || url.startsWith('/\\') || url.includes('\\')) return null;
  if (/[\u0000-\u001F\u007F]/.test(url)) return null;
  return url;
}
