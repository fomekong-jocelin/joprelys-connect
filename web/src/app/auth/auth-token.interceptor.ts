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

  const sendWithToken = (token: string) => next(withBearerToken(request, token));
  const refreshAndRetry = (rejectedToken?: string) =>
    sessionRecovery.refreshAccessToken(rejectedToken).pipe(
      switchMap((freshToken) => sendWithToken(freshToken)),
      catchError((error: HttpErrorResponse) => {
        // Only a positive authentication refusal means the browser session is no
        // longer usable. Network errors, server outages and concurrent-refresh
        // conflicts must never be converted into a clinician logout.
        if (error.status === 401 || error.status === 403) {
          sessionRecovery.expireSession();
        }
        return throwError(() => error);
      }),
    );

  const session = tokenStorage.session();
  if (!session) {
    // A missing access token is not proof that the persistent session expired.
    // This occurs naturally in a fresh tab, after browser memory pressure or when
    // bootstrap refresh was temporarily unavailable. Recover from the HttpOnly
    // refresh cookie before deciding to log the user out.
    return refreshAndRetry();
  }

  const recoverRejectedToken = (rejectedToken: string) => {
    const latest = tokenStorage.session();
    if (latest
      && latest.accessToken !== rejectedToken
      && !tokenStorage.isExpired()) {
      // Another request or browser tab already refreshed the session while this
      // request was in flight. Reuse that token instead of rotating the shared
      // HttpOnly refresh cookie again.
      return sendWithToken(latest.accessToken).pipe(
        catchError((retryError: HttpErrorResponse) => {
          if (retryError.status === 401) {
            return refreshAndRetry(latest.accessToken);
          }
          return throwError(() => retryError);
        }),
      );
    }
    return refreshAndRetry(rejectedToken);
  };

  if (tokenStorage.isExpired()) {
    return refreshAndRetry(session.accessToken);
  }

  return sendWithToken(session.accessToken).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 means the bearer token is no longer accepted and may be refreshed.
      // 403 is an authorization refusal: refreshing the token cannot grant a
      // permission and, critically, must never log the user out.
      if (error.status === 401) {
        return recoverRejectedToken(session.accessToken);
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
