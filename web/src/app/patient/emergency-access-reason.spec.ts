import { describe, expect, it } from 'vitest';
import { isEmergencyReasonValid } from './emergency-access-reason';

describe('isEmergencyReasonValid', () => {
  it('refuse un motif vide ou trop court, même entouré d’espaces', () => {
    expect(isEmergencyReasonValid('')).toBe(false);
    expect(isEmergencyReasonValid('   urgence   ')).toBe(false);
  });

  it('accepte un motif circonstancié de 10 à 500 caractères', () => {
    expect(isEmergencyReasonValid('Patient inconscient aux urgences')).toBe(true);
    expect(isEmergencyReasonValid('x'.repeat(501))).toBe(false);
  });
});
