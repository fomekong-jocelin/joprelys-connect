import { DOCUMENT } from '@angular/common';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { defer, finalize, firstValueFrom, from, Observable, shareReplay, throwError } from 'rxjs';
import { LoginResponse } from './auth.models';
import { AuthTokenStorageService } from './auth-token-storage.service';

const AUTH_REFRESH_LOCK = 'joprelys-professional-auth-refresh';
const AUTH_BROADCAST_CHANNEL = 'joprelys-professional-auth-session';
const CONCURRENT_REFRESH_RETRY_DELAYS_MS = [250, 750, 1_500] as const;

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
    // Patient JWTs are intentionally non-persistent and must never be exchanged
    // against the professional HttpOnly refresh cookie. This is the hard context
    // boundary preventing a patient tab from becoming a clinician tab silently.
    if (this.isPatientContext()) {
      return throwError(() => new Error('PATIENT_CONTEXT_HAS_NO_PROFESSIONAL_REFRESH'));
    }

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
    const patientContext = this.isPatientContext();
    const returnUrl = this.resolveReturnUrl(currentUrl, patientContext);

    this.tokenStorage.clear();

    if (this.isRedirectingToLogin) {
      return;
    }

    if (currentUrl && currentUrl !== '/' && !currentUrl.startsWith('/auth/login') && !currentUrl.startsWith('/login')) {
      this.isRedirectingToLogin = true;
      void this.router.navigate(['/login'], {
        queryParams: {
          ...(patientContext ? { mode: 'patient' } : {}),
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
    if (this.isPatientContext()) {
      throw new Error('PATIENT_CONTEXT_HAS_NO_PROFESSIONAL_REFRESH');
    }
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

    let lastConflict: HttpErrorResponse | null = null;
    for (let attempt = 0; attempt <= CONCURRENT_REFRESH_RETRY_DELAYS_MS.length; attempt++) {
      try {
        return await this.performRefresh();
      } catch (error) {
        if (!(error instanceof HttpErrorResponse) || error.status !== 409) {
          throw error;
        }
        lastConflict = error;
        const delayMs = CONCURRENT_REFRESH_RETRY_DELAYS_MS[attempt];
        if (delayMs === undefined) break;

        // Another professional request/tab may have rotated the HttpOnly cookie at
        // the same instant. Patient tabs never participate in this channel.
        await this.delay(delayMs);
        const refreshedByPeer = this.currentUsableReplacement(expectedAccessToken);
        if (refreshedByPeer) {
          return refreshedByPeer;
        }
      }
    }

    throw lastConflict ?? new Error('AUTH_REFRESH_CONCURRENT');
  }

  private async performRefresh(): Promise<string> {
    const response = await firstValueFrom(this.rawHttp.post<LoginResponse>(
      '/api/auth/refresh',
      {},
      { withCredentials: true },
    ));
    if (this.roles(response.role).includes('PATIENT')) {
      throw new Error('PROFESSIONAL_REFRESH_RETURNED_PATIENT_IDENTITY');
    }
    this.tokenStorage.save(response);
    this.broadcastChannel?.postMessage({ type: 'SESSION_REFRESHED', response });
    return response.accessToken;
  }

  private currentUsableReplacement(expectedAccessToken?: string): string | null {
    const current = this.tokenStorage.session();
    if (!current || this.tokenStorage.isPatientSession(current) || this.tokenStorage.isExpired()) {
      return null;
    }
    if (!expectedAccessToken) {
      return current.accessToken;
    }
    if (current.accessToken === expectedAccessToken) {
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
    if (this.isPatientContext()) return;

    const payload = event.data as { type?: unknown; response?: Partial<LoginResponse> } | null;
    if (payload?.type !== 'SESSION_REFRESHED') return;
    const response = payload.response;
    if (!response
      || typeof response.accessToken !== 'string'
      || typeof response.expiresAt !== 'string'
      || typeof response.email !== 'string'
      || typeof response.name !== 'string'
      || typeof response.role !== 'string'
      || this.roles(response.role).includes('PATIENT')) {
      return;
    }
    this.tokenStorage.save(response as LoginResponse);
  };

  private isPatientContext(): boolean {
    if (this.tokenStorage.isPatientSession()) return true;
    const routerUrl = this.router.url ?? '';
    if (routerUrl.startsWith('/patient/') || routerUrl.includes('mode=patient')) return true;
    const view = this.document.defaultView;
    const pathname = view?.location?.pathname ?? '';
    if (pathname.startsWith('/patient/')) return true;
    const search = view?.location?.search ?? '';
    return new URLSearchParams(search).get('mode') === 'patient';
  }

  private roles(value: string): string[] {
    return value.split(',').map(role => role.trim()).filter(Boolean);
  }

  private delay(milliseconds: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
  }

  private resolveReturnUrl(currentUrl: string, patientContext: boolean): string | null {
    if (!currentUrl || currentUrl === '/' || currentUrl.startsWith('//') || currentUrl.startsWith('/auth/login') || currentUrl.startsWith('/login')) {
      return null;
    }
    if (patientContext && !currentUrl.startsWith('/patient/')) {
      return null;
    }
    if (!patientContext && currentUrl.startsWith('/patient/')) {
      return null;
    }
    return currentUrl;
  }
}
