import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { ExamType } from '../clinic/lab/lab.models';
import { I18nService } from '../core/i18n/i18n.service';
import {
  Patient,
  PatientAllergy,
  PatientMedicalHistory,
} from '../patient/patient.models';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { VisitApiService } from '../visit/visit-api.service';
import { Visit, Vitals } from '../visit/visit.models';
import {
  AiConsultationDraft,
  AiPrescriptionLine,
  AiVitalsDraft,
} from './ai-consultation-api.service';
import { ClinicalNoteEditorComponent } from './clinical-note-editor.component';
import { ConsultationApiService } from './consultation-api.service';
import { Consultation, Prescription } from './consultation.models';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';

interface CommonExam {
  code: string;
  labelKey: string;
}

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AppShellComponent,
    VoiceAssistantPanelComponent,
    ClinicalNoteEditorComponent,
  ],
  templateUrl: './consultation.component.html',
})
export class ConsultationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly patientApi = inject(import('../patient/patient-api.service').then ? null as never : null as never);
  private readonly labOrderApi = inject(LabOrderApiService);
  readonly i18n = inject(I18nService);

  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isClosing = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly vitals = signal<Vitals | null>(null);
  readonly consultation = signal<Consultation | null>(null);
  readonly prescription = signal<Prescription | null>(null);
  readonly visitNumber = signal('');
  readonly visitReason = signal('');
  readonly patient = signal<Patient | null>(null);
  readonly patientAllergies = signal<PatientAllergy[]>([]);
  readonly patientMedicalHistory = signal<PatientMedicalHistory[]>([]);

  visitId = '';
  private patientId = '';
  shouldCloseAfterSave = false;

  readonly commonExams: readonly CommonExam[] = [
    { code: 'NFS', labelKey: 'consultation.lab.common.nfs' },
    { code: 'Glycémie à jeun', labelKey: 'consultation.lab.common.fastingGlucose' },
    { code: 'Créatinine', labelKey: 'consultation.lab.common.creatinine' },
    { code: 'Urée', labelKey: 'consultation.lab.common.urea' },
    { code: 'Bilan Lipidique', labelKey: 'consultation.lab.common.lipidPanel' },
    { code: 'Transaminases', labelKey: 'consultation.lab.common.transaminases' },
    { code: 'CRP', labelKey: 'consultation.lab.common.crp' },
    { code: 'ECBU', labelKey: 'consultation.lab.common.ecbu' },
    { code: 'HbA1c', labelKey: 'consultation.lab.common.hba1c' },
  ];

  readonly form: FormGroup = this.fb.group({
    symptoms: ['', Validators.required],
    clinicalExam: [''],
    suspectedDiagnosis: [''],
    diagnosis: ['', Validators.required],
    // Compatibilité API historique : le médecin ne voit qu'un seul diagnostic retenu.
    finalDiagnosis: [''],
    conclusion: [''],
    advice: [''],
    followUp: [''],
    prescription: this.fb.array([]),
    exams: this.fb.array([]),
    labPriority: ['NORMALE'],
    labReason: [''],
  });

  get prescriptionItems(): FormArray {
    return this.form.get('prescription') as FormArray;
  }

  get labExams(): FormArray {
    return this.form.get('exams') as FormArray;
  }

  ngOnInit(): void {
    this.visitId = this.route.snapshot.paramMap.get('visitId') ?? '';
    this.loadData();
  }

  currentVoiceDraft(): Record<string, unknown> {
    const raw = this.form.getRawValue();
    return {
      ...raw,
      finalDiagnosis: raw.diagnosis || '',
      vitals: this.vitals() ?? {},
    };
  }

  commonExamLabel(exam: CommonExam): string {
    return this.i18n.t(exam.labelKey);
  }

  prescriptionStatusLabel(status?: string | null): string {
    if (!status) return '';
    const keyByStatus: Record<string, string> = {
      DRAFT: 'consultation.prescription.statusDraft',
      ACTIVE: 'consultation.prescription.statusActive',
      CANCELLED: 'consultation.prescription.statusCancelled',
      EXPIRED: 'consultation.prescription.statusExpired',
    };
    return this.i18n.t(keyByStatus[status] ?? 'consultation.prescription.statusUnknown');
  }

  prescriptionStatusClasses(status?: string | null): string {
    if (status === 'ACTIVE') {
      return 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    }
    if (status === 'DRAFT') {
      return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    }
    return 'border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)]';
  }

  painLabel(): string {
    const pain = this.vitals()?.painScale;
    if (pain === undefined || pain === null) return '';
    if (pain === 0) return this.i18n.t('consultation.vitals.painNone');
    if (pain <= 3) return this.i18n.t('consultation.vitals.painMild');
    if (pain <= 6) return this.i18n.t('consultation.vitals.painModerate');
    return this.i18n.t('consultation.vitals.painSevere');
  }

  painClasses(): string {
    const pain = this.vitals()?.painScale ?? 0;
    if (pain <= 3) {
      return 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    }
    if (pain <= 6) {
      return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    }
    return 'border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]';
  }

  applyAiDraft(draft: AiConsultationDraft): void {
    const acceptedDraft: Record<string, string> = {};
    const textFields: Array<keyof AiConsultationDraft> = [
      'symptoms',
      'clinicalExam',
      'suspectedDiagnosis',
      'conclusion',
      'advice',
      'followUp',
    ];
    textFields.forEach(field => {
      const value = draft[field];
      if (typeof value === 'string') acceptedDraft[field] = value.trim();
    });

    const retainedDiagnosis = [draft.finalDiagnosis, draft.diagnosis]
      .find(value => typeof value === 'string' && value.trim())
      ?.trim();
    if (retainedDiagnosis) {
      acceptedDraft['diagnosis'] = retainedDiagnosis;
      acceptedDraft['finalDiagnosis'] = retainedDiagnosis;
    }

    if (Object.keys(acceptedDraft).length > 0) {
      this.form.patchValue(acceptedDraft);
    }

    try {
      if (draft.prescription) this.applyAiPrescription(draft.prescription);
      if (draft.labOrders) this.applyAiLabOrders(draft.labOrders);
      if (draft.vitals) this.applyAiVitals(draft.vitals);
    } catch {
      this.errorMessage.set(this.i18n.t('consultation.ai.applyInvalidStructuredProposal'));
      return;
    }

    this.form.markAsDirty();
    this.successMessage.set(this.i18n.t('consultation.ai.applySuccessReview'));
    this.errorMessage.set('');
  }

  private applyAiPrescription(raw: string): void {
    const parsed = JSON.parse(raw) as AiPrescriptionLine[];
    if (!Array.isArray(parsed)) throw new Error('Invalid prescription');
    const presc = this.prescription();
    if (presc && presc.status !== 'DRAFT') throw new Error('Prescription finalized');

    this.prescriptionItems.clear();
    parsed.forEach(item => {
      if (!item || typeof item.drugName !== 'string' || !item.drugName.trim()) return;
      this.prescriptionItems.push(this.fb.group({
        drugName: [item.drugName.trim(), Validators.required],
        dosage: [item.dosage?.trim() ?? '', Validators.required],
        posology: [item.posology?.trim() ?? ''],
        duration: [item.duration?.trim() ?? ''],
        quantity: [item.quantity?.trim() ?? ''],
        instructions: [item.instructions?.trim() ?? ''],
        form: [item.form?.trim() ?? ''],
        route: [item.route?.trim() ?? ''],
        frequency: [item.frequency?.trim() ?? ''],
        substitutionAllowed: [item.substitutionAllowed !== false],
      }));
    });
  }

  private applyAiLabOrders(raw: string): void {
    const parsed = JSON.parse(raw) as unknown[];
    if (!Array.isArray(parsed)) throw new Error('Invalid lab orders');
    this.labExams.clear();
    parsed.forEach(item => {
      if (typeof item === 'string' && item.trim()) this.addLabExam(item.trim());
    });
  }

  private applyAiVitals(raw: string): void {
    const parsed = JSON.parse(raw) as AiVitalsDraft;
    if (!parsed || typeof parsed !== 'object') throw new Error('Invalid vitals');
    const vitals: Vitals = {};
    const numericFields: Array<keyof AiVitalsDraft> = [
      'temperature', 'weight', 'height', 'pulse', 'systolic', 'diastolic',
      'spo2', 'glycemia', 'respiratoryRate', 'painScale',
    ];
    numericFields.forEach(field => {
      const value = parsed[field];
      if (typeof value === 'number' && Number.isFinite(value)) vitals[field] = value;
    });
    if (Object.keys(vitals).length === 0) return;

    this.visitApi.saveVitals(this.visitId, vitals).subscribe({
      next: saved => this.vitals.set(saved),
      error: err => {
        this.errorMessage.set(
          err.error?.detail
          || err.error?.title
          || this.i18n.t('consultation.ai.vitalsSaveFailed'),
        );
      },
    });
  }

  private loadData(): void {
    if (!this.visitId) return;
    this.isLoading.set(true);

    this.http.get<{ visitNumber?: string } & Vitals>(`/api/visits/${this.visitId}/vitals`).subscribe({
      next: data => {
        this.vitals.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });

    this.http.get<Visit>(`/api/visits/${this.visitId}`).subscribe({
      next: visit => {
        if (visit?.visitNumber) this.visitNumber.set(visit.visitNumber);
        this.visitReason.set(visit?.reason || '');

        if (visit?.patientId) {
          this.patientId = visit.patientId;
          this.loadPatientContext(this.patientId);
          this.labOrderApi.getPatientLabOrders(this.patientId).subscribe({
            next: orders => {
              const currentVisitOrder = orders.find(order => order.visitId === this.visitId);
              if (!currentVisitOrder) return;
              this.labExams.clear();
              currentVisitOrder.exams.forEach(exam => this.addLabExam(exam));
              this.form.patchValue({
                labPriority: currentVisitOrder.priority,
                labReason: currentVisitOrder.reason || '',
              });
            },
          });
        }
      },
      error: () => undefined,
    });

    this.consultationApi.getConsultation(this.visitId).subscribe({
      next: existing => {
        this.consultation.set(existing);
        const retainedDiagnosis = existing.finalDiagnosis?.trim() || existing.diagnosis || '';
        this.form.patchValue({
          symptoms: existing.symptoms,
          clinicalExam: existing.clinicalExam ?? '',
          suspectedDiagnosis: existing.suspectedDiagnosis ?? '',
          diagnosis: retainedDiagnosis,
          finalDiagnosis: retainedDiagnosis,
          conclusion: existing.conclusion ?? '',
          advice: existing.advice ?? '',
          followUp: existing.followUp ?? '',
        });
        this.loadPrescription(existing.id);
      },
      error: () => undefined,
    });
  }

  private loadPatientContext(patientId: string): void {
    this.patientApi.getById(patientId).subscribe({
      next: patient => this.patient.set(patient),
      error: () => this.patient.set(null),
    });

    this.patientApi.getAllergies(patientId).subscribe({
      next: allergies => this.patientAllergies.set(allergies),
      error: () => this.patientAllergies.set([]),
    });

    this.patientApi.getMedicalHistory(patientId).subscribe({
      next: history => this.patientMedicalHistory.set(history),
      error: () => this.patientMedicalHistory.set([]),
    });
  }

  loadPrescription(consultationId: string): void {
    this.consultationApi.getPrescription(consultationId).subscribe({
      next: presc => {
        this.prescription.set(presc);
        this.prescriptionItems.clear();
        presc.items.forEach(item => {
          this.prescriptionItems.push(this.fb.group({
            drugName: [{ value: item.drugName, disabled: presc.status !== 'DRAFT' }, Validators.required],
            dosage: [{ value: item.dosage, disabled: presc.status !== 'DRAFT' }, Validators.required],
            posology: [{ value: item.posology || '', disabled: presc.status !== 'DRAFT' }],
            duration: [{ value: item.duration || '', disabled: presc.status !== 'DRAFT' }],
            quantity: [{ value: item.quantity || '', disabled: presc.status !== 'DRAFT' }],
            instructions: [{ value: item.instructions || '', disabled: presc.status !== 'DRAFT' }],
            form: [{ value: item.form || '', disabled: presc.status !== 'DRAFT' }],
            route: [{ value: item.route || '', disabled: presc.status !== 'DRAFT' }],
            frequency: [{ value: item.frequency || '', disabled: presc.status !== 'DRAFT' }],
            substitutionAllowed: [{ value: item.substitutionAllowed !== false, disabled: presc.status !== 'DRAFT' }],
          }));
        });
      },
      error: () => this.prescription.set(null),
    });
  }

  addPrescriptionLine(): void {
    const presc = this.prescription();
    if (presc && presc.status !== 'DRAFT') return;

    this.prescriptionItems.push(this.fb.group({
      drugName: ['', Validators.required],
      dosage: ['', Validators.required],
      posology: [''],
      duration: [''],
      quantity: [''],
      instructions: [''],
      form: [''],
      route: [''],
      frequency: [''],
      substitutionAllowed: [true],
    }));
  }

  removePrescriptionLine(index: number): void {
    const presc = this.prescription();
    if (presc && presc.status !== 'DRAFT') return;
    this.prescriptionItems.removeAt(index);
  }

  addLabExam(examName: string = ''): void {
    if (!examName.trim()) return;
    const exists = this.labExams.controls.some(
      ctrl => ctrl.value.toLowerCase() === examName.trim().toLowerCase(),
    );
    if (!exists) this.labExams.push(this.fb.control(examName.trim(), Validators.required));
  }

  removeLabExam(index: number): void {
    this.labExams.removeAt(index);
  }

  finalizePrescription(): void {
    const presc = this.prescription();
    if (!presc) return;

    this.isLoading.set(true);
    this.consultationApi.finalizePrescription(presc.id).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.successMessage.set(this.i18n.t('consultation.prescription.finalizedSuccess'));
        this.loadPrescription(presc.consultationId);
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error?.detail || this.i18n.t('consultation.errors.finalizePrescription'),
        );
      },
    });
  }

  cancelPrescription(): void {
    const presc = this.prescription();
    if (!presc) return;
    if (!confirm(this.i18n.t('consultation.prescription.confirmCancel'))) return;

    this.isLoading.set(true);
    this.consultationApi.cancelPrescription(presc.id).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.successMessage.set(this.i18n.t('consultation.prescription.cancelledSuccess'));
        this.loadPrescription(presc.consultationId);
      },
      error: err => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error?.detail || this.i18n.t('consultation.errors.cancelPrescription'),
        );
      },
    });
  }

  downloadPrescriptionPdf(): void {
    const presc = this.prescription();
    if (!presc || !presc.documentId) return;

    this.isLoading.set(true);
    this.consultationApi.downloadDocumentById(presc.documentId).subscribe({
      next: blob => {
        this.isLoading.set(false);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ordonnance-${presc.prescriptionNumber}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set(this.i18n.t('consultation.errors.downloadPrescription'));
      },
    });
  }

  onSave(closeVisitAfter: boolean = false): void {
    if (this.form.invalid || this.isSaving() || this.isClosing()) return;

    this.shouldCloseAfterSave = closeVisitAfter;
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const {
      symptoms,
      clinicalExam,
      suspectedDiagnosis,
      diagnosis,
      conclusion,
      advice,
      followUp,
    } = this.form.getRawValue();
    const retainedDiagnosis = typeof diagnosis === 'string' ? diagnosis.trim() : diagnosis;
    this.form.patchValue({ finalDiagnosis: retainedDiagnosis || '' }, { emitEvent: false });

    this.consultationApi.saveConsultation(this.visitId, {
      symptoms,
      clinicalExam: clinicalExam || undefined,
      suspectedDiagnosis: suspectedDiagnosis || undefined,
      diagnosis: retainedDiagnosis,
      finalDiagnosis: retainedDiagnosis || undefined,
      conclusion: conclusion || undefined,
      advice: advice || undefined,
      followUp: followUp || undefined,
    }).subscribe({
      next: savedConsultation => {
        this.consultation.set(savedConsultation);

        const prescriptionLines = this.prescriptionItems.getRawValue();
        const examsLines = this.labExams.value;

        let successMsg = this.i18n.t('consultation.success.saved');
        if (prescriptionLines.length > 0 && examsLines.length > 0) {
          successMsg = this.i18n.t('consultation.success.savedAll');
        } else if (prescriptionLines.length > 0) {
          successMsg = this.i18n.t('consultation.success.savedPrescription');
        } else if (examsLines.length > 0) {
          successMsg = this.i18n.t('consultation.success.savedLab');
        }

        const presc = this.prescription();
        if (prescriptionLines.length > 0 && (!presc || presc.status === 'DRAFT')) {
          this.consultationApi.savePrescription(savedConsultation.id, {
            items: prescriptionLines,
          }).subscribe({
            next: savedPresc => {
              this.prescription.set(savedPresc);
              this.saveLabOrderAndComplete(closeVisitAfter, successMsg);
            },
            error: err => {
              this.isSaving.set(false);
              this.errorMessage.set(
                err.error?.detail || this.i18n.t('consultation.errors.savePrescription'),
              );
            },
          });
        } else {
          this.saveLabOrderAndComplete(closeVisitAfter, successMsg);
        }
      },
      error: err => {
        this.isSaving.set(false);
        this.errorMessage.set(
          err.error?.detail
          || err.error?.title
          || this.i18n.t('consultation.errors.saveConsultation'),
        );
      },
    });
  }

  private saveLabOrderAndComplete(closeVisitAfter: boolean, successMsg: string): void {
    const examsList = this.labExams.value;
    if (examsList.length > 0 && this.patientId) {
      const request = {
        patientId: this.patientId,
        visitId: this.visitId || undefined,
        examType: ExamType.LABORATOIRE,
        exams: examsList,
        reason: this.form.get('labReason')?.value || undefined,
        priority: this.form.get('labPriority')?.value || 'NORMALE',
      };

      this.labOrderApi.create(request).subscribe({
        next: () => this.handleAfterSaveSuccess(closeVisitAfter, successMsg),
        error: err => {
          this.isSaving.set(false);
          this.errorMessage.set(
            err.error?.detail || this.i18n.t('consultation.errors.saveLabOrder'),
          );
        },
      });
    } else {
      this.handleAfterSaveSuccess(closeVisitAfter, successMsg);
    }
  }

  private handleAfterSaveSuccess(closeVisitAfter: boolean, successMsg: string): void {
    if (closeVisitAfter) {
      this.isClosing.set(true);
      this.visitApi.closeVisit(this.visitId).subscribe({
        next: () => {
          this.isClosing.set(false);
          this.isSaving.set(false);
          void this.router.navigate(['/dashboard']);
        },
        error: err => {
          this.isClosing.set(false);
          this.isSaving.set(false);
          this.errorMessage.set(
            err.error?.detail || this.i18n.t('consultation.errors.closeVisit'),
          );
        },
      });
    } else {
      this.isSaving.set(false);
      this.successMessage.set(successMsg);
      const consultationObj = this.consultation();
      if (consultationObj) this.loadPrescription(consultationObj.id);
    }
  }

  goBack(): void {
    void this.router.navigate(['/dashboard']);
  }
}
