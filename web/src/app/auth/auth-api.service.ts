import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, finalize, map, Observable, of, tap, throwError } from 'rxjs';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginRequest, LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly apiBaseUrl = '/api/auth';

  constructor(
    private readonly http: HttpClient,
    private readonly tokenStorage: AuthTokenStorageService,
    private readonly sessionRecovery: AuthSessionRecoveryService,
  ) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiBaseUrl}/login`, request, { withCredentials: true }).pipe(
      tap((response) => {
        if (!response.requiresOtp) {
          this.tokenStorage.save(response);
        }
      }),
    );
  }

  verifyStaffOtp(email: string, otpCode: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiBaseUrl}/verify-otp`,
      { email, otpCode },
      { withCredentials: true },
    ).pipe(
      tap((response) => this.tokenStorage.save(response)),
    );
  }

  restoreSession(): Observable<void> {
    const currentSession = this.tokenStorage.session();
    if (currentSession && !this.tokenStorage.isExpired()) {
      return of(undefined);
    }

    return this.sessionRecovery.refreshAccessToken(currentSession?.accessToken).pipe(
      map(() => undefined),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          this.tokenStorage.clear();
          return of(undefined);
        }
        return throwError(() => error);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/logout`, {}, { withCredentials: true }).pipe(
      finalize(() => this.tokenStorage.clear()),
    );
  }

  requestPasswordRecovery(email: string): Observable<void> {
    return this.http.post<void>('/api/public/auth/password-recovery/request', { email });
  }

  resetPassword(request: any): Observable<void> {
    return this.http.post<void>('/api/public/auth/password-recovery/reset', request);
  }
}
