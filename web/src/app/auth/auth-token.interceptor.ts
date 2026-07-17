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
    return next(request);
  }

  const sendWithToken = (token: string) => next(withBearerToken(request, token));
  const refreshAndRetry = () => sessionRecovery.refreshAccessToken().pipe(
    switchMap((freshToken) => sendWithToken(freshToken)),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
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
      if (error.status !== 401) {
        return throwError(() => error);
      }
      return refreshAndRetry();
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
