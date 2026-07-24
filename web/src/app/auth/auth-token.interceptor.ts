import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const sessionRecovery = inject(AuthSessionRecoveryService);

  if (!isProtectedApiRequest(request.url)) {
    return next(request);
  }

  const session = tokenStorage.session();
  if (!session) {
    sessionRecovery.expireSession();
    return throwError(
      () =>
        new HttpErrorResponse({
          error: 'Unauthenticated',
          status: 401,
          statusText: 'Unauthorized',
          url: request.url,
        }),
    );
  }

  const sendWithToken = (token: string) => next(withBearerToken(request, token));
  const refreshAndRetry = () =>
    sessionRecovery.refreshAccessToken().pipe(
      switchMap((freshToken) => sendWithToken(freshToken)),
      catchError((error: HttpErrorResponse) => {
        // A rejected refresh means the authentication session is no longer usable.
        // Server/network errors must remain visible and must not be reclassified as
        // an expired session.
        if (error.status === 401 || error.status === 403) {
          sessionRecovery.expireSession();
        }
        return throwError(() => error);
      }),
    );

  if (tokenStorage.isExpired()) {
    return refreshAndRetry();
  }

  return sendWithToken(session.accessToken).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 means the bearer token is no longer accepted and may be refreshed.
      // 403 is an authorization refusal: refreshing the token cannot grant a
      // permission and, critically, must never log the user out.
      if (error.status === 401) {
        return refreshAndRetry();
      }
      return throwError(() => error);
    }),
  );
};

function isProtectedApiRequest(url: string): boolean {
  return url.startsWith('/api/')
    && !url.startsWith('/api/auth/')
    && !url.startsWith('/api/public/');
}

function withBearerToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}
