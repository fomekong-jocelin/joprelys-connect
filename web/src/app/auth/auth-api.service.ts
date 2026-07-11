import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { finalize, Observable, tap } from 'rxjs';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { CurrentSessionResponse, LoginRequest, LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly apiBaseUrl = '/api/auth';

  constructor(
    private readonly http: HttpClient,
    private readonly tokenStorage: AuthTokenStorageService,
  ) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiBaseUrl}/login`, request).pipe(
      tap((response) => {
        if (!response.requiresOtp) {
          this.tokenStorage.save(response);
        }
      }),
    );
  }

  verifyStaffOtp(email: string, otpCode: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiBaseUrl}/verify-otp`, { email, otpCode }).pipe(
      tap((response) => this.tokenStorage.save(response)),
    );
  }

  refreshCurrentSession(): Observable<CurrentSessionResponse> {
    return this.http.get<CurrentSessionResponse>(`${this.apiBaseUrl}/me`).pipe(
      tap((response) => this.tokenStorage.updateIdentity(response)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/logout`, {}).pipe(
      finalize(() => this.tokenStorage.clear()),
    );
  }

  requestPasswordRecovery(email: string): Observable<{ otpCode: string }> {
    return this.http.post<{ otpCode: string }>('/api/public/auth/password-recovery/request', { email });
  }

  resetPassword(request: unknown): Observable<void> {
    return this.http.post<void>('/api/public/auth/password-recovery/reset', request);
  }
}
