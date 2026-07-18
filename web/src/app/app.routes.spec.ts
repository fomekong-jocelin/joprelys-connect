import { Route } from '@angular/router';
import { routes } from './app.routes';
import { roleGuard } from './auth/role.guard';

describe('application route role boundaries', () => {
  const guardedRoutes = flatten(routes).filter((route) => route.canActivate?.includes(roleGuard));

  it('should reserve every patient route to the patient role', () => {
    const patientRoutes = guardedRoutes.filter((route) => route.path?.startsWith('patient/'));

    expect(patientRoutes.length).toBeGreaterThan(0);
    for (const route of patientRoutes) {
      expect(route.data?.['expectedRoles']).toEqual(['PATIENT']);
    }
  });

  it('should authorize every professional guarded route with permissions only', () => {
    const professionalRoutes = guardedRoutes.filter((route) => !route.path?.startsWith('patient/'));

    for (const route of professionalRoutes) {
      expect(route.data?.['expectedRoles']).toBeUndefined();
      const permissions = route.data?.['expectedPermissions'] as string[] | undefined;
      const internalEntry = route.data?.['allowAnyInternalRole'] === true
        || route.path === 'dashboard';
      expect((permissions?.length ?? 0) > 0 || internalEntry).toBe(true);
    }
  });
});

function flatten(source: Route[]): Route[] {
  return source.flatMap((route) => [route, ...flatten(route.children ?? [])]);
}
