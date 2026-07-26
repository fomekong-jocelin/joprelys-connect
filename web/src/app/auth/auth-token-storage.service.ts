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
      this.purgeTabIdentityState();
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

  isPatientSession(session: AuthSession | null = this.session()): boolean {
    if (!session) return false;
    return session.role
      .split(',')
      .map(role => role.trim())
      .filter(Boolean)
      .includes('PATIENT');
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
    this.purgeTabIdentityState();
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

  private purgeTabIdentityState(): void {
    // sessionStorage is tab-scoped, so clearing it cannot disconnect another tab.
    // localStorage, cookies and persistent preferences are origin-scoped and must
    // not be globally deleted when one tab changes identity. Identity-sensitive
    // feature caches register an explicit cleanup below (RBAC, active patient...).
    this.clearStorage(this.storage('sessionStorage'));
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
