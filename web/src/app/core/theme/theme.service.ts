import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';
import { APP_BRAND_CONFIG, AppTheme } from '../config/app-brand.config';
import { ConsentManagementService } from '../privacy/consent-management.service';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly consent = inject(ConsentManagementService);
  private readonly storageKey = 'joprelys.theme';

  readonly theme = signal<AppTheme>(this.resolveInitialTheme());

  constructor() {
    effect(() => this.applyTheme(this.theme()));
  }

  setTheme(theme: AppTheme): void {
    this.theme.set(theme);
    const storage = this.document.defaultView?.localStorage;
    try {
      if (this.consent.preferencesAllowed() && typeof storage?.setItem === 'function') {
        storage.setItem(this.storageKey, theme);
      } else {
        storage?.removeItem(this.storageKey);
      }
    } catch {
      // The theme still applies in memory when storage is unavailable.
    }
  }

  private resolveInitialTheme(): AppTheme {
    if (!this.consent.preferencesAllowed()) {
      return APP_BRAND_CONFIG.defaultTheme;
    }

    try {
      const storage = this.document.defaultView?.localStorage;
      const stored = typeof storage?.getItem === 'function' ? storage.getItem(this.storageKey) : null;
      return stored === 'dark' || stored === 'light' ? stored : APP_BRAND_CONFIG.defaultTheme;
    } catch {
      return APP_BRAND_CONFIG.defaultTheme;
    }
  }

  private applyTheme(theme: AppTheme): void {
    const root = this.document.documentElement;
    root.dataset['theme'] = theme;
    root.classList.toggle('dark', theme === 'dark');
  }
}
