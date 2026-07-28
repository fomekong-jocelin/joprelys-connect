import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenStorageService } from './auth-token-storage.service';

/**
 * Guard preventing logged-in users from seeing or flashing the login page,
 * routing them directly to their respective dashboard (or patient dashboard).
 */
export const loginGuard: CanActivateFn = (route, state) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const router = inject(Router);

  const session = tokenStorage.session();
  if (session) {
    const isPatient = tokenStorage.isPatientSession(session);
    const mode = route.queryParams['mode'] || route.data?.['loginMode'];

    // Allow explicit mode switching in same tab
    if (isPatient && mode === 'staff') {
      return true;
    }
    if (!isPatient && mode === 'patient') {
      return true;
    }

    return router.parseUrl(isPatient ? '/patient/dashboard' : '/dashboard');
  }

  return true;
};
