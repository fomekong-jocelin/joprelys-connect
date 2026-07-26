import { DOCUMENT } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { catchError, finalize, map, Observable, of, tap } from 'rxjs';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginRequest, LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly apiBaseUrl = '/api/auth';
  private readonly document = inject(DOCUMENT);

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

    // A patient JWT has no professional refresh lifecycle. When it expires, keep
    // the tab in patient context and require a new patient OTP instead of silently
    // restoring the clinician identity from the shared professional cookie.
    if (currentSession && this.tokenStorage.isPatientSession(currentSession)) {
      if (this.tokenStorage.isExpired()) {
        this.tokenStorage.clear();
      }
      return of(undefined);
    }

    // Patient routes/login mode are an explicit identity boundary. A fresh patient
    // tab must never import the professional session merely because the browser
    // also owns the professional HttpOnly refresh cookie.
    if (!currentSession && this.isPatientRoute()) {
      return of(undefined);
    }

    if (currentSession && !this.tokenStorage.isExpired()) {
      return of(undefined);
    }

    return this.sessionRecovery.refreshAccessToken(currentSession?.accessToken).pipe(
      map(() => undefined),
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          this.tokenStorage.clear();
        }
        // A network outage, 409 refresh race or 5xx auth outage must never abort
        // Angular bootstrap and produce a blank application. Preserve the local
        // state and let subsequent protected requests recover when connectivity
        // returns.
        return of(undefined);
      }),
    );
  }

  logout(): Observable<void> {
    if (!this.tokenStorage.session()) {
      this.tokenStorage.clear();
      return of(undefined);
    }
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

  private isPatientRoute(): boolean {
    const view = this.document.defaultView;
    if ((view?.location?.pathname ?? '').startsWith('/patient/')) return true;
    return new URLSearchParams(view?.location?.search ?? '').get('mode') === 'patient';
  }
}
