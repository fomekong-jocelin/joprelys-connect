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
  if (expectedRoles && !expectedRoles.includes(session.role)) {
    return router.parseUrl('/unauthorized');
  }

  return true;
};
