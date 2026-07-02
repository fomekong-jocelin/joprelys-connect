import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { finalize, Observable, tap } from 'rxjs';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginRequest, LoginResponse } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly apiBaseUrl = '/api/auth';

  constructor(
    private readonly http: HttpClient,
    private readonly tokenStorage: AuthTokenStorageService,
  ) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiBaseUrl}/login`, request).pipe(
      tap((response) => this.tokenStorage.save(response)),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/logout`, {}).pipe(
      finalize(() => this.tokenStorage.clear()),
    );
  }
}
