import { formatDate, formatNumber } from '@angular/common';
import { resolveAppLocaleId } from './app.config';

describe('appConfig locale', () => {
  it('aligne la locale Angular sur la langue applicative, le français par défaut', () => {
    expect(resolveAppLocaleId('en')).toBe('en');
    expect(resolveAppLocaleId('fr')).toBe('fr');
    expect(resolveAppLocaleId('de')).toBe('fr');
  });

  it('enregistre les données de locale françaises pour les pipes', () => {
    expect(formatNumber(1234.5, 'fr', '1.1-1')).toBe('1 234,5');
    expect(formatDate(new Date(2026, 10, 4), 'd MMMM y', 'fr')).toBe('4 novembre 2026');
  });
});
