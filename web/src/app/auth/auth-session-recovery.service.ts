import { DOCUMENT } from '@angular/common';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { defer, finalize, firstValueFrom, from, Observable, shareReplay } from 'rxjs';
import { LoginResponse } from './auth.models';
import { AuthTokenStorageService } from './auth-token-storage.service';

const AUTH_REFRESH_LOCK = 'joprelys-auth-refresh';
const AUTH_BROADCAST_CHANNEL = 'joprelys-auth-session';
const CONCURRENT_REFRESH_RETRY_DELAY_MS = 350;

@Injectable({ providedIn: 'root' })
export class AuthSessionRecoveryService implements OnDestroy {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly rawHttp = new HttpClient(inject(HttpBackend));
  private readonly broadcastChannel = this.createBroadcastChannel();

  private refreshInFlight$: Observable<string> | null = null;
  private isRedirectingToLogin = false;

  constructor() {
    this.broadcastChannel?.addEventListener('message', this.onBroadcastMessage);
  }

  refreshAccessToken(expectedAccessToken?: string): Observable<string> {
    const currentRefresh = this.refreshInFlight$;
    if (currentRefresh) {
      return currentRefresh;
    }

    const refreshRequest = defer(() => from(this.refreshAcrossTabs(expectedAccessToken))).pipe(
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

  ngOnDestroy(): void {
    this.broadcastChannel?.removeEventListener('message', this.onBroadcastMessage);
    this.broadcastChannel?.close();
  }

  private async refreshAcrossTabs(expectedAccessToken?: string): Promise<string> {
    const navigatorRef = this.document.defaultView?.navigator;
    if (navigatorRef?.locks) {
      return navigatorRef.locks.request(AUTH_REFRESH_LOCK, async () => this.refreshUnderLock(expectedAccessToken));
    }
    return this.refreshUnderLock(expectedAccessToken);
  }

  private async refreshUnderLock(expectedAccessToken?: string): Promise<string> {
    const synchronizedToken = this.currentUsableReplacement(expectedAccessToken);
    if (synchronizedToken) {
      return synchronizedToken;
    }

    try {
      return await this.performRefresh();
    } catch (error) {
      if (!(error instanceof HttpErrorResponse) || error.status !== 409) {
        throw error;
      }

      // Another request/tab may have rotated the HttpOnly refresh cookie at the
      // same instant. Do not log the clinician out: give the successful response
      // time to update the shared cookie / BroadcastChannel, then retry once.
      await this.delay(CONCURRENT_REFRESH_RETRY_DELAY_MS);
      const refreshedByPeer = this.currentUsableReplacement(expectedAccessToken);
      if (refreshedByPeer) {
        return refreshedByPeer;
      }
      return this.performRefresh();
    }
  }

  private async performRefresh(): Promise<string> {
    const response = await firstValueFrom(this.rawHttp.post<LoginResponse>(
      '/api/auth/refresh',
      {},
      { withCredentials: true },
    ));
    this.tokenStorage.save(response);
    this.broadcastChannel?.postMessage({ type: 'SESSION_REFRESHED', response });
    return response.accessToken;
  }

  private currentUsableReplacement(expectedAccessToken?: string): string | null {
    if (!expectedAccessToken) return null;
    const current = this.tokenStorage.session();
    if (!current || current.accessToken === expectedAccessToken || this.tokenStorage.isExpired()) {
      return null;
    }
    return current.accessToken;
  }

  private createBroadcastChannel(): BroadcastChannel | null {
    const BroadcastChannelCtor = this.document.defaultView?.BroadcastChannel;
    if (!BroadcastChannelCtor) return null;
    try {
      return new BroadcastChannelCtor(AUTH_BROADCAST_CHANNEL);
    } catch {
      return null;
    }
  }

  private readonly onBroadcastMessage = (event: MessageEvent<unknown>): void => {
    const payload = event.data as { type?: unknown; response?: Partial<LoginResponse> } | null;
    if (payload?.type !== 'SESSION_REFRESHED') return;
    const response = payload.response;
    if (!response
      || typeof response.accessToken !== 'string'
      || typeof response.expiresAt !== 'string'
      || typeof response.email !== 'string'
      || typeof response.name !== 'string'
      || typeof response.role !== 'string') {
      return;
    }
    this.tokenStorage.save(response as LoginResponse);
  };

  private delay(milliseconds: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
  }

  private resolveReturnUrl(currentUrl: string): string | null {
    if (!currentUrl || currentUrl === '/' || currentUrl.startsWith('//') || currentUrl.startsWith('/auth/login')) {
      return null;
    }
    return currentUrl;
  }
}
