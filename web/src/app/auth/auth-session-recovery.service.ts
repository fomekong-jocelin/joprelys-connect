import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, map, Observable, shareReplay, tap } from 'rxjs';
import { LoginResponse } from './auth.models';
import { AuthTokenStorageService } from './auth-token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthSessionRecoveryService {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);
  private readonly rawHttp = new HttpClient(inject(HttpBackend));

  private refreshInFlight$: Observable<string> | null = null;
  private isRedirectingToLogin = false;

  refreshAccessToken(): Observable<string> {
    const currentRefresh = this.refreshInFlight$;
    if (currentRefresh) {
      return currentRefresh;
    }

    const refreshRequest = this.rawHttp.post<LoginResponse>('/api/auth/refresh', {}).pipe(
      tap((response) => {
        this.tokenStorage.save(response);
      }),
      map((response) => response.accessToken),
      finalize(() => {
        this.refreshInFlight$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.refreshInFlight$ = refreshRequest;
    return refreshRequest;
  }

  expireSession(): void {
    const currentUrl = this.router.url;
    const returnUrl = this.resolveReturnUrl(currentUrl);

    this.tokenStorage.clear();

    if (this.isRedirectingToLogin) {
      return;
    }

    if (currentUrl && currentUrl !== '/' && !currentUrl.startsWith('/auth/login') && !currentUrl.startsWith('/?')) {
      this.isRedirectingToLogin = true;
      void this.router.navigate(['/'], {
        queryParams: {
          sessionExpired: 'true',
          ...(returnUrl ? { returnUrl } : {}),
        },
        replaceUrl: true,
      }).finally(() => {
        this.isRedirectingToLogin = false;
      });
    }
  }

  private resolveReturnUrl(currentUrl: string): string | null {
    if (!currentUrl || currentUrl === '/' || currentUrl.startsWith('//') || currentUrl.startsWith('/auth/login')) {
      return null;
    }
    return currentUrl;
  }
}
