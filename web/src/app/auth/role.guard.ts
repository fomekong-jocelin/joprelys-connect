import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, isObservable, map, of, switchMap } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { AuthSession } from './auth.models';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { canAccessBillingManagement } from './professional-access-policies';

export const roleGuard: CanActivateFn = (route, state) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const sessionRecovery = inject(AuthSessionRecoveryService);
  const rbacApi = inject(RbacApiService);
  const router = inject(Router);

  const expectedRoles = (route.data['expectedRoles'] as string[] | undefined) ?? [];
  const expectedPermissions = (route.data['expectedPermissions'] as string[] | undefined) ?? [];
  const expectsPatient = expectedRoles.includes('PATIENT');
  const routePath = route.routeConfig?.path ?? '';
  const isBillingManagementRoute = routePath === 'clinic/billing'
    || routePath === 'clinic/billing/invoice/:invoiceId';
  const isInternalEntryRoute = routePath === 'dashboard'
    || (routePath === 'profile' && !expectsPatient);
  const allowAnyInternalRole = route.data['allowAnyInternalRole'] === true || isInternalEntryRoute;
  const professionalLoginTree = () => router.createUrlTree(['/'], { queryParams: { returnUrl: state.url } });
  const patientLoginTree = () => router.createUrlTree(['/'], {
    queryParams: { mode: 'patient', returnUrl: state.url },
  });

  const authorizeSession = (session: AuthSession) => {
    const isPatientSession = tokenStorage.isPatientSession(session);

    if (isPatientSession) {
      return expectsPatient
        ? true
        : router.parseUrl('/unauthorized');
    }

    // A valid professional session in this tab must not be mistaken for patient
    // authentication. Send the user to the explicit patient login mode instead of
    // importing/reusing clinician credentials on a patient route.
    if (expectsPatient) {
      return patientLoginTree();
    }

    if (expectedPermissions.length > 0 || allowAnyInternalRole || isBillingManagementRoute) {
      return rbacApi.ensureMyAccess(true).pipe(
        map((access) => {
          const permissions = new Set(access.permissions);
          if (isBillingManagementRoute && !canAccessBillingManagement(permissions)) {
            return router.parseUrl('/unauthorized');
          }
          if (expectedPermissions.length > 0) {
            const hasPermission = access.permissions.some((permission) => expectedPermissions.includes(permission));
            return hasPermission ? true : router.parseUrl('/unauthorized');
          }
          if (allowAnyInternalRole) {
            const hasInternalRole = access.roles.some((role) => role !== 'PATIENT');
            if (hasInternalRole) {
              return true;
            }
          }
          return isBillingManagementRoute ? true : router.parseUrl('/unauthorized');
        }),
        catchError(() => of(router.parseUrl('/unauthorized'))),
      );
    }

    return expectedRoles.length > 0
      ? router.parseUrl('/unauthorized')
      : true;
  };

  const session = tokenStorage.session();
  if (session) {
    return authorizeSession(session);
  }

  // Patient authentication is deliberately JWT/OTP only. Never attempt the
  // professional refresh cookie while entering a patient route.
  if (expectsPatient || state.url.startsWith('/patient/')) {
    return patientLoginTree();
  }

  // sessionStorage is per-tab while the professional refresh cookie is HttpOnly
  // and shared by the origin. A professional tab may recover before login.
  return sessionRecovery.refreshAccessToken().pipe(
    switchMap(() => {
      const restored = tokenStorage.session();
      if (!restored) return of(professionalLoginTree());
      const decision = authorizeSession(restored);
      return isObservable(decision) ? decision : of(decision);
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        return of(professionalLoginTree());
      }
      return of(true);
    }),
  );
};
