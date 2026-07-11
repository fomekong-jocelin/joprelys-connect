import { Component, computed, inject, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import {
  IdentitySourceType,
  PatientReconciliationCandidate,
  PatientReconciliationDecision,
  PatientReconciliationDecisionDto,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';

interface DecisionForm {
  decision: FormControl<PatientReconciliationDecision>;
  candidatePatientId: FormControl<string>;
  evidenceSourceType: FormControl<IdentitySourceType>;
  evidenceReference: FormControl<string>;
  justification: FormControl<string>;
}

@Component({
  selector: 'app-patient-reconciliation-decision',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './patient-reconciliation-decision.component.html',
})
export class PatientReconciliationDecisionComponent {
  private readonly i18n = inject(I18nService);

  readonly patient = input.required<PatientReconciliationQueueItem>();
  readonly candidates = input.required<PatientReconciliationCandidate[]>();
  readonly loadingCandidates = input(false);
  readonly disabled = input(false);
  readonly submitted = output<PatientReconciliationDecisionDto>();

  readonly form = new FormGroup<DecisionForm>({
    decision: new FormControl<PatientReconciliationDecision>('DEFER', { nonNullable: true }),
    candidatePatientId: new FormControl('', { nonNullable: true }),
    evidenceSourceType: new FormControl<IdentitySourceType>('DOCUMENT', { nonNullable: true }),
    evidenceReference: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    justification: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(1500)],
    }),
  });

  readonly requiresCandidate = computed(() => this.form.controls.decision.value === 'LINK_EXISTING_DPU');

  readonly sourceTypes: IdentitySourceType[] = [
    'PATIENT',
    'DOCUMENT',
    'ACCOMPANYING_PERSON',
    'WITNESS',
    'TRANSPORTER',
    'HEALTHCARE_PROFESSIONAL',
    'OTHER',
  ];

  t(key: string): string {
    return this.i18n.t(key);
  }

  formatDate(value: string | null): string {
    if (!value) return this.t('patientReconciliation.notRecorded');
    return new Intl.DateTimeFormat(this.i18n.locale(), { dateStyle: 'medium' }).format(new Date(value));
  }

  chooseDecision(decision: PatientReconciliationDecision): void {
    this.form.controls.decision.setValue(decision);
    if (decision !== 'LINK_EXISTING_DPU') {
      this.form.controls.candidatePatientId.setValue('');
    }
  }

  submit(): void {
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (this.form.invalid || (value.decision === 'LINK_EXISTING_DPU' && !value.candidatePatientId)) {
      return;
    }

    this.submitted.emit({
      decision: value.decision,
      candidatePatientId: value.candidatePatientId || null,
      evidenceSourceType: value.evidenceSourceType,
      evidenceReference: value.evidenceReference.trim() || null,
      justification: value.justification.trim(),
    });
  }

  reset(): void {
    this.form.reset({
      decision: 'DEFER',
      candidatePatientId: '',
      evidenceSourceType: 'DOCUMENT',
      evidenceReference: '',
      justification: '',
    });
  }
}
