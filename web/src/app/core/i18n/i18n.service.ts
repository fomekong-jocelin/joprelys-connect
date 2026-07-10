import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, firstValueFrom, forkJoin, of } from 'rxjs';
import { APP_BRAND_CONFIG, AppLocale } from '../config/app-brand.config';

type TranslationDictionary = Record<string, string>;

const LOCALE_STORAGE_KEY = 'joprelys.locale';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly http = inject(HttpClient);

  readonly locale = signal<AppLocale>(this.loadStoredLocale());
  private readonly dictionary = signal<TranslationDictionary>({});
  private readonly loaded = signal<Record<AppLocale, boolean>>({ fr: false, en: false });

  /** Initial load called via APP_INITIALIZER. */
  async init(): Promise<void> {
    await this.loadLocale(this.locale());
  }

  t(key: string, defaultValue?: string): string {
    return this.dictionary()[key] ?? defaultValue ?? key;
  }

  async setLocale(lang: AppLocale): Promise<void> {
    this.locale.set(lang);
    try {
      localStorage.setItem(LOCALE_STORAGE_KEY, lang);
    } catch {
      /* SSR safe */
    }
    await this.loadLocale(lang);
  }

  toggle(): void {
    void this.setLocale(this.locale() === 'fr' ? 'en' : 'fr');
  }

  private async loadLocale(lang: AppLocale): Promise<void> {
    try {
      const dictionaries = await firstValueFrom(forkJoin({
        base: this.http.get<TranslationDictionary>(`/assets/i18n/${lang}.json`),
        extension: this.http.get<TranslationDictionary>(`/assets/i18n/extensions/${lang}.json`).pipe(
          catchError(() => of({} as TranslationDictionary))
        ),
      }));

      this.dictionary.set({ ...dictionaries.base, ...dictionaries.extension });
      this.loaded.update((state) => ({ ...state, [lang]: true }));
    } catch {
      // Fallback to empty dictionary so keys are displayed instead of crashing.
      this.dictionary.set({});
    }
  }

  private loadStoredLocale(): AppLocale {
    try {
      const stored = localStorage.getItem(LOCALE_STORAGE_KEY) as AppLocale | null;
      if (stored && APP_BRAND_CONFIG.supportedLocales.includes(stored)) return stored;
    } catch {
      /* SSR safe */
    }
    return APP_BRAND_CONFIG.defaultLocale;
  }
}
