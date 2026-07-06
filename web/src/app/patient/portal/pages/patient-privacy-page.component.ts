import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { CardComponent } from '../../../shared/ui/card.component';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';
import { PatientConsent, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-privacy-page',
  standalone: true,
  imports: [CommonModule, RouterLink, AppShellComponent, PageHeaderComponent, CardComponent, StatusBadgeComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('patient.privacy.title')"
        [subtitle]="t('patient.privacy.subtitle')"
      />

      <div class="app-container pb-10">
        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else {
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-1 space-y-6">
              <app-ui-card [title]="t('patient.privacy.shortcuts')">
                <nav class="space-y-2">
                  <a
                    routerLink="/patient/consents"
                    class="flex items-center justify-between p-3 text-sm font-semibold no-underline transition-colors hover:bg-[var(--app-surface-muted)]"
                    style="border-radius: var(--radius-brand-sm); color: var(--text-primary); border: 1px solid var(--app-border)"
                  >
                    {{ t('patient.privacy.manageConsents') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4" style="color: var(--text-muted)">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                  <a
                    routerLink="/patient/requests"
                    class="flex items-center justify-between p-3 text-sm font-semibold no-underline transition-colors hover:bg-[var(--app-surface-muted)]"
                    style="border-radius: var(--radius-brand-sm); color: var(--text-primary); border: 1px solid var(--app-border)"
                  >
                    {{ t('patient.privacy.accessRequests') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4" style="color: var(--text-muted)">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                  <a
                    routerLink="/patient/audit"
                    class="flex items-center justify-between p-3 text-sm font-semibold no-underline transition-colors hover:bg-[var(--app-surface-muted)]"
                    style="border-radius: var(--radius-brand-sm); color: var(--text-primary); border: 1px solid var(--app-border)"
                  >
                    {{ t('patient.privacy.auditTrail') }}
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4" style="color: var(--text-muted)">
                      <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
                    </svg>
                  </a>
                </nav>
              </app-ui-card>
            </div>

            <div class="lg:col-span-2 space-y-6">
              <app-ui-card [title]="t('patient.privacy.activeConsents')">
                @if (activeConsents().length === 0) {
                  <p class="text-sm" style="color: var(--text-secondary)">
                    {{ t('patient.privacy.noActiveConsents') }}
                  </p>
                } @else {
                  <div class="space-y-3">
                    @for (consent of activeConsents(); track consent.organizationId) {
                      <div class="flex items-center justify-between p-3" style="border-radius: var(--radius-brand-sm); background: var(--app-surface-muted); border: 1px solid var(--app-border)">
                        <div>
                          <p class="text-sm font-semibold" style="color: var(--text-primary)">{{ consent.organizationName }}</p>
                          <p class="text-xs" style="color: var(--text-muted)">{{ consent.scopes || t('patient.privacy.defaultScope') }}</p>
                        </div>
                        <app-status-badge [active]="true" [label]="t('patient.privacy.statusActive')" />
                      </div>
                    }
                  </div>
                }
              </app-ui-card>

              <app-ui-card [title]="t('patient.privacy.dataPolicy')">
                <p class="text-sm leading-relaxed" style="color: var(--text-secondary)">
                  {{ t('patient.privacy.dataPolicyText') }}
                </p>
              </app-ui-card>
            </div>
          </div>
        }
      </div>
    </app-shell>
  `,
})
export class PatientPrivacyPageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly consents = signal<PatientConsent[]>([]);
  readonly isLoading = signal(false);
  readonly error = signal('');

  readonly activeConsents = signal<PatientConsent[]>([]);

  ngOnInit(): void {
    this.loadConsents();
  }

  private loadConsents(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getConsents().subscribe({
      next: (data) => {
        this.consents.set(data);
        this.activeConsents.set(data.filter((c) => c.status?.toUpperCase() === 'ACTIVE'));
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.t('patient.privacy.loadError'));
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
