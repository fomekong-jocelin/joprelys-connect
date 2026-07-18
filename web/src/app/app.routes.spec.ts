import { Route } from '@angular/router';
import { routes } from './app.routes';
import { roleGuard } from './auth/role.guard';
import { PROFESSIONAL_ACCESS_POLICIES } from './auth/professional-access-policies';

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

  it('should keep global laboratory and billing routes aligned with their shared menu policies', () => {
    const labRoute = guardedRoutes.find((route) => route.path === 'clinic/lab-orders');
    const billingRoute = guardedRoutes.find((route) => route.path === 'clinic/billing');

    expect(labRoute?.data?.['expectedPermissions']).toEqual([
      ...PROFESSIONAL_ACCESS_POLICIES.labQueue,
    ]);
    expect(billingRoute?.data?.['expectedPermissions']).toEqual([
      ...PROFESSIONAL_ACCESS_POLICIES.billingWorkspace,
    ]);
  });
});

function flatten(source: Route[]): Route[] {
  return source.flatMap((route) => [route, ...flatten(route.children ?? [])]);
}
