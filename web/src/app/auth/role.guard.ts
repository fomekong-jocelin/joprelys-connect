import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

export const roleGuard: CanActivateFn = (route) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const rbacApi = inject(RbacApiService);
  const router = inject(Router);
  const session = tokenStorage.session();

  if (!session) {
    return router.parseUrl('/');
  }

  const expectedRoles = (route.data['expectedRoles'] as string[] | undefined) ?? [];
  const expectedPermissions = (route.data['expectedPermissions'] as string[] | undefined) ?? [];
  const legacyRoles = session.role.split(',').map((role) => role.trim()).filter(Boolean);

  if (expectedPermissions.length > 0) {
    return rbacApi.ensureMyAccess(true).pipe(
      map((access) => {
        const hasPermission = access.permissions.some((permission) => expectedPermissions.includes(permission));
        const hasEffectiveRole = access.roles.some((role) => expectedRoles.includes(role));
        return hasPermission || hasEffectiveRole ? true : router.parseUrl('/unauthorized');
      }),
      catchError(() => of(router.parseUrl('/unauthorized'))),
    );
  }

  if (expectedRoles.length > 0) {
    return legacyRoles.some((role) => expectedRoles.includes(role))
      ? true
      : router.parseUrl('/unauthorized');
  }

  return true;
};
