import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, firstValueFrom, forkJoin, of } from 'rxjs';
import { APP_BRAND_CONFIG, AppLocale } from '../config/app-brand.config';
import { ConsentManagementService } from '../privacy/consent-management.service';

type TranslationDictionary = Record<string, string>;

const LOCALE_STORAGE_KEY = 'joprelys.locale';
const EMPTY_DICTIONARY = {} as TranslationDictionary;

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly http = inject(HttpClient);
  private readonly consent = inject(ConsentManagementService);

  readonly locale = signal<AppLocale>(this.loadStoredLocale());
  private readonly dictionary = signal<TranslationDictionary>({});
  private readonly loaded = signal<Record<AppLocale, boolean>>({ fr: false, en: false });
  // Translation JSON files are copied as static assets and are not fingerprinted by
  // Angular. A deployment can therefore serve the new JS bundle together with a
  // browser/proxy-cached older dictionary. One revision per application bootstrap
  // forces a fresh dictionary fetch while keeping all requests cacheable during the
  // current page lifetime.
  private readonly translationAssetRevision = Date.now().toString(36);

  async init(): Promise<void> {
    await this.loadLocale(this.locale());
  }

  t(key: string, defaultValue?: string): string {
    return this.dictionary()[key] ?? defaultValue ?? key;
  }

  currentLanguage(): AppLocale {
    return this.locale();
  }

  async setLocale(lang: AppLocale): Promise<void> {
    this.locale.set(lang);
    try {
      if (this.consent.preferencesAllowed()) {
        localStorage.setItem(LOCALE_STORAGE_KEY, lang);
      } else {
        localStorage.removeItem(LOCALE_STORAGE_KEY);
      }
    } catch {
      // Storage can be unavailable during SSR or in restricted browsers.
    }
    await this.loadLocale(lang);
  }

  toggle(): void {
    void this.setLocale(this.locale() === 'fr' ? 'en' : 'fr');
  }

  private async loadLocale(lang: AppLocale): Promise<void> {
    try {
      const dictionaries = await firstValueFrom(forkJoin({
        base: this.http.get<TranslationDictionary>(this.translationAssetUrl(`/assets/i18n/${lang}.json`)),
        extension: this.optionalDictionary(`/assets/i18n/extensions/${lang}.json`),
        admission: this.optionalDictionary(`/assets/i18n/features/admission/${lang}.json`),
        urgTemp: this.optionalDictionary(`/assets/i18n/features/urg-temp/${lang}.json`),
        emergency: this.optionalDictionary(`/assets/i18n/features/emergency/${lang}.json`),
        emergencyJourney: this.optionalDictionary(`/assets/i18n/features/emergency-journey/${lang}.json`),
        medicoLegal: this.optionalDictionary(`/assets/i18n/features/medico-legal/${lang}.json`),
        patientReconciliation: this.optionalDictionary(`/assets/i18n/features/patient-reconciliation/${lang}.json`),
        hospitalContinuity: this.optionalDictionary(`/assets/i18n/features/hospital-continuity/${lang}.json`),
        hospitalOrganization: this.optionalDictionary(`/assets/i18n/features/hospital-organization/${lang}.json`),
        staffOnboarding: this.optionalDictionary(`/assets/i18n/features/staff-onboarding/${lang}.json`),
        aiConsultation: this.optionalDictionary(`/assets/i18n/features/ai-consultation/${lang}.json`),
        consultationUi: this.optionalDictionary(`/assets/i18n/features/consultation-ui/${lang}.json`),
        linkedEvidence: this.optionalDictionary(`/assets/i18n/features/linked-evidence/${lang}.json`),
        vitalsAssistant: this.optionalDictionary(`/assets/i18n/features/vitals-assistant/${lang}.json`),
        availability: this.optionalDictionary(`/assets/i18n/features/availability/${lang}.json`),
        appointments: this.optionalDictionary(`/assets/i18n/features/appointments/${lang}.json`),
        spatialServices: this.optionalDictionary(`/assets/i18n/features/spatial-services/${lang}.json`),
        legal: this.optionalDictionary(`/assets/i18n/features/legal/${lang}.json`),
        consent: this.optionalDictionary(`/assets/i18n/features/consent/${lang}.json`),
      }));

      this.dictionary.set({
        ...dictionaries.base,
        ...dictionaries.extension,
        ...dictionaries.admission,
        ...dictionaries.urgTemp,
        ...dictionaries.emergency,
        ...dictionaries.emergencyJourney,
        ...dictionaries.medicoLegal,
        ...dictionaries.patientReconciliation,
        ...dictionaries.hospitalContinuity,
        ...dictionaries.hospitalOrganization,
        ...dictionaries.staffOnboarding,
        ...dictionaries.aiConsultation,
        ...dictionaries.consultationUi,
        ...dictionaries.linkedEvidence,
        ...dictionaries.vitalsAssistant,
        ...dictionaries.availability,
        ...dictionaries.appointments,
        ...dictionaries.spatialServices,
        ...dictionaries.legal,
        ...dictionaries.consent,
      });
      this.loaded.update(state => ({ ...state, [lang]: true }));
    } catch {
      this.dictionary.set({});
    }
  }

  private optionalDictionary(path: string) {
    return this.http.get<TranslationDictionary>(this.translationAssetUrl(path)).pipe(
      catchError(() => of(EMPTY_DICTIONARY)),
    );
  }

  private translationAssetUrl(path: string): string {
    const separator = path.includes('?') ? '&' : '?';
    return `${path}${separator}v=${this.translationAssetRevision}`;
  }

  private loadStoredLocale(): AppLocale {
    if (!this.consent.preferencesAllowed()) {
      return APP_BRAND_CONFIG.defaultLocale;
    }

    try {
      const stored = localStorage.getItem(LOCALE_STORAGE_KEY) as AppLocale | null;
      if (stored && APP_BRAND_CONFIG.supportedLocales.includes(stored)) {
        return stored;
      }
    } catch {
      // Storage can be unavailable during SSR or in restricted browsers.
    }
    return APP_BRAND_CONFIG.defaultLocale;
  }
}
