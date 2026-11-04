/** Le serveur exige un motif de bris de glace de 10 à 500 caractères (PatientController, SEC-02). */
export const EMERGENCY_REASON_MIN_LENGTH = 10;
export const EMERGENCY_REASON_MAX_LENGTH = 500;

export function isEmergencyReasonValid(reason: string): boolean {
  const length = reason.trim().length;
  return length >= EMERGENCY_REASON_MIN_LENGTH && length <= EMERGENCY_REASON_MAX_LENGTH;
}
