import { legalRoutes } from './legal.routes';

describe('legalRoutes', () => {
  it('should expose all legal documents without an authentication guard', () => {
    expect(legalRoutes.map(route => route.path)).toEqual([
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
