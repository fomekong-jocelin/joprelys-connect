import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

export const roleGuard: CanActivateFn = (route) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const authApi = inject(AuthApiService);
  const router = inject(Router);
  const session = tokenStorage.session();

  if (!session) {
    return router.parseUrl('/');
  }

  const expectedRoles = route.data['expectedRoles'] as string[] | undefined;
  const authorize = (rawRoles: string): true | UrlTree => {
    if (!expectedRoles) {
      return true;
    }
    const userRoles = rawRoles.split(',').map((role) => role.trim()).filter(Boolean);
    return userRoles.some((role) => expectedRoles.includes(role))
      ? true
      : router.parseUrl('/unauthorized');
  };

  if (session.role.split(',').map((role) => role.trim()).includes('PATIENT')) {
    return authorize(session.role);
  }

  return authApi.refreshCurrentSession().pipe(
    map((currentSession) => authorize(currentSession.role)),
    catchError(() => {
      tokenStorage.clear();
      return of(router.parseUrl('/'));
    }),
  );
};
