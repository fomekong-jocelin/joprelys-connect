import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenStorageService } from './auth-token-storage.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const router = inject(Router);
  const session = tokenStorage.session();

  if (!session) {
    return router.parseUrl('/');
  }

  const expectedRoles = route.data['expectedRoles'] as string[];
  if (expectedRoles) {
    const userRoles = session.role.split(',').map((r) => r.trim());
    const hasRole = userRoles.some((r) => expectedRoles.includes(r));
    if (!hasRole) {
      return router.parseUrl('/unauthorized');
    }
  }

  return true;
};
