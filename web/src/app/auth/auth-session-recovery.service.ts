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
  private expiredSessionToken: string | null = null;

  refreshAccessToken(): Observable<string> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.rawHttp.post<LoginResponse>('/api/auth/refresh', {}).pipe(
        tap((response) => {
          this.tokenStorage.save(response);
          this.expiredSessionToken = null;
        }),
        map((response) => response.accessToken),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }

    return this.refreshInFlight$;
  }

  expireSession(): void {
    const session = this.tokenStorage.session();
    if (!session || this.expiredSessionToken === session.accessToken) {
      return;
    }

    this.expiredSessionToken = session.accessToken;
    const returnUrl = this.resolveReturnUrl(this.router.url);
    this.tokenStorage.clear();

    void this.router.navigate(['/'], {
      queryParams: {
        sessionExpired: 'true',
        ...(returnUrl ? { returnUrl } : {}),
      },
      replaceUrl: true,
    });
  }

  private resolveReturnUrl(currentUrl: string): string | null {
    if (!currentUrl || currentUrl === '/' || currentUrl.startsWith('//')) {
      return null;
    }
    return currentUrl;
  }
}
