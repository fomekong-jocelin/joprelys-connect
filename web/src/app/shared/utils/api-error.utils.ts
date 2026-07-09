/**
 * Extrait un message d'erreur lisible d'une réponse HTTP Angular (HttpErrorResponse).
 *
 * Règles :
 * - Priorité au message structuré renvoyé par le backend (format { error: { message } }).
 * - Jamais de message brut technique Angular du type "Http failure response for ...".
 * - Fallback fourni par l'appelant si le backend ne renvoie rien d'exploitable.
 */
export function extractApiErrorMessage(err: any): string | null {
  if (!err) {
    return null;
  }

  // Format normalisé du backend : { error: { code, message, trace_id } }
  const backendMessage = err.error?.error?.message || err.error?.message;
  if (typeof backendMessage === 'string' && backendMessage.trim().length > 0) {
    const normalized = backendMessage.trim();
    if (!normalized.startsWith('Http failure response')) {
      return normalized;
    }
  }

  // Sécurité supplémentaire : si err.message (propre à Angular) contenait le message brut,
  // on ne le propage jamais à l'utilisateur.
  return null;
}
