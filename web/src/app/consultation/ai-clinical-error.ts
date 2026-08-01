export type ClinicalAiErrorLocale = 'fr' | 'en';

export function extractClinicalAiErrorCode(error: unknown): string | null {
  const candidates = errorCandidates(error);
  for (const candidate of candidates) {
    const match = candidate.match(/\bAI_[A-Z0-9_]+\b/);
    if (match) return match[0];
  }
  return null;
}

export function clinicalAiErrorMessage(
  code: string | null,
  locale: ClinicalAiErrorLocale,
  fallback: string,
): string {
  const french = locale === 'fr';
  return switchError(code, french) ?? fallback;
}

function switchError(code: string | null, french: boolean): string | null {
  switch (code) {
    case 'AI_AUDIO_SILENCE':
      return french
        ? 'Aucune parole n’a été détectée. Reprenez l’enregistrement en parlant près du microphone.'
        : 'No speech was detected. Record again while speaking near the microphone.';
    case 'AI_CAPTURE_EMPTY':
      return french
        ? 'Aucune transcription exploitable n’est disponible. Enregistrez ou conservez au moins un passage avant de générer le compte rendu.'
        : 'No usable transcript is available. Record or keep at least one passage before generating the clinical note.';
    case 'AI_TRANSCRIPT_REVIEW_REQUIRED':
      return french
        ? 'Une transcription attend votre relecture. Vérifiez-la avant de poursuivre.'
        : 'A transcript is waiting for review. Check it before continuing.';
    case 'AI_CHANGE_INVALID':
    case 'AI_OUTPUT_INVALID':
      return french
        ? 'Certaines données n’ont pas pu être structurées. La transcription reste sauvegardée : corrigez le passage concerné puis relancez le compte rendu.'
        : 'Some data could not be structured. The transcript remains saved: correct the affected passage and generate the note again.';
    case 'AI_SESSION_EXPIRED':
      return french
        ? 'La session IA a expiré. Rechargez la transcription sauvegardée avant de relancer le compte rendu.'
        : 'The AI session expired. Reload the saved transcript before generating the note again.';
    case 'AI_CLARIFICATION_INVALID':
      return french
        ? 'La demande de précision reçue n’est pas exploitable. Corrigez directement la transcription puis relancez l’analyse.'
        : 'The clarification request is not usable. Correct the transcript directly and run the analysis again.';
    default:
      return null;
  }
}

function errorCandidates(error: unknown): string[] {
  if (!error || typeof error !== 'object') return typeof error === 'string' ? [error] : [];
  const source = error as Record<string, unknown>;
  const payload = isRecord(source['error']) ? source['error'] : null;
  const envelope = payload && isRecord(payload['error']) ? payload['error'] : null;
  return [
    envelope?.['message'],
    payload?.['detail'],
    payload?.['title'],
    payload?.['message'],
    source['message'],
  ].filter((value): value is string => typeof value === 'string' && !!value.trim());
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}
