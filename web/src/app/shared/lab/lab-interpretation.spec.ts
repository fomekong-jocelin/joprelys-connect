import { describe, expect, it } from 'vitest';
import { labInterpretationKey, labInterpretationTone, toLabInterpretation } from './lab-interpretation';

describe('lab interpretation', () => {
  it('never presents a missing or unknown interpretation as normal', () => {
    expect(toLabInterpretation(undefined)).toBe('NOT_INTERPRETED');
    expect(toLabInterpretation('')).toBe('NOT_INTERPRETED');
    expect(toLabInterpretation('ELEVE')).toBe('NOT_INTERPRETED');
    expect(labInterpretationTone(null)).toBe('unknown');
    expect(labInterpretationTone('CRITIQUE')).toBe('unknown');
  });

  it('maps backend values to their label key and tone', () => {
    expect(labInterpretationKey('high')).toBe('lab.interpretation.HIGH');
    expect(labInterpretationTone('NORMAL')).toBe('normal');
    expect(labInterpretationTone('LOW')).toBe('outside');
    expect(labInterpretationTone('ABNORMAL')).toBe('outside');
    expect(labInterpretationTone('CRITICAL')).toBe('critical');
  });
});
