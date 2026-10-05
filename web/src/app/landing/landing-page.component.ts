import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AppLogoComponent } from '../shared/ui/app-logo.component';
import { ThemeService } from '../core/theme/theme.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { APP_BRAND_CONFIG } from '../core/config/app-brand.config';

import { LANDING_I18N, LandingContent } from './landing-i18n';
import { LandingDemoService, DemoLeadPayload, DemoLeadResponse } from './landing-demo.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, AppLogoComponent],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css'],
})
export class LandingPageComponent {
  private readonly router = inject(Router);
  readonly themeService = inject(ThemeService);
  readonly i18n = inject(I18nService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly demoService = inject(LandingDemoService);

  readonly brand = APP_BRAND_CONFIG;

  // Bilingual content
  readonly t = computed<LandingContent>(() => LANDING_I18N[this.i18n.currentLanguage()]);
  readonly faqs = computed(() => this.t().faq.items);

  // Session check
  readonly session = computed(() => this.tokenStorage.session());
  readonly isAuthenticated = computed(() => {
    const s = this.session();
    return !!s && !this.tokenStorage.isExpired();
  });
  readonly isPatient = computed(() => {
    const s = this.session();
    return s ? this.tokenStorage.isPatientSession(s) : false;
  });
  readonly dashboardUrl = computed(() => (this.isPatient() ? '/patient/dashboard' : '/dashboard'));

  // Mobile menu
  readonly mobileMenuOpen = signal(false);

  // FAQ state
  readonly activeFaq = signal<number | null>(0);

  // Demo lead capture form
  demoFullName = '';
  demoOrgName = '';
  demoRole = 'directeur';
  demoPhone = '';
  demoEmail = '';
  demoCity = 'Douala';
  demoMessage = '';

  readonly isSubmittingDemo = signal(false);
  readonly demoSuccess = signal(false);
  readonly demoSubmittedLead = signal<DemoLeadResponse | null>(null);
  readonly demoError = signal(false);

  demoWhatsAppUrl(): string {
    return this.demoService.buildWhatsAppUrl(this.demoPayload(), this.i18n.currentLanguage());
  }

  toggleFaq(index: number): void {
    this.activeFaq.update((curr) => (curr === index ? null : index));
  }

  toggleTheme(): void {
    const current = this.themeService.theme();
    this.themeService.setTheme(current === 'dark' ? 'light' : 'dark');
  }

  toggleLang(): void {
    this.i18n.toggle();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((v) => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  submitDemoRequest(): void {
    if (this.isSubmittingDemo() || !this.demoFullName.trim() || !this.demoPhone.trim() || !this.demoOrgName.trim()) {
      return;
    }

    this.isSubmittingDemo.set(true);
    this.demoError.set(false);
    this.demoSuccess.set(false);
    this.demoSubmittedLead.set(null);

    this.demoService.submitDemo(this.demoPayload()).subscribe({
      next: (res) => {
        this.isSubmittingDemo.set(false);
        if (!res?.id) {
          this.demoError.set(true);
          return;
        }
        this.demoSubmittedLead.set(res);
        this.demoSuccess.set(true);
      },
      error: () => {
        this.isSubmittingDemo.set(false);
        this.demoError.set(true);
      }
    });
  }

  private demoPayload(): DemoLeadPayload {
    return {
      fullName: this.demoFullName.trim(),
      organizationName: this.demoOrgName.trim(),
      role: this.demoRole,
      phone: this.demoPhone.trim(),
      email: this.demoEmail.trim() || undefined,
      city: this.demoCity,
      message: this.demoMessage.trim() || undefined,
      source: 'landing-page',
      locale: this.i18n.currentLanguage()
    };
  }
}
