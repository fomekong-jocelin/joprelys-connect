import { Injectable, signal } from '@angular/core';
import { AuthSession, CurrentSessionResponse, LoginResponse } from './auth.models';

const SESSION_KEY = 'joprelys.auth.session';

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
    this.persist(session);
  }

  updateIdentity(response: CurrentSessionResponse): void {
    const current = this.session();
    if (!current) {
      return;
    }
    this.persist({
      ...current,
      email: response.email,
      name: response.name,
      role: response.role,
    });
  }

  clear(): void {
    sessionStorage.removeItem(SESSION_KEY);
    this.session.set(null);
  }

  private persist(session: AuthSession): void {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.session.set(session);
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
