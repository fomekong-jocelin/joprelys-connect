import { Injectable, signal } from '@angular/core';
import { AuthSession, LoginResponse } from './auth.models';

const SESSION_KEY = 'joprelys.auth.session';
const DEFAULT_EXPIRATION_LEEWAY_SECONDS = 30;

@Injectable({ providedIn: 'root' })
export class AuthTokenStorageService {
  readonly session = signal<AuthSession | null>(this.readSession());

  get accessToken(): string | null {
    return this.session()?.accessToken ?? null;
  }

  save(response: LoginResponse): void {
    const session: AuthSession = {
      accessToken: response.accessToken,
      expiresAt: response.expiresAt,
      email: response.email,
      name: response.name,
      role: response.role,
    };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
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

  clear(): void {
    sessionStorage.removeItem(SESSION_KEY);
    this.session.set(null);
  }

  private readSession(): AuthSession | null {
    const rawSession = sessionStorage.getItem(SESSION_KEY);
    if (!rawSession) {
      return null;
    }

    try {
      return JSON.parse(rawSession) as AuthSession;
    } catch {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
