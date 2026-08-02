import { formatClinicalValue } from './ai-clinical-value-formatter';

describe('formatClinicalValue', () => {
  it('formats a prescription without exposing JSON keys', () => {
    const formatted = formatClinicalValue(
      'prescription',
      '[{"drugName":"Doliprane","dosage":"100 mg","posology":"deux comprimés par prise","frequency":"matin, midi et soir","duration":"deux jours"}]',
      'fr',
    );

    expect(formatted).toContain('Doliprane');
    expect(formatted).toContain('Dosage : 100 mg');
    expect(formatted).toContain('Posologie : deux comprimés par prise');
    expect(formatted).toContain('Fréquence : matin, midi et soir');
    expect(formatted).toContain('Durée : deux jours');
    expect(formatted).not.toContain('drugName');
    expect(formatted).not.toContain('{');
    expect(formatted).not.toContain('}');
  });

  it('separates multiple medications into readable blocks', () => {
    const formatted = formatClinicalValue(
      'prescription',
      '[{"drugName":"Paracétamol","dosage":"500 mg"},{"drugName":"Amoxicilline","frequency":"trois fois par jour"}]',
      'fr',
    );

    expect(formatted).toContain('Paracétamol');
    expect(formatted).toContain('Amoxicilline');
    expect(formatted).toContain('\n\n');
  });

  it('preserves an invalid legacy value instead of hiding it', () => {
    const raw = '{drugName: Doliprane, dosage: 100 mg}';
    expect(formatClinicalValue('prescription', raw, 'fr')).toBe(raw);
  });
});
