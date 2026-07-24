import { Injectable, inject, OnDestroy } from '@angular/core';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';

const KEEP_ALIVE_CHECK_INTERVAL_MS = 60_000; // Check every minute
const REFRESH_TRIGGER_LEEWAY_SECONDS = 900; // Refresh if token expires in <= 15 minutes

@Injectable({ providedIn: 'root' })
export class AuthSessionKeepAliveService implements OnDestroy {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly sessionRecovery = inject(AuthSessionRecoveryService);
  private timerId: ReturnType<typeof setInterval> | null = null;

  start(): void {
    if (this.timerId !== null) return;
    this.timerId = setInterval(() => this.checkAndKeepAlive(), KEEP_ALIVE_CHECK_INTERVAL_MS);
    this.checkAndKeepAlive();
  }

  stop(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  ngOnDestroy(): void {
    this.stop();
  }

  private checkAndKeepAlive(): void {
    const session = this.tokenStorage.session();
    if (!session) return;

    if (this.tokenStorage.isExpired(REFRESH_TRIGGER_LEEWAY_SECONDS)) {
      this.sessionRecovery.refreshAccessToken().subscribe({
        error: () => {
          // Failure handling will be taken care of by sessionRecovery / interceptor if unauthenticated
        },
      });
    }
  }
}
