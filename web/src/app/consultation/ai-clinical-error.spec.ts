import {
  clinicalAiErrorMessage,
  extractClinicalAiErrorCode,
} from './ai-clinical-error';

describe('clinical AI errors', () => {
  it('extracts a nested backend error code', () => {
    expect(extractClinicalAiErrorCode({
      error: { error: { message: '422 AI_OUTPUT_INVALID' } },
    })).toBe('AI_OUTPUT_INVALID');
  });

  it('shows a precise silence message instead of a generic report error', () => {
    const message = clinicalAiErrorMessage(
      'AI_AUDIO_SILENCE',
      'fr',
      'Erreur générique',
    );

    expect(message).toContain('Aucune parole');
    expect(message).not.toBe('Erreur générique');
  });

  it('keeps the transcript recovery instruction for invalid output', () => {
    const message = clinicalAiErrorMessage(
      'AI_OUTPUT_INVALID',
      'fr',
      'Erreur générique',
    );

    expect(message).toContain('transcription reste sauvegardée');
    expect(message).toContain('corrigez');
  });

  it('uses the fallback for an unknown code', () => {
    expect(clinicalAiErrorMessage('AI_UNKNOWN', 'fr', 'Repli')).toBe('Repli');
  });
});
