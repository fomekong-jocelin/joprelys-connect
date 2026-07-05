import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ExternalAccessApiService, CreateExternalAccessRequestDto } from './external-access-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { AlertComponent } from '../../shared/ui/alert.component';

const DURATION_OPTIONS = [
  { value: 0.25, labelFr: '15 minutes', labelEn: '15 minutes' },
  { value: 1, labelFr: '1 heure', labelEn: '1 hour' },
  { value: 24, labelFr: '24 heures', labelEn: '24 hours' },
  { value: 168, labelFr: '7 jours', labelEn: '7 days' },
];

const ACCESS_SCOPES = [
  { key: 'medical_records', labelFr: 'Dossier médical', labelEn: 'Medical records' },
  { key: 'prescriptions', labelFr: 'Ordonnances', labelEn: 'Prescriptions' },
  { key: 'lab_results', labelFr: 'Résultats de labo', labelEn: 'Lab results' },
  { key: 'allergies_history', labelFr: 'Allergies & ATCD', labelEn: 'Allergies & history' },
];

@Component({
  selector: 'app-clinic-access-request',
  standalone: true,
  imports: [CommonModule, FormsModule, AppShellComponent, ButtonComponent, AlertComponent],
  template: `
    <app-shell>
      <div class="app-container py-8">
        <div class="mb-6">
          <p class="ui-label">{{ i18n.t('clinic.accessRequest.label') }}</p>
          <h2 class="font-display text-2xl font-extrabold" style="color: var(--text-primary)">
            {{ i18n.t('clinic.accessRequest.title') }}
          </h2>
          <p class="mt-2 text-sm" style="color: var(--text-secondary)">
            {{ i18n.t('clinic.accessRequest.subtitle') }}
          </p>
        </div>

        <div class="ui-card-subtle p-5 lg:p-6 flex flex-col gap-5 max-w-2xl">
          @if (success()) {
            <div class="p-4 rounded-[var(--radius-brand-md)] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-300 text-sm font-semibold flex items-start gap-3">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5 shrink-0 mt-0.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <p class="font-bold">{{ i18n.t('clinic.accessRequest.successTitle') }}</p>
                <p class="mt-1">{{ i18n.t('clinic.accessRequest.successMessage') }}</p>
              </div>
            </div>
            <div class="flex justify-end">
              <app-ui-button variant="primary" (pressed)="resetForm()">
                {{ i18n.t('clinic.accessRequest.newRequest') }}
              </app-ui-button>
            </div>
          } @else {
            <!-- DPU Patient -->
            <div class="space-y-1.5">
              <label for="dpu" class="ui-label">{{ i18n.t('clinic.accessRequest.dpuLabel') }} <span class="text-red-500">*</span></label>
              <input
                id="dpu"
                type="text"
                class="ui-input w-full"
                [placeholder]="i18n.t('clinic.accessRequest.dpuPlaceholder')"
                [(ngModel)]="dpuNumber"
                [disabled]="isLoading()"
              />
            </div>

            <!-- Motif -->
            <div class="space-y-1.5">
              <label for="reason" class="ui-label">{{ i18n.t('clinic.accessRequest.reasonLabel') }} <span class="text-red-500">*</span></label>
              <textarea
                id="reason"
                class="ui-textarea w-full min-h-[100px]"
                [placeholder]="i18n.t('clinic.accessRequest.reasonPlaceholder')"
                [(ngModel)]="reason"
                [disabled]="isLoading()"
              ></textarea>
            </div>

            <!-- Durée -->
            <div class="space-y-1.5">
              <label for="duration" class="ui-label">{{ i18n.t('clinic.accessRequest.durationLabel') }} <span class="text-red-500">*</span></label>
              <select
                id="duration"
                class="ui-select w-full"
                [(ngModel)]="durationHours"
                [disabled]="isLoading()"
              >
                <option value="" disabled selected>{{ i18n.t('clinic.accessRequest.durationPlaceholder') }}</option>
                @for (opt of durationOptions; track opt.value) {
                  <option [value]="opt.value">{{ localeLabel(opt) }}</option>
                }
              </select>
            </div>

            <!-- Scopes granulaires -->
            <div class="space-y-2">
              <p class="ui-label">{{ i18n.t('clinic.accessRequest.scopesLabel') }}</p>
              <div class="grid grid-cols-2 gap-2">
                @for (scope of accessScopes; track scope.key) {
                  <label class="flex items-center gap-2 text-xs text-[var(--text-primary)] cursor-pointer select-none">
                    <input
                      type="checkbox"
                      [checked]="isScopeSelected(scope.key)"
                      (change)="toggleScope(scope.key)"
                      class="ui-checkbox"
                      [disabled]="isLoading()"
                    />
                    {{ localeLabel(scope) }}
                  </label>
                }
              </div>
            </div>

            @if (error()) {
              <app-ui-alert tone="error">{{ error() }}</app-ui-alert>
            }

            <div class="flex items-center justify-between gap-3 pt-2 border-t border-[var(--app-border)]">
              <app-ui-button variant="secondary" (pressed)="goBack()" [disabled]="isLoading()">
                {{ i18n.t('common.cancel') }}
              </app-ui-button>
              <app-ui-button
                variant="primary"
                (pressed)="submit()"
                [disabled]="isLoading() || !isFormValid()"
              >
                {{ isLoading() ? i18n.t('common.saving') : i18n.t('clinic.accessRequest.submit') }}
              </app-ui-button>
            </div>
          }
        </div>
      </div>
    </app-shell>
  `
})
export class ClinicAccessRequestComponent {
  private readonly api = inject(ExternalAccessApiService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly dpuNumber = signal('');
  readonly reason = signal('');
  readonly durationHours = signal<number | ''>('');
  readonly selectedScopes = signal<string[]>(ACCESS_SCOPES.map(s => s.key));
  readonly isLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);

  readonly durationOptions = DURATION_OPTIONS;
  readonly accessScopes = ACCESS_SCOPES;

  isScopeSelected(key: string): boolean {
    return this.selectedScopes().includes(key);
  }

  toggleScope(key: string): void {
    this.selectedScopes.update(current =>
      current.includes(key) ? current.filter(s => s !== key) : [...current, key]
    );
  }

  isFormValid(): boolean {
    return this.dpuNumber().trim().length > 0
      && this.reason().trim().length > 0
      && this.durationHours() !== '';
  }

  localeLabel(item: { labelFr: string; labelEn: string }): string {
    return this.i18n.locale() === 'en' ? item.labelEn : item.labelFr;
  }

  submit(): void {
    if (!this.isFormValid()) return;

    this.isLoading.set(true);
    this.error.set(null);

    const dto: CreateExternalAccessRequestDto = {
      patientDpu: this.dpuNumber().trim(),
      reason: this.reason().trim(),
      durationHours: Number(this.durationHours()),
      scopes: this.selectedScopes().join(','),
    };

    this.api.createRequest(dto).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.success.set(true);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || err.error?.title || this.i18n.t('clinic.accessRequest.error'));
      }
    });
  }

  resetForm(): void {
    this.dpuNumber.set('');
    this.reason.set('');
    this.durationHours.set('');
    this.selectedScopes.set(ACCESS_SCOPES.map(s => s.key));
    this.error.set(null);
    this.success.set(false);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
