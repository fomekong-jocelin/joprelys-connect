import { TestBed } from '@angular/core/testing';
import { AppDatePipe } from './app-date.pipe';
import { formatXaf } from './app-formats';
import { XafPipe } from './xaf.pipe';

describe('formatXaf / XafPipe', () => {
  it('arrondit et sépare les milliers par des espaces insécables', () => {
    expect(formatXaf(1250000.6)).toBe('1 250 001 FCFA');
    expect(new XafPipe().transform(12500)).toBe('12 500 FCFA');
  });

  it('gère zéro, les petits montants et les montants négatifs', () => {
    expect(formatXaf(0)).toBe('0 FCFA');
    expect(formatXaf(999)).toBe('999 FCFA');
    expect(formatXaf(-15000)).toBe('-15 000 FCFA');
  });

  it('accepte une chaîne numérique et affiche un tiret si la valeur est absente ou invalide', () => {
    expect(formatXaf('2500')).toBe('2 500 FCFA');
    expect(formatXaf(null)).toBe('—');
    expect(formatXaf(undefined)).toBe('—');
    expect(formatXaf('abc')).toBe('—');
  });
});

describe('AppDatePipe', () => {
  function pipe(): AppDatePipe {
    return TestBed.runInInjectionContext(() => new AppDatePipe());
  }

  it('formate une date au format jj/MM/aaaa', () => {
    expect(pipe().transform(new Date(2026, 10, 4, 14, 5))).toBe('04/11/2026');
  });

  it('formate une date et heure au format jj/MM/aaaa HH:mm (24 h)', () => {
    expect(pipe().transform(new Date(2026, 10, 4, 14, 5), 'dateTime')).toBe('04/11/2026 14:05');
  });

  it("n'affiche rien pour une valeur absente et un tiret pour une date invalide", () => {
    expect(pipe().transform(null)).toBe('');
    expect(pipe().transform('')).toBe('');
    expect(pipe().transform('pas-une-date')).toBe('—');
  });
});
