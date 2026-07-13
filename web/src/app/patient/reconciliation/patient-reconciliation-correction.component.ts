import { Component, computed, effect, inject, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import {
  IdentitySourceType,
  PatientReconciliationCandidate,
  PatientReconciliationCorrectionDto,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';

interface CorrectionForm {
  replacementCanonicalPatientId: FormControl<string>;
  evidenceSourceType: FormControl<IdentitySourceType>;
  evidenceReference: FormControl<string>;
  justification: FormControl<string>;
  confirmed: FormControl<boolean>;
}

@Component({
  selector: 'app-patient-reconciliation-correction',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './patient-reconciliation-correction.component.html',
})
export class PatientReconciliationCorrectionComponent {
  private readonly i18n = inject(I18nService);

  readonly patient = input.required<PatientReconciliationQueueItem>();
  readonly candidates = input.required<PatientReconciliationCandidate[]>();
  readonly loadingCandidates = input(false);
  readonly disabled = input(false);
  readonly submitted = output<PatientReconciliationCorrectionDto>();

  readonly replacementCandidates = computed(() => this.candidates()
    .filter((candidate) => candidate.patientId !== this.patient().canonicalPatientId));
  readonly currentCanonicalCandidate = computed(() => this.candidates()
    .find((candidate) => candidate.patientId === this.patient().canonicalPatientId) ?? null);

  readonly form = new FormGroup<CorrectionForm>({
    replacementCanonicalPatientId: new FormControl('', { nonNullable: true }),
    evidenceSourceType: new FormControl<IdentitySourceType>('DOCUMENT', { nonNullable: true }),
    evidenceReference: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(255)],
    }),
    justification: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(1500)],
    }),
    confirmed: new FormControl(false, {
      nonNullable: true,
      validators: [Validators.requiredTrue],
    }),
  });

  readonly sourceTypes: IdentitySourceType[] = [
    'PATIENT',
    'DOCUMENT',
    'ACCOMPANYING_PERSON',
    'WITNESS',
    'TRANSPORTER',
    'HEALTHCARE_PROFESSIONAL',
    'OTHER',
  ];

  constructor() {
    effect(() => {
      if (this.disabled()) {
        this.form.disable({ emitEvent: false });
      } else {
        this.form.enable({ emitEvent: false });
      }
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  submit(): void {
    if (this.form.disabled) return;

    this.form.markAllAsTouched();
    const correctedEventId = this.patient().decisionEventId;
    if (this.form.invalid || !correctedEventId) {
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      correctedEventId,
      replacementCanonicalPatientId: value.replacementCanonicalPatientId || null,
      evidenceSourceType: value.evidenceSourceType,
      evidenceReference: value.evidenceReference.trim() || null,
      justification: value.justification.trim(),
    });
  }

  reset(): void {
    if (this.form.disabled) return;

    this.form.reset({
      replacementCanonicalPatientId: '',
      evidenceSourceType: 'DOCUMENT',
      evidenceReference: '',
      justification: '',
      confirmed: false,
    });
  }
}
