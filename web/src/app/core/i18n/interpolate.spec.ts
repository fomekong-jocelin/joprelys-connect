import { interpolate } from './interpolate';

describe('interpolate', () => {
  it('remplace chaque jeton par sa valeur', () => {
    expect(interpolate('Lit {bed} — {count} disponibles', { bed: 'A-12', count: 3 })).toBe('Lit A-12 — 3 disponibles');
  });

  it('conserve un jeton sans valeur pour rester visible', () => {
    expect(interpolate('Bonjour {name}', {})).toBe('Bonjour {name}');
  });

  it('ignore les propriétés héritées du prototype', () => {
    expect(interpolate('{toString}', {})).toBe('{toString}');
  });
});
