import { DOCUMENT } from '@angular/common';
import { inject, Injectable, signal } from '@angular/core';
import { AuthSession, LoginResponse } from './auth.models';

const SESSION_KEY = 'joprelys.auth.session';
const DEFAULT_EXPIRATION_LEEWAY_SECONDS = 30;

@Injectable({ providedIn: 'root' })
export class AuthTokenStorageService {
  private readonly document = inject(DOCUMENT);
  readonly session = signal<AuthSession | null>(this.readSession());
  private readonly sessionBoundaryCleanups = new Set<() => void>();

  get accessToken(): string | null {
    return this.session()?.accessToken ?? null;
  }

  save(response: LoginResponse): void {
    const previousSession = this.session();
    if (previousSession && this.isDifferentIdentity(previousSession, response)) {
      this.purgeBrowserState();
    }

    const session: AuthSession = {
      accessToken: response.accessToken,
      expiresAt: response.expiresAt,
      email: response.email,
      name: response.name,
      role: response.role,
    };
    this.writeSession(session);
    this.session.set(session);
  }

  isExpired(leewaySeconds = DEFAULT_EXPIRATION_LEEWAY_SECONDS): boolean {
    const session = this.session();
    if (!session) {
      return true;
    }

    const expiresAt = Date.parse(session.expiresAt);
    if (Number.isNaN(expiresAt)) {
      return true;
    }

    return expiresAt <= Date.now() + leewaySeconds * 1_000;
  }

  clearAccessToken(): void {
    try {
      this.storage('sessionStorage')?.removeItem(SESSION_KEY);
    } catch {
      // The in-memory session must still be cleared when storage is unavailable.
    }
    this.session.set(null);
  }

  clear(): void {
    this.purgeBrowserState();
  }

  registerSessionBoundaryCleanup(cleanup: () => void): () => void {
    this.sessionBoundaryCleanups.add(cleanup);
    return () => this.sessionBoundaryCleanups.delete(cleanup);
  }

  private readSession(): AuthSession | null {
    const sessionStorage = this.storage('sessionStorage');
    let rawSession: string | null = null;
    try {
      rawSession = sessionStorage?.getItem(SESSION_KEY) ?? null;
    } catch {
      return null;
    }
    if (!rawSession) {
      return null;
    }

    try {
      return JSON.parse(rawSession) as AuthSession;
    } catch {
      try {
        sessionStorage?.removeItem(SESSION_KEY);
      } catch {
        // Invalid storage remains unusable, but no session is restored in memory.
      }
      return null;
    }
  }

  private purgeBrowserState(): void {
    this.clearStorage(this.storage('sessionStorage'));
    this.clearStorageExceptDrafts(this.storage('localStorage'));
    // Do not iterate through document.cookie here. The refresh cookie is HttpOnly
    // and therefore cannot be cleared safely from JavaScript anyway. Deleting all
    // accessible cookies can remove unrelated functional/consent cookies and make
    // an authentication incident cascade into other application failures.
    this.session.set(null);
    for (const cleanup of this.sessionBoundaryCleanups) {
      try {
        cleanup();
      } catch {
        // One feature cleanup must not prevent the remaining session state from being purged.
      }
    }
  }

  private clearStorage(storage: Storage | null): void {
    if (!storage) return;
    try {
      storage.clear();
    } catch {
      // Storage can be unavailable in hardened/private browser contexts.
    }
  }

  private clearStorageExceptDrafts(storage: Storage | null): void {
    if (!storage) return;
    try {
      const draftItems: Array<{ key: string; value: string }> = [];
      for (let i = 0; i < storage.length; i++) {
        const key = storage.key(i);
        if (key && (key.startsWith('joprelys_draft_') || key.startsWith('joprelys_admission_draft'))) {
          const value = storage.getItem(key);
          if (value) draftItems.push({ key, value });
        }
      }
      storage.clear();
      for (const item of draftItems) {
        storage.setItem(item.key, item.value);
      }
    } catch {
      // Storage can be unavailable in hardened/private browser contexts.
    }
  }

  private isDifferentIdentity(previous: AuthSession, next: LoginResponse): boolean {
    return previous.email !== next.email || previous.role !== next.role;
  }

  private storage(kind: 'localStorage' | 'sessionStorage'): Storage | null {
    try {
      return this.document.defaultView?.[kind] ?? null;
    } catch {
      return null;
    }
  }

  private writeSession(session: AuthSession): void {
    try {
      this.storage('sessionStorage')?.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
      // Memory-only authentication remains possible in restricted browser contexts.
    }
  }
}
