import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { VisitDetailsFieldsComponent } from '../../admission/visit-details-fields.component';
import {
  createVisitDetailsForm,
  hasRequiredVisitDetails,
  toCreateVisitRequest,
} from '../../admission/visit-details-form';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import { VisitApiService } from '../../visit/visit-api.service';

/** Ouverture d'une visite depuis le dossier patient, avec les mêmes champs que l'admission unifiée. */
@Component({
  selector: 'app-patient-visit-admission-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, VisitDetailsFieldsComponent],
  template: `
    <div class="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 p-0 backdrop-blur-xs sm:items-center sm:p-4 animate-fade-in">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="patient-visit-admission-title"
        class="max-h-[92vh] w-full overflow-y-auto rounded-t-md border border-[var(--app-border)]/80 bg-[var(--app-surface)] p-5 shadow-2xl sm:max-w-lg sm:rounded-md sm:p-6"
      >
        <div class="mb-5 flex items-start justify-between gap-4">
          <div>
            <h3 id="patient-visit-admission-title" class="font-display text-lg font-bold text-[var(--text-primary)]">
              {{ i18n.t('patient.visit.admitTitle') }}
            </h3>
            <p class="mt-1 text-xs text-[var(--text-muted)]">{{ i18n.t('patient.visit.admitSubtitle') }}</p>
          </div>
          <button
            type="button"
            class="flex min-h-11 min-w-11 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--text-secondary)]"
            [attr.aria-label]="i18n.t('common.cancel')"
            (click)="cancel()"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        @if (visitError()) {
          <div class="mb-4 rounded-md border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 text-xs font-semibold leading-relaxed text-[var(--brand-danger-text)]">
            {{ visitError() }}
          </div>
        }

        <app-visit-details-fields [form]="form" />

        <div class="mt-7 grid grid-cols-2 gap-3">
          <app-ui-button variant="secondary" (pressed)="cancel()" [disabled]="isSubmitting()">
            {{ i18n.t('common.cancel') }}
          </app-ui-button>
          <app-ui-button variant="primary" (pressed)="submitVisit()" [disabled]="isSubmitting() || !canSubmit()">
            {{ isSubmitting() ? i18n.t('common.saving') : i18n.t('patient.visit.submitLabel') }}
          </app-ui-button>
        </div>
      </section>
    </div>
  `,
})
export class PatientVisitAdmissionDialogComponent {
  readonly patientId = input.required<string>();
  readonly cancelled = output<void>();
  readonly created = output<void>();

  readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);

  readonly form = createVisitDetailsForm(inject(FormBuilder));
  readonly isSubmitting = signal(false);
  readonly visitError = signal('');

  canSubmit(): boolean {
    return hasRequiredVisitDetails(this.form.getRawValue());
  }

  cancel(): void {
    if (!this.isSubmitting()) this.cancelled.emit();
  }

  submitVisit(): void {
    if (!this.canSubmit() || this.isSubmitting()) return;
    this.isSubmitting.set(true);
    this.visitError.set('');

    this.visitApi.create(toCreateVisitRequest(this.patientId(), this.form.getRawValue())).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.created.emit();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.visitError.set(err.error?.detail || err.error?.title || this.i18n.t('patient.visit.openError'));
      },
    });
  }
}
