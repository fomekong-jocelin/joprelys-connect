import { safeInternalUrl } from './safe-return-url';

describe('safeInternalUrl', () => {
  it('accepte un chemin interne avec paramètres', () => {
    expect(safeInternalUrl('/clinic/consultation/visit-1?tab=notes')).toBe('/clinic/consultation/visit-1?tab=notes');
  });

  it('refuse les URL absolues, protocolaires ou ambiguës', () => {
    expect(safeInternalUrl('https://evil.example')).toBeNull();
    expect(safeInternalUrl('//evil.example')).toBeNull();
    expect(safeInternalUrl('/\\evil.example')).toBeNull();
    expect(safeInternalUrl('/ok\\..\\evil')).toBeNull();
    expect(safeInternalUrl('/path\nheader')).toBeNull();
    expect(safeInternalUrl('javascript:alert(1)')).toBeNull();
  });

  it('refuse une valeur vide ou absente', () => {
    expect(safeInternalUrl('')).toBeNull();
    expect(safeInternalUrl('   ')).toBeNull();
    expect(safeInternalUrl(null)).toBeNull();
    expect(safeInternalUrl(undefined)).toBeNull();
  });
});
