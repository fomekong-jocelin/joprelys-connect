import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { CardComponent } from '../../../shared/ui/card.component';
import { PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-qr-code-page',
  standalone: true,
  imports: [CommonModule, AppShellComponent, PageHeaderComponent, CardComponent],
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
                <img
                  [src]="qrCodeUrl(p.globalPatientNumber)"
                  [alt]="t('patient.qrCode.alt')"
                  class="w-56 h-56"
                />
              </div>

              <div class="space-y-2 text-sm">
                <p style="color: var(--text-muted)">
                  {{ t('patient.qrCode.patient') }} : <span class="font-mono font-semibold" style="color: var(--text-primary)">{{ p.globalPatientNumber }}</span>
                </p>
                <p style="color: var(--text-muted)">
                  {{ t('patient.qrCode.validUntil') }} : <span class="font-semibold" style="color: var(--text-primary)">{{ validUntil() | date:'short' }}</span>
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

  ngOnInit(): void {
    this.loadProfile();
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

  qrCodeUrl(globalPatientNumber: string): string {
    const payload = JSON.stringify({
      type: 'JOPRELYS_PATIENT_ACCESS',
      globalPatientNumber,
      validUntil: this.validUntil().toISOString(),
    });
    return `https://api.qrserver.com/v1/create-qr-code/?size=224x224&data=${encodeURIComponent(payload)}`;
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
