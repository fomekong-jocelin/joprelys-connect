import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { CardComponent } from '../../../shared/ui/card.component';
import { PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

import { AppDatePipe } from '../../../shared/formatting/app-date.pipe';
@Component({
  selector: 'app-patient-qr-code-page',
  standalone: true,
  imports: [AppDatePipe, CommonModule, AppShellComponent, PageHeaderComponent, CardComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('patient.qrCode.title')"
        [subtitle]="t('patient.qrCode.subtitle')"
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
        } @else if (patient(); as p) {
          <div class="max-w-xl mx-auto">
            <app-ui-card class="text-center">
              <p class="text-sm" style="color: var(--text-secondary)">
                {{ t('patient.qrCode.description') }}
              </p>

              <div class="my-8 inline-block border p-2" style="border-color: var(--app-border); border-radius: var(--radius-brand-md)">
                @if (qrCodeUrl(); as url) {
                  <img [src]="url" [alt]="t('patient.qrCode.alt')" class="w-56 h-56" />
                } @else if (qrError()) {
                  <div class="w-56 h-56 flex flex-col items-center justify-center gap-3 p-3" role="alert">
                    <p class="text-sm" style="color: var(--text-secondary)">{{ t('patient.qrCode.qrError') }}</p>
                    <button type="button" class="ui-button ui-button-secondary" (click)="loadQrCode()">
                      {{ t('common.retry') }}
                    </button>
                  </div>
                } @else {
                  <div class="w-56 h-56 animate-pulse rounded" style="background: var(--app-border)" aria-hidden="true"></div>
                }
              </div>

              <div class="space-y-2 text-sm">
                <p style="color: var(--text-muted)">
                  {{ t('patient.qrCode.patient') }} : <span class="font-mono font-semibold" style="color: var(--text-primary)">{{ p.globalPatientNumber }}</span>
                </p>
                <p style="color: var(--text-muted)">
                  {{ t('patient.qrCode.validUntil') }} : <span class="font-semibold" style="color: var(--text-primary)">{{ validUntil() | appDate:'dateTime' }}</span>
                </p>
              </div>

              <div class="mt-6 p-4 text-left" style="border-radius: var(--radius-brand-md); background: color-mix(in srgb, var(--brand-primary) 8%, var(--app-surface)); border: 1px solid color-mix(in srgb, var(--brand-primary) 20%, var(--app-border))">
                <p class="text-xs font-bold uppercase tracking-wider mb-1" style="color: var(--brand-primary)">
                  {{ t('patient.qrCode.securityNotice') }}
                </p>
                <p class="text-xs leading-relaxed" style="color: var(--text-secondary)">
                  {{ t('patient.qrCode.securityText') }}
                </p>
              </div>
            </app-ui-card>
          </div>
        }
      </div>
    </app-shell>
  `,
})
export class PatientQrCodePageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly patient = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly validUntil = signal<Date>(this.computeValidUntil());
  readonly qrCodeUrl = signal<string | null>(null);
  readonly qrError = signal(false);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.releaseQrCodeUrl());
  }

  ngOnInit(): void {
    this.loadProfile();
    this.loadQrCode();
  }

  /** Le QR est généré par le serveur Joprelys : aucun identifiant patient n'est envoyé à un service tiers. */
  loadQrCode(): void {
    this.qrError.set(false);
    this.releaseQrCodeUrl();
    this.portalService.getAccessQr().subscribe({
      next: (response) => {
        if (!response.body) {
          this.qrError.set(true);
          return;
        }
        const serverValidUntil = response.headers.get('X-Qr-Valid-Until');
        this.validUntil.set(serverValidUntil ? new Date(serverValidUntil) : this.computeValidUntil());
        this.qrCodeUrl.set(URL.createObjectURL(response.body));
      },
      error: () => this.qrError.set(true),
    });
  }

  private releaseQrCodeUrl(): void {
    const url = this.qrCodeUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.qrCodeUrl.set(null);
    }
  }

  private loadProfile(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMe().subscribe({
      next: (data) => {
        this.patient.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.t('patient.qrCode.loadError'));
      },
    });
  }

  private computeValidUntil(): Date {
    const date = new Date();
    date.setMinutes(date.getMinutes() + 15);
    return date;
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
