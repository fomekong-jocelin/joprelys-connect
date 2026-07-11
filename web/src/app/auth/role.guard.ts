import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const rbacApi = inject(RbacApiService);
  const router = inject(Router);
  const session = tokenStorage.session();

  if (!session) {
    return router.parseUrl('/');
  }

  const expectedRoles = (route.data['expectedRoles'] as string[] | undefined) ?? [];
  const expectedPermissions = (route.data['expectedPermissions'] as string[] | undefined) ?? [];
  const userRoles = session.role.split(',').map((role) => role.trim()).filter(Boolean);
  const hasLegacyRole = expectedRoles.length === 0 || userRoles.some((role) => expectedRoles.includes(role));

  if (hasLegacyRole && expectedRoles.length > 0) {
    return true;
  }

  if (expectedPermissions.length > 0) {
    return rbacApi.ensureMyAccess().pipe(
      map((access) => access.permissions.some((permission) => expectedPermissions.includes(permission))
        ? true
        : router.parseUrl('/unauthorized')),
      catchError(() => of(router.parseUrl('/unauthorized'))),
    );
  }

  if (expectedRoles.length > 0) {
    return router.parseUrl('/unauthorized');
  }

  return true;
};
