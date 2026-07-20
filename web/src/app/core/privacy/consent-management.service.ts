import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';

export type ConsentCategory = 'necessary' | 'preferences' | 'analytics' | 'marketing';

export interface ConsentState {
  readonly version: string;
  readonly necessary: true;
  readonly preferences: boolean;
  readonly analytics: false;
  readonly marketing: false;
  readonly updatedAt: string | null;
}

export const CONSENT_POLICY_VERSION = '1.0';
export const CONSENT_STORAGE_KEY = 'joprelys.privacy.consent';

const PREFERENCE_STORAGE_KEYS = [
  'joprelys.theme',
  'joprelys.locale',
  'joprelys.sidebar.collapsed',
] as const;

const DEFAULT_CONSENT_STATE: ConsentState = {
  version: CONSENT_POLICY_VERSION,
  necessary: true,
  preferences: false,
  analytics: false,
  marketing: false,
  updatedAt: null,
};

@Injectable({ providedIn: 'root' })
export class ConsentManagementService {
  private readonly document = inject(DOCUMENT);
  private readonly stateSignal = signal<ConsentState>(this.readStoredState());

  readonly state = this.stateSignal.asReadonly();
  readonly hasStoredDecision = computed(() => this.stateSignal().updatedAt !== null);
  readonly preferencesAllowed = computed(() => this.stateSignal().preferences);
  readonly analyticsAllowed = computed(() => false);
  readonly marketingAllowed = computed(() => false);

  isAllowed(category: ConsentCategory): boolean {
    if (category === 'necessary') return true;
    if (category === 'preferences') return this.preferencesAllowed();
    return false;
  }

  savePreferences(preferences: boolean): ConsentState {
    const next: ConsentState = {
      version: CONSENT_POLICY_VERSION,
      necessary: true,
      preferences,
      analytics: false,
      marketing: false,
      updatedAt: new Date().toISOString(),
    };

    this.stateSignal.set(next);
    this.writeState(next);

    if (!preferences) {
      this.clearPreferenceStorage();
    }

    return next;
  }

  acceptAvailableOptions(): ConsentState {
    return this.savePreferences(true);
  }

  rejectOptionalOptions(): ConsentState {
    return this.savePreferences(false);
  }

  resetDecision(): void {
    this.stateSignal.set(DEFAULT_CONSENT_STATE);
    try {
      this.storage()?.removeItem(CONSENT_STORAGE_KEY);
    } catch {
      // Storage may be unavailable in hardened or private browser contexts.
    }
    this.clearPreferenceStorage();
  }

  private readStoredState(): ConsentState {
    try {
      const raw = this.storage()?.getItem(CONSENT_STORAGE_KEY);
      if (!raw) return DEFAULT_CONSENT_STATE;

      const parsed = JSON.parse(raw) as Partial<ConsentState>;
      if (parsed.version !== CONSENT_POLICY_VERSION || typeof parsed.preferences !== 'boolean') {
        return DEFAULT_CONSENT_STATE;
      }

      return {
        version: CONSENT_POLICY_VERSION,
        necessary: true,
        preferences: parsed.preferences,
        analytics: false,
        marketing: false,
        updatedAt: typeof parsed.updatedAt === 'string' ? parsed.updatedAt : null,
      };
    } catch {
      return DEFAULT_CONSENT_STATE;
    }
  }

  private writeState(state: ConsentState): void {
    try {
      this.storage()?.setItem(CONSENT_STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Consent remains effective in memory if browser storage is unavailable.
    }
  }

  private clearPreferenceStorage(): void {
    try {
      const storage = this.storage();
      for (const key of PREFERENCE_STORAGE_KEYS) {
        storage?.removeItem(key);
      }
    } catch {
      // Revocation remains effective in memory even if storage cannot be cleaned.
    }
  }

  private storage(): Storage | null {
    try {
      return this.document.defaultView?.localStorage ?? null;
    } catch {
      return null;
    }
  }
}
