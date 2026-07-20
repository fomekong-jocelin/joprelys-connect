import { legalRoutes } from './legal.routes';

describe('legalRoutes', () => {
  it('should expose all public legal and privacy routes without an authentication guard', () => {
    expect(legalRoutes.map(route => route.path)).toEqual([
      'legal/privacy-preferences',
      'legal/privacy',
      'legal/terms',
      'legal/legal-notice',
      'legal/cookies',
      'legal/health-data',
      'legal/retention',
      'legal/rights',
    ]);

    for (const route of legalRoutes) {
      expect(route.canActivate).toBeUndefined();
      expect(route.loadComponent).toBeTypeOf('function');
    }
  });
});
