import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthSession } from './auth.models';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const tokenStorage = inject(AuthTokenStorageService);
  const sessionRecovery = inject(AuthSessionRecoveryService);
  const router = inject(Router);

  if (!isProtectedApiRequest(request.url)) {
    return next(request);
  }

  const sendWithToken = (token: string) => next(withBearerToken(request, token));
  const refreshAndRetry = (rejectedToken?: string) =>
    sessionRecovery.refreshAccessToken(rejectedToken).pipe(
      switchMap((freshToken) => sendWithToken(freshToken)),
      catchError((error: HttpErrorResponse) => {
        // Only a positive authentication refusal means the professional browser
        // session is no longer usable. Patient requests never enter this path.
        if (error.status === 401 || error.status === 403) {
          sessionRecovery.expireSession();
        }
        return throwError(() => error);
      }),
    );

  const session = tokenStorage.session();
  const patientContext = isPatientContext(session, request.url, router.url, tokenStorage);

  if (!session) {
    if (patientContext) {
      // A patient session is OTP/JWT based and intentionally has no persistent
      // professional refresh. Do not exchange the clinician cookie on its behalf.
      sessionRecovery.expireSession();
      return throwError(() => unauthenticated(request.url));
    }

    // A missing professional access token is not proof that the persistent
    // professional session expired. Recover from the HttpOnly refresh cookie.
    return refreshAndRetry();
  }

  if (tokenStorage.isPatientSession(session)) {
    if (tokenStorage.isExpired()) {
      sessionRecovery.expireSession();
      return throwError(() => unauthenticated(request.url));
    }

    return sendWithToken(session.accessToken).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          sessionRecovery.expireSession();
        }
        return throwError(() => error);
      }),
    );
  }

  const recoverRejectedToken = (rejectedToken: string) => {
    const latest = tokenStorage.session();
    if (latest
      && !tokenStorage.isPatientSession(latest)
      && latest.accessToken !== rejectedToken
      && !tokenStorage.isExpired()) {
      // Another professional request or browser tab already refreshed the session
      // while this request was in flight. Reuse that professional token.
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
      // 401 means the professional bearer token is no longer accepted and may be
      // refreshed. 403 remains an authorization refusal and never triggers logout.
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

function isPatientContext(
  session: AuthSession | null,
  requestUrl: string,
  routerUrl: string,
  tokenStorage: AuthTokenStorageService,
): boolean {
  return tokenStorage.isPatientSession(session)
    || routerUrl.startsWith('/patient/')
    || requestUrl.startsWith('/api/patient/');
}

function unauthenticated(url: string): HttpErrorResponse {
  return new HttpErrorResponse({
    error: 'Unauthenticated',
    status: 401,
    statusText: 'Unauthorized',
    url,
  });
}

function withBearerToken(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}
