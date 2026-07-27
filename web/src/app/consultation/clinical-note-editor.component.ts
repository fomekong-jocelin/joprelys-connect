import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import {
  Patient,
  PatientAllergy,
  PatientMedicalHistory,
} from '../patient/patient.models';

@Component({
  selector: 'app-clinical-note-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="ui-card overflow-hidden">
      <header class="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-4 py-3 sm:px-5 sm:py-4">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div class="min-w-0">
            <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
              <h2 class="ui-title truncate text-base sm:text-lg">
                {{ patient?.displayName || patient?.fullName || i18n.t('consultation.clinicalNote.patient') }}
              </h2>
              @if (patient?.birthDate) {
                <span class="text-xs font-semibold text-[var(--text-secondary)]">{{ ageLabel() }}</span>
              }
              @if (patient?.gender) {
                <span class="text-xs text-[var(--text-muted)]">{{ genderLabel() }}</span>
              }
            </div>
            <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-[11px] font-semibold text-[var(--text-muted)]">
              @if (patient?.localPatientNumber || patient?.globalPatientNumber) {
                <span>{{ i18n.t('consultation.clinicalNote.dpu') }} {{ patient?.localPatientNumber || patient?.globalPatientNumber }}</span>
              }
              @if (patient?.bloodGroup) {
                <span>{{ i18n.t('consultation.clinicalNote.bloodGroup') }} : {{ patient?.bloodGroup }}</span>
              }
            </div>
          </div>

          <div class="rounded-[var(--radius-brand-md)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] px-3 py-2 lg:max-w-[44%]">
            <p class="ui-label text-[var(--brand-primary)]">
              {{ i18n.t('consultation.clinicalNote.reason') }}
            </p>
            <p class="mt-0.5 text-sm font-semibold leading-5 text-[var(--text-primary)]">
              {{ visitReason || i18n.t('consultation.clinicalNote.reasonMissing') }}
            </p>
          </div>
        </div>
      </header>

      <div class="grid grid-cols-1 border-b border-[var(--app-border)] lg:grid-cols-[minmax(0,1fr)_280px]">
        <div class="px-4 py-4 sm:px-5">
          <div class="flex items-start gap-3">
            <span class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary-subtle)] text-xs font-extrabold text-[var(--brand-primary)]">1</span>
            <div>
              <h3 class="text-sm font-bold text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.historyTitle') }}
              </h3>
              <p class="mt-0.5 text-[11px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.historyHelp') }}
              </p>
            </div>
          </div>

          <div class="mt-3" [formGroup]="form">
            <textarea
              formControlName="symptoms"
              rows="5"
              [placeholder]="i18n.t('consultation.clinicalNote.historyPlaceholder')"
              class="ui-textarea min-h-28 resize-y"
              [attr.aria-invalid]="form.get('symptoms')?.invalid && form.get('symptoms')?.touched"
            ></textarea>
            @if (form.get('symptoms')?.invalid && form.get('symptoms')?.touched) {
              <p class="mt-1 text-xs font-semibold text-[var(--brand-danger-text)]">
                {{ i18n.t('consultation.clinicalNote.historyRequired') }}
              </p>
            }
          </div>
        </div>

        <aside class="border-t border-[var(--app-border)] bg-[var(--app-surface-muted)] px-4 py-4 lg:border-l lg:border-t-0">
          <h3 class="ui-label text-[var(--text-secondary)]">
            {{ i18n.t('consultation.clinicalNote.patientContext') }}
          </h3>

          <div class="mt-3">
            <p class="ui-label">{{ i18n.t('consultation.clinicalNote.allergies') }}</p>
            @if (activeAllergies().length > 0) {
              <div class="mt-2 flex flex-wrap gap-1.5">
                @for (allergy of activeAllergies(); track allergy.id || allergy.substance) {
                  <span class="inline-flex rounded-[var(--radius-brand-sm)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] px-2 py-1 text-[11px] font-semibold text-[var(--brand-danger-text)]">
                    {{ allergy.substance }}
                    @if (allergy.severity === 'CRITICAL' || allergy.severity === 'HIGH') {
                      · {{ allergy.severity }}
                    }
                  </span>
                }
              </div>
            } @else {
              <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.noKnownAllergy') }}
              </p>
            }
          </div>

          <div class="mt-4">
            <p class="ui-label">{{ i18n.t('consultation.clinicalNote.history') }}</p>
            @if (relevantHistory().length > 0) {
              <ul class="mt-1.5 space-y-1.5">
                @for (item of relevantHistory(); track item.id || item.description) {
                  <li class="text-xs leading-5 text-[var(--text-secondary)]">
                    <span class="font-semibold text-[var(--text-primary)]">{{ historyCategory(item.category) }}</span>
                    · {{ item.description }}
                  </li>
                }
              </ul>
            } @else if (patient?.medicalHistory) {
              <p class="mt-1 text-xs leading-5 text-[var(--text-secondary)]">{{ patient?.medicalHistory }}</p>
            } @else {
              <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.noHistory') }}
              </p>
            }
          </div>
        </aside>
      </div>

      <div class="space-y-5 px-4 py-4 sm:px-5 sm:py-5" [formGroup]="form">
        <section>
          <div class="flex items-start gap-3">
            <span class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary-subtle)] text-xs font-extrabold text-[var(--brand-primary)]">2</span>
            <div>
              <h3 class="text-sm font-bold text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.examTitle') }}
              </h3>
              <p class="mt-0.5 text-[11px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.examHelp') }}
              </p>
            </div>
          </div>
          <textarea
            formControlName="clinicalExam"
            rows="4"
            [placeholder]="i18n.t('consultation.clinicalNote.examPlaceholder')"
            class="ui-textarea mt-3 resize-y"
          ></textarea>
        </section>

        <div class="border-t border-[var(--divider-subtle)]"></div>

        <section>
          <div class="flex items-start gap-3">
            <span class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary-subtle)] text-xs font-extrabold text-[var(--brand-primary)]">3</span>
            <div>
              <h3 class="text-sm font-bold text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.assessmentTitle') }}
              </h3>
              <p class="mt-0.5 text-[11px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.assessmentHelp') }}
              </p>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div>
              <label class="ui-label">
                {{ i18n.t('consultation.clinicalNote.differential') }}
              </label>
              <textarea
                formControlName="suspectedDiagnosis"
                rows="4"
                [placeholder]="i18n.t('consultation.clinicalNote.differentialPlaceholder')"
                class="ui-textarea mt-1.5 resize-y"
              ></textarea>
            </div>

            <div>
              <label class="ui-label">
                {{ i18n.t('consultation.clinicalNote.retainedDiagnosis') }}
                <span class="text-[var(--brand-danger-text)]">*</span>
              </label>
              <textarea
                formControlName="diagnosis"
                rows="4"
                [placeholder]="i18n.t('consultation.clinicalNote.retainedDiagnosisPlaceholder')"
                class="ui-textarea mt-1.5 resize-y"
                [attr.aria-invalid]="form.get('diagnosis')?.invalid && form.get('diagnosis')?.touched"
              ></textarea>
              @if (form.get('diagnosis')?.invalid && form.get('diagnosis')?.touched) {
                <p class="mt-1 text-xs font-semibold text-[var(--brand-danger-text)]">
                  {{ i18n.t('consultation.clinicalNote.diagnosisRequired') }}
                </p>
              }
            </div>
          </div>
        </section>

        <div class="border-t border-[var(--divider-subtle)]"></div>

        <section>
          <div class="flex items-start gap-3">
            <span class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary-subtle)] text-xs font-extrabold text-[var(--brand-primary)]">4</span>
            <div>
              <h3 class="text-sm font-bold text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.summaryTitle') }}
              </h3>
              <p class="mt-0.5 text-[11px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.summaryHelp') }}
              </p>
            </div>
          </div>

          <div class="mt-3 space-y-4">
            <div>
              <label class="ui-label">{{ i18n.t('consultation.clinicalNote.summary') }}</label>
              <textarea
                formControlName="conclusion"
                rows="3"
                [placeholder]="i18n.t('consultation.clinicalNote.summaryPlaceholder')"
                class="ui-textarea mt-1.5 resize-y"
              ></textarea>
            </div>

            <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
              <div>
                <label class="ui-label">{{ i18n.t('consultation.clinicalNote.instructions') }}</label>
                <textarea
                  formControlName="advice"
                  rows="3"
                  [placeholder]="i18n.t('consultation.clinicalNote.instructionsPlaceholder')"
                  class="ui-textarea mt-1.5 resize-y"
                ></textarea>
              </div>

              <div>
                <label class="ui-label">{{ i18n.t('consultation.clinicalNote.followUp') }}</label>
                <textarea
                  formControlName="followUp"
                  rows="3"
                  [placeholder]="i18n.t('consultation.clinicalNote.followUpPlaceholder')"
                  class="ui-textarea mt-1.5 resize-y"
                ></textarea>
              </div>
            </div>
          </div>
        </section>
      </div>
    </section>
  `,
})
export class ClinicalNoteEditorComponent {
  readonly i18n = inject(I18nService);

  @Input({ required: true }) form!: FormGroup;
  @Input() patient: Patient | null = null;
  @Input() visitReason = '';
  @Input() allergies: PatientAllergy[] = [];
  @Input() medicalHistory: PatientMedicalHistory[] = [];

  ageLabel(): string {
    const birthDate = this.patient?.birthDate;
    if (!birthDate) return '';
    const birth = new Date(birthDate);
    if (Number.isNaN(birth.getTime())) return '';
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const month = today.getMonth() - birth.getMonth();
    if (month < 0 || (month === 0 && today.getDate() < birth.getDate())) age -= 1;
    return age >= 0 ? `${age} ${this.i18n.t('consultation.clinicalNote.years')}` : '';
  }

  genderLabel(): string {
    const gender = this.patient?.gender?.trim().toUpperCase();
    if (gender === 'M' || gender === 'MALE' || gender === 'HOMME') {
      return this.i18n.t('consultation.clinicalNote.male');
    }
    if (gender === 'F' || gender === 'FEMALE' || gender === 'FEMME') {
      return this.i18n.t('consultation.clinicalNote.female');
    }
    return this.patient?.gender || '';
  }

  activeAllergies(): PatientAllergy[] {
    return this.allergies
      .filter(item => item.status === 'ACTIVE')
      .slice(0, 6);
  }

  relevantHistory(): PatientMedicalHistory[] {
    return [...this.medicalHistory]
      .sort((left, right) => Number(!!right.important) - Number(!!left.important))
      .slice(0, 6);
  }

  historyCategory(category: PatientMedicalHistory['category']): string {
    const labels: Record<PatientMedicalHistory['category'], string> = {
      MEDICAL: this.i18n.t('consultation.clinicalNote.historyMedical'),
      SURGICAL: this.i18n.t('consultation.clinicalNote.historySurgical'),
      FAMILY: this.i18n.t('consultation.clinicalNote.historyFamily'),
      OBSTETRICAL: this.i18n.t('consultation.clinicalNote.historyObstetrical'),
      OTHER: this.i18n.t('consultation.clinicalNote.historyOther'),
      ALLERGIC: this.i18n.t('consultation.clinicalNote.historyAllergic'),
      SOCIAL: this.i18n.t('consultation.clinicalNote.historySocial'),
    };
    return labels[category];
  }
}
