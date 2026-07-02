import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';
import { APP_BRAND_CONFIG, AppTheme } from '../config/app-brand.config';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storageKey = 'joprelys.theme';

  readonly theme = signal<AppTheme>(this.resolveInitialTheme());

  constructor() {
    effect(() => this.applyTheme(this.theme()));
  }

  setTheme(theme: AppTheme): void {
    this.theme.set(theme);
    const storage = this.document.defaultView?.localStorage;
    if (typeof storage?.setItem === 'function') {
      storage.setItem(this.storageKey, theme);
    }
  }

  private resolveInitialTheme(): AppTheme {
    const storage = this.document.defaultView?.localStorage;
    const stored = typeof storage?.getItem === 'function' ? storage.getItem(this.storageKey) : null;
    return stored === 'dark' || stored === 'light' ? stored : APP_BRAND_CONFIG.defaultTheme;
  }

  private applyTheme(theme: AppTheme): void {
    const root = this.document.documentElement;
    root.dataset['theme'] = theme;
    root.classList.toggle('dark', theme === 'dark');
  }
}
