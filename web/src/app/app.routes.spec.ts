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

  it('should redirect the audit shortcut to the permission audit and provide a distinct interop page', () => {
    const auditRoute = routes.find((route) => route.path === 'audit-trail');
    expect(auditRoute?.redirectTo).toBe('clinic/rbac?tab=audit');
    expect(auditRoute?.pathMatch).toBe('full');
    const interopRoute = routes.find((route) => route.path === 'interop');
    expect(interopRoute?.loadComponent).toBeDefined();
    expect(interopRoute?.canActivate).toContain(roleGuard);
    expect(interopRoute?.data?.['expectedPermissions']).toContain('ORGANIZATION_MANAGE');
  });

  it('should end with an unguarded wildcard 404 route and expose the server unavailable state publicly', () => {
    const wildcard = routes[routes.length - 1];
    expect(wildcard.path).toBe('**');
    expect(wildcard.loadComponent).toBeDefined();
    expect(wildcard.canActivate).toBeUndefined();
    const serverUnavailable = routes.find((route) => route.path === 'server-unavailable');
    expect(serverUnavailable?.loadComponent).toBeDefined();
    expect(serverUnavailable?.canActivate).toBeUndefined();
  });
});

function flatten(source: Route[]): Route[] {
  return source.flatMap((route) => [route, ...flatten(route.children ?? [])]);
}
