import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { Patient } from '../patient.models';
import { ProvisionalPatientRegularizationApiService } from '../provisional-patient-regularization-api.service';
import { IdentitySourceType, RegularizeProvisionalPatientDto } from '../regularize-provisional-patient.models';

interface IdentitySourceOption {
  value: IdentitySourceType;
  translationKey: string;
}

@Component({
  selector: 'app-patient-identity-regularization-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, AlertComponent, ButtonComponent],
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-[var(--overlay-bg)] p-3 backdrop-blur-sm sm:p-4"
      role="presentation"
      (click)="requestClose()"
    >
      <form
        class="ui-card flex max-h-[92vh] w-full max-w-2xl flex-col overflow-hidden"
        [formGroup]="form"
        (ngSubmit)="submit()"
        (click)="$event.stopPropagation()"
        role="dialog"
        aria-modal="true"
        aria-labelledby="regularization-dialog-title"
      >
        <header class="flex items-start justify-between gap-4 border-b border-[var(--app-border)] px-4 py-4 sm:px-5">
          <div>
            <h3 id="regularization-dialog-title" class="font-display text-lg font-black text-[var(--text-primary)]">
              {{ t('patient.urgTemp.regularization.title') }}
            </h3>
            <p class="mt-1 text-xs text-[var(--text-muted)]">
              {{ t('patient.urgTemp.regularization.aliasHint') }}
              <strong class="font-mono">{{ patient().temporaryPatientNumber }}</strong>
            </p>
          </div>
          <button
            type="button"
            class="rounded-sm p-2 text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)]"
            [attr.aria-label]="t('patient.urgTemp.common.close')"
            (click)="requestClose()"
          >
            ✕
          </button>
        </header>

        <div class="min-h-0 flex-1 space-y-5 overflow-y-auto px-4 py-4 sm:px-5">
          @if (error(); as message) {
            <app-ui-alert tone="error">{{ message }}</app-ui-alert>
          }

          <fieldset>
            <legend class="ui-label mb-3">{{ t('patient.urgTemp.regularization.confirmedIdentity') }}</legend>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div class="sm:col-span-2">
                <label class="ui-label mb-1.5" for="regularization-full-name">{{ t('patient.urgTemp.regularization.fullName') }} *</label>
                <input id="regularization-full-name" class="ui-input" formControlName="fullName" autocomplete="name" />
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-gender">{{ t('patient.urgTemp.regularization.gender') }} *</label>
                <select id="regularization-gender" class="ui-input" formControlName="gender">
                  <option value="">{{ t('patient.urgTemp.regularization.select') }}</option>
                  @for (gender of genders; track gender) {
                    <option [value]="gender">{{ t('patient.urgTemp.regularization.gender.' + gender) }}</option>
                  }
                </select>
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-birth-date">{{ t('patient.urgTemp.regularization.birthDate') }} *</label>
                <input
                  id="regularization-birth-date"
                  class="ui-input"
                  type="date"
                  formControlName="birthDate"
                  [max]="today"
                />
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-phone">{{ t('patient.urgTemp.regularization.phone') }}</label>
                <input id="regularization-phone" class="ui-input" formControlName="phone" autocomplete="tel" />
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-email">{{ t('patient.urgTemp.regularization.email') }}</label>
                <input id="regularization-email" class="ui-input" type="email" formControlName="email" autocomplete="email" />
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-city">{{ t('patient.urgTemp.regularization.city') }} *</label>
                <input id="regularization-city" class="ui-input" formControlName="city" autocomplete="address-level2" />
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-district">{{ t('patient.urgTemp.regularization.district') }}</label>
                <input id="regularization-district" class="ui-input" formControlName="district" />
              </div>
              <div class="sm:col-span-2">
                <label class="ui-label mb-1.5" for="regularization-address">{{ t('patient.urgTemp.regularization.address') }}</label>
                <input id="regularization-address" class="ui-input" formControlName="address" autocomplete="street-address" />
              </div>
            </div>
          </fieldset>

          <fieldset class="border-t border-[var(--divider-subtle)] pt-4">
            <legend class="ui-label mb-3">{{ t('patient.urgTemp.regularization.emergencyContact') }}</legend>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label class="ui-label mb-1.5" for="regularization-contact-name">{{ t('patient.urgTemp.contact.name') }}</label>
                <input id="regularization-contact-name" class="ui-input" formControlName="emergencyContactName" />
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-contact-phone">{{ t('patient.urgTemp.contact.phone') }}</label>
                <input id="regularization-contact-phone" class="ui-input" formControlName="emergencyContactPhone" />
              </div>
            </div>
          </fieldset>

          <fieldset class="border-t border-[var(--divider-subtle)] pt-4">
            <legend class="ui-label mb-3">{{ t('patient.urgTemp.regularization.evidence') }}</legend>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label class="ui-label mb-1.5" for="regularization-source">{{ t('patient.urgTemp.regularization.source') }} *</label>
                <select id="regularization-source" class="ui-input" formControlName="sourceType">
                  @for (option of sourceOptions; track option.value) {
                    <option [value]="option.value">{{ t(option.translationKey) }}</option>
                  }
                </select>
              </div>
              <div>
                <label class="ui-label mb-1.5" for="regularization-source-details">{{ t('patient.urgTemp.regularization.sourceDetails') }} *</label>
                <input
                  id="regularization-source-details"
                  class="ui-input"
                  formControlName="sourceDetails"
                  [placeholder]="t('patient.urgTemp.regularization.sourceDetailsPlaceholder')"
                />
              </div>
              <div class="sm:col-span-2">
                <label class="ui-label mb-1.5" for="regularization-reason">{{ t('patient.urgTemp.regularization.reason') }} *</label>
                <textarea
                  id="regularization-reason"
                  class="ui-input min-h-20 py-2.5"
                  formControlName="reason"
                  [placeholder]="t('patient.urgTemp.regularization.reasonPlaceholder')"
                ></textarea>
              </div>
            </div>
            <p class="mt-3 text-xs text-[var(--text-muted)]">{{ t('patient.urgTemp.regularization.mergeWarning') }}</p>
          </fieldset>
        </div>

        <footer class="flex flex-col-reverse gap-2 border-t border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3 sm:flex-row sm:justify-end sm:px-5">
          <app-ui-button type="button" variant="secondary" [disabled]="submitting()" (pressed)="requestClose()">
            {{ t('patient.urgTemp.common.cancel') }}
          </app-ui-button>
          <app-ui-button type="submit" variant="primary" [disabled]="submitting()">
            {{ submitting() ? t('patient.urgTemp.regularization.saving') : t('patient.urgTemp.regularization.submit') }}
          </app-ui-button>
        </footer>
      </form>
    </div>
  `,
})
export class PatientIdentityRegularizationDialogComponent implements OnInit {
  readonly patient = input.required<Patient>();
  readonly cancelled = output<void>();
  readonly saved = output<Patient>();

  private readonly fb = inject(NonNullableFormBuilder);
  private readonly regularizationApi = inject(ProvisionalPatientRegularizationApiService);
  private readonly i18n = inject(I18nService);
  private readonly apiErrors = inject(ApiErrorI18nService);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly today = new Date().toISOString().slice(0, 10);
  readonly genders = ['MASCULIN', 'FEMININ', 'AUTRE'] as const;
  readonly sourceOptions: readonly IdentitySourceOption[] = [
    { value: 'PATIENT', translationKey: 'patient.urgTemp.regularization.source.PATIENT' },
    { value: 'DOCUMENT', translationKey: 'patient.urgTemp.regularization.source.DOCUMENT' },
    { value: 'ACCOMPANYING_PERSON', translationKey: 'patient.urgTemp.regularization.source.ACCOMPANYING_PERSON' },
    { value: 'WITNESS', translationKey: 'patient.urgTemp.regularization.source.WITNESS' },
    { value: 'HEALTHCARE_PROFESSIONAL', translationKey: 'patient.urgTemp.regularization.source.HEALTHCARE_PROFESSIONAL' },
    { value: 'OTHER', translationKey: 'patient.urgTemp.regularization.source.OTHER' },
  ];

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(160)]],
    gender: ['', Validators.required],
    birthDate: ['', Validators.required],
    phone: ['', Validators.maxLength(40)],
    email: ['', [Validators.email, Validators.maxLength(190)]],
    city: ['', [Validators.required, Validators.maxLength(120)]],
    district: ['', Validators.maxLength(120)],
    address: ['', Validators.maxLength(255)],
    emergencyContactName: ['', Validators.maxLength(160)],
    emergencyContactPhone: ['', Validators.maxLength(40)],
    sourceType: ['PATIENT' as IdentitySourceType, Validators.required],
    sourceDetails: ['', [Validators.required, Validators.maxLength(500)]],
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  ngOnInit(): void {
    const currentPatient = this.patient();
    this.form.patchValue({
      gender: currentPatient.gender || '',
      birthDate: currentPatient.birthDate || '',
      phone: currentPatient.phone || '',
      email: currentPatient.email || '',
      city: currentPatient.city || '',
      district: currentPatient.district || '',
      address: currentPatient.address || '',
      emergencyContactName: currentPatient.emergencyContactName || '',
      emergencyContactPhone: currentPatient.emergencyContactPhone || '',
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  requestClose(): void {
    if (!this.submitting()) {
      this.cancelled.emit();
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      this.error.set(this.t('patient.urgTemp.regularization.required'));
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.regularizationApi.regularize(this.patient().id, this.toDto()).pipe(
      finalize(() => this.submitting.set(false)),
    ).subscribe({
      next: (updatedPatient) => this.saved.emit(updatedPatient),
      error: (err) => this.error.set(this.apiErrors.message(
        err,
        'patient.urgTemp.error',
        'patient.urgTemp.regularization.error',
      )),
    });
  }

  private toDto(): RegularizeProvisionalPatientDto {
    const value = this.form.getRawValue();
    return {
      fullName: value.fullName.trim(),
      gender: value.gender,
      birthDate: value.birthDate,
      phone: this.optional(value.phone),
      city: value.city.trim(),
      district: this.optional(value.district),
      address: this.optional(value.address),
      email: this.optional(value.email),
      emergencyContactName: this.optional(value.emergencyContactName),
      emergencyContactPhone: this.optional(value.emergencyContactPhone),
      sourceType: value.sourceType,
      sourceDetails: value.sourceDetails.trim(),
      reason: value.reason.trim(),
    };
  }

  private optional(value: string): string | undefined {
    const normalized = value.trim();
    return normalized || undefined;
  }
}
