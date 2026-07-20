import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLocale } from '../core/config/app-brand.config';
import { I18nService } from '../core/i18n/i18n.service';
import {
  CONSENT_POLICY_VERSION,
  ConsentManagementService,
} from '../core/privacy/consent-management.service';
import { ThemeService } from '../core/theme/theme.service';
import { AppLogoComponent } from '../shared/ui/app-logo.component';

@Component({
  selector: 'app-consent-preferences',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './consent-preferences.component.html',
  styleUrl: './consent-preferences.component.css',
})
export class ConsentPreferencesComponent {
  private readonly consent = inject(ConsentManagementService);
  private readonly i18n = inject(I18nService);
  private readonly themeService = inject(ThemeService);

  readonly locale = this.i18n.locale;
  readonly theme = this.themeService.theme;
  readonly policyVersion = CONSENT_POLICY_VERSION;
  readonly preferencesEnabled = signal(this.consent.preferencesAllowed());
  readonly feedbackKey = signal<string | null>(null);

  t(key: string): string {
    return this.i18n.t(key);
  }

  policyVersionLabel(): string {
    return this.t('consent.page.version').replace('{version}', this.policyVersion);
  }

  setLang(lang: AppLocale): void {
    void this.i18n.setLocale(lang);
  }

  toggleTheme(): void {
    this.themeService.setTheme(this.theme() === 'light' ? 'dark' : 'light');
  }

  themeTooltip(): string {
    return this.theme() === 'dark'
      ? this.t('shell.theme.light')
      : this.t('shell.theme.dark');
  }

  togglePreferences(event: Event): void {
    const target = event.target;
    if (target instanceof HTMLInputElement) {
      this.preferencesEnabled.set(target.checked);
      this.feedbackKey.set(null);
    }
  }

  save(): void {
    const enabled = this.preferencesEnabled();
    this.consent.savePreferences(enabled);
    if (enabled) {
      this.persistCurrentInterfacePreferences();
      this.feedbackKey.set('consent.actions.saved');
    } else {
      this.feedbackKey.set('consent.actions.rejected');
    }
  }

  acceptAvailable(): void {
    this.preferencesEnabled.set(true);
    this.consent.acceptAvailableOptions();
    this.persistCurrentInterfacePreferences();
    this.feedbackKey.set('consent.actions.saved');
  }

  rejectOptional(): void {
    this.preferencesEnabled.set(false);
    this.consent.rejectOptionalOptions();
    this.feedbackKey.set('consent.actions.rejected');
  }

  private persistCurrentInterfacePreferences(): void {
    this.themeService.setTheme(this.theme());
    void this.i18n.setLocale(this.locale());
  }
}
