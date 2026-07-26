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
    <section class="overflow-hidden rounded-[8px] border border-[var(--app-border)] bg-[var(--app-surface)] shadow-sm">
      <header class="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-4 py-4 sm:px-5">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
              <h2 class="truncate text-lg font-black text-[var(--text-primary)]">
                {{ patient?.displayName || patient?.fullName || i18n.t('consultation.clinicalNote.patient', 'Patient') }}
              </h2>
              @if (patient?.birthDate) {
                <span class="text-xs font-bold text-[var(--text-secondary)]">{{ ageLabel() }}</span>
              }
              @if (patient?.gender) {
                <span class="text-xs text-[var(--text-muted)]">{{ genderLabel() }}</span>
              }
            </div>
            <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-[11px] font-semibold text-[var(--text-muted)]">
              @if (patient?.localPatientNumber || patient?.globalPatientNumber) {
                <span>DPU {{ patient?.localPatientNumber || patient?.globalPatientNumber }}</span>
              }
              @if (patient?.bloodGroup) {
                <span>{{ i18n.t('consultation.clinicalNote.bloodGroup', 'Groupe sanguin') }} : {{ patient?.bloodGroup }}</span>
              }
            </div>
          </div>

          <div class="rounded-[6px] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] px-3 py-2 lg:max-w-[44%]">
            <p class="text-[10px] font-extrabold uppercase tracking-wider text-[var(--brand-primary)]">
              {{ i18n.t('consultation.clinicalNote.reason', 'Motif de consultation') }}
            </p>
            <p class="mt-0.5 text-sm font-bold leading-5 text-[var(--text-primary)]">
              {{ visitReason || i18n.t('consultation.clinicalNote.reasonMissing', 'Motif non renseigné') }}
            </p>
          </div>
        </div>
      </header>

      <div class="grid grid-cols-1 border-b border-[var(--app-border)] lg:grid-cols-[minmax(0,1fr)_300px]">
        <div class="px-4 py-4 sm:px-5">
          <div class="flex items-center gap-2">
            <span class="inline-flex h-7 w-7 items-center justify-center rounded-[5px] bg-cyan-50 text-xs font-black text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">1</span>
            <div>
              <h3 class="text-sm font-black text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.historyTitle', 'Histoire de la maladie actuelle') }}
              </h3>
              <p class="text-[11px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.historyHelp', 'Début, évolution, symptômes associés, facteurs aggravants ou soulageants et traitements déjà essayés.') }}
              </p>
            </div>
          </div>

          <div class="mt-3" [formGroup]="form">
            <textarea
              formControlName="symptoms"
              rows="6"
              [placeholder]="i18n.t('consultation.clinicalNote.historyPlaceholder', 'Ex : douleur abdominale depuis 3 jours, début brutal, prédominant en FID, nausées sans vomissements, pas de diarrhée…')"
              class="ui-textarea min-h-36 w-full resize-y rounded-[5px] border-[var(--app-border)] bg-transparent p-3 text-sm leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
              [class.border-red-400]="form.get('symptoms')?.invalid && form.get('symptoms')?.touched"
            ></textarea>
            @if (form.get('symptoms')?.invalid && form.get('symptoms')?.touched) {
              <p class="mt-1 text-xs font-semibold text-[var(--brand-danger)]">
                {{ i18n.t('consultation.clinicalNote.historyRequired', 'L’histoire de la maladie est requise.') }}
              </p>
            }
          </div>
        </div>

        <aside class="border-t border-[var(--app-border)] bg-[var(--app-surface-muted)]/50 px-4 py-4 lg:border-l lg:border-t-0">
          <h3 class="text-xs font-black uppercase tracking-wide text-[var(--text-secondary)]">
            {{ i18n.t('consultation.clinicalNote.patientContext', 'Contexte patient') }}
          </h3>

          <div class="mt-3">
            <p class="text-[10px] font-extrabold uppercase tracking-wider text-[var(--text-muted)]">
              {{ i18n.t('consultation.clinicalNote.allergies', 'Allergies') }}
            </p>
            @if (activeAllergies().length > 0) {
              <div class="mt-1.5 flex flex-wrap gap-1.5">
                @for (allergy of activeAllergies(); track allergy.id || allergy.substance) {
                  <span class="inline-flex rounded-[4px] border border-rose-200 bg-rose-50 px-2 py-1 text-[11px] font-bold text-rose-800 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-200">
                    {{ allergy.substance }}
                    @if (allergy.severity === 'CRITICAL' || allergy.severity === 'HIGH') {
                      · {{ allergy.severity }}
                    }
                  </span>
                }
              </div>
            } @else {
              <p class="mt-1 text-xs text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.noKnownAllergy', 'Aucune allergie active connue') }}
              </p>
            }
          </div>

          <div class="mt-4">
            <p class="text-[10px] font-extrabold uppercase tracking-wider text-[var(--text-muted)]">
              {{ i18n.t('consultation.clinicalNote.history', 'Antécédents') }}
            </p>
            @if (relevantHistory().length > 0) {
              <ul class="mt-1.5 space-y-1.5">
                @for (item of relevantHistory(); track item.id || item.description) {
                  <li class="text-xs leading-5 text-[var(--text-secondary)]">
                    <span class="font-bold text-[var(--text-primary)]">{{ historyCategory(item.category) }}</span>
                    · {{ item.description }}
                  </li>
                }
              </ul>
            } @else if (patient?.medicalHistory) {
              <p class="mt-1 text-xs leading-5 text-[var(--text-secondary)]">{{ patient?.medicalHistory }}</p>
            } @else {
              <p class="mt-1 text-xs text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.noHistory', 'Aucun antécédent structuré disponible') }}
              </p>
            }
          </div>
        </aside>
      </div>

      <div class="space-y-6 px-4 py-5 sm:px-5" [formGroup]="form">
        <section>
          <div class="flex items-center gap-2">
            <span class="inline-flex h-7 w-7 items-center justify-center rounded-[5px] bg-cyan-50 text-xs font-black text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">2</span>
            <div>
              <h3 class="text-sm font-black text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.examTitle', 'Examen clinique') }}
              </h3>
              <p class="text-[11px] text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.examHelp', 'Constatations pertinentes de l’examen physique et signes négatifs importants.') }}
              </p>
            </div>
          </div>
          <textarea
            formControlName="clinicalExam"
            rows="4"
            [placeholder]="i18n.t('consultation.clinicalNote.examPlaceholder', 'Ex : abdomen souple, sensibilité en FID sans défense, bruits hydro-aériques présents…')"
            class="ui-textarea mt-3 w-full resize-y rounded-[5px] border-[var(--app-border)] bg-transparent p-3 text-sm leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
          ></textarea>
        </section>

        <section>
          <div class="flex items-center gap-2">
            <span class="inline-flex h-7 w-7 items-center justify-center rounded-[5px] bg-cyan-50 text-xs font-black text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">3</span>
            <div>
              <h3 class="text-sm font-black text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.assessmentTitle', 'Évaluation diagnostique') }}
              </h3>
              <p class="text-[11px] text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.assessmentHelp', 'Séparez les diagnostics différentiels du diagnostic finalement retenu.') }}
              </p>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div>
              <label class="ui-label text-xs font-bold">
                {{ i18n.t('consultation.clinicalNote.differential', 'Hypothèses / diagnostics différentiels') }}
              </label>
              <textarea
                formControlName="suspectedDiagnosis"
                rows="4"
                [placeholder]="i18n.t('consultation.clinicalNote.differentialPlaceholder', 'Ex : appendicite aiguë, adénolymphite mésentérique, infection urinaire…')"
                class="ui-textarea mt-1.5 w-full resize-y rounded-[5px] border-[var(--app-border)] bg-transparent p-3 text-sm leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
              ></textarea>
            </div>

            <div class="rounded-[6px] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-3">
              <label class="ui-label text-xs font-black text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.retainedDiagnosis', 'Diagnostic retenu') }}
                <span class="text-[var(--brand-danger)]">*</span>
              </label>
              <textarea
                formControlName="diagnosis"
                rows="4"
                [placeholder]="i18n.t('consultation.clinicalNote.retainedDiagnosisPlaceholder', 'Diagnostic principal retenu après l’évaluation clinique')"
                class="ui-textarea mt-1.5 w-full resize-y rounded-[5px] border-[var(--brand-primary-border)] bg-[var(--app-surface)] p-3 text-sm font-semibold leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
                [class.border-red-400]="form.get('diagnosis')?.invalid && form.get('diagnosis')?.touched"
              ></textarea>
              @if (form.get('diagnosis')?.invalid && form.get('diagnosis')?.touched) {
                <p class="mt-1 text-xs font-semibold text-[var(--brand-danger)]">
                  {{ i18n.t('consultation.clinicalNote.diagnosisRequired', 'Le diagnostic retenu est requis.') }}
                </p>
              }
            </div>
          </div>
        </section>

        <section>
          <div class="flex items-center gap-2">
            <span class="inline-flex h-7 w-7 items-center justify-center rounded-[5px] bg-cyan-50 text-xs font-black text-cyan-700 dark:bg-cyan-950/30 dark:text-cyan-300">4</span>
            <div>
              <h3 class="text-sm font-black text-[var(--text-primary)]">
                {{ i18n.t('consultation.clinicalNote.summaryTitle', 'Synthèse et conduite à tenir') }}
              </h3>
              <p class="text-[11px] text-[var(--text-muted)]">
                {{ i18n.t('consultation.clinicalNote.summaryHelp', 'Résumé clinique, consignes données au patient et organisation du suivi.') }}
              </p>
            </div>
          </div>

          <div class="mt-3 space-y-4">
            <div>
              <label class="ui-label text-xs font-bold">
                {{ i18n.t('consultation.clinicalNote.summary', 'Synthèse clinique') }}
              </label>
              <textarea
                formControlName="conclusion"
                rows="3"
                [placeholder]="i18n.t('consultation.clinicalNote.summaryPlaceholder', 'Synthèse courte de la situation, niveau de gravité, décision et éléments à surveiller.')"
                class="ui-textarea mt-1.5 w-full resize-y rounded-[5px] border-[var(--app-border)] bg-transparent p-3 text-sm leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
              ></textarea>
            </div>

            <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
              <div>
                <label class="ui-label text-xs font-bold">
                  {{ i18n.t('consultation.clinicalNote.instructions', 'Conseils et consignes') }}
                </label>
                <textarea
                  formControlName="advice"
                  rows="3"
                  [placeholder]="i18n.t('consultation.clinicalNote.instructionsPlaceholder', 'Signes d’alerte, mesures hygiéno-diététiques, consignes de retour…')"
                  class="ui-textarea mt-1.5 w-full resize-y rounded-[5px] border-[var(--app-border)] bg-transparent p-3 text-sm leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
                ></textarea>
              </div>

              <div>
                <label class="ui-label text-xs font-bold">
                  {{ i18n.t('consultation.clinicalNote.followUp', 'Suivi / contrôle') }}
                </label>
                <textarea
                  formControlName="followUp"
                  rows="3"
                  [placeholder]="i18n.t('consultation.clinicalNote.followUpPlaceholder', 'Ex : contrôle dans 48 h, revoir avec résultats, consultation spécialisée…')"
                  class="ui-textarea mt-1.5 w-full resize-y rounded-[5px] border-[var(--app-border)] bg-transparent p-3 text-sm leading-6 text-[var(--text-primary)] focus:border-[var(--brand-primary)]"
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
    return age >= 0 ? `${age} ${this.i18n.t('consultation.clinicalNote.years', 'ans')}` : '';
  }

  genderLabel(): string {
    const gender = this.patient?.gender?.trim().toUpperCase();
    if (gender === 'M' || gender === 'MALE' || gender === 'HOMME') {
      return this.i18n.t('consultation.clinicalNote.male', 'Homme');
    }
    if (gender === 'F' || gender === 'FEMALE' || gender === 'FEMME') {
      return this.i18n.t('consultation.clinicalNote.female', 'Femme');
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
      MEDICAL: this.i18n.t('consultation.clinicalNote.historyMedical', 'Médical'),
      SURGICAL: this.i18n.t('consultation.clinicalNote.historySurgical', 'Chirurgical'),
      FAMILY: this.i18n.t('consultation.clinicalNote.historyFamily', 'Familial'),
      OBSTETRICAL: this.i18n.t('consultation.clinicalNote.historyObstetrical', 'Obstétrical'),
      OTHER: this.i18n.t('consultation.clinicalNote.historyOther', 'Autre'),
      ALLERGIC: this.i18n.t('consultation.clinicalNote.historyAllergic', 'Allergique'),
      SOCIAL: this.i18n.t('consultation.clinicalNote.historySocial', 'Social'),
    };
    return labels[category];
  }
}
