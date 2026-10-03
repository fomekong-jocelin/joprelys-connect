import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { ExamType } from '../clinic/lab/lab.models';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from '../patient/patient-api.service';
import { Patient, PatientAllergy, PatientMedicalHistory } from '../patient/patient.models';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { VisitApiService } from '../visit/visit-api.service';
import { Vitals } from '../visit/visit.models';
import {
  AiConsultationDraft,
  AiPrescriptionLine,
  AiVitalsDraft,
} from './ai-consultation-api.service';
import { ClinicalNoteEditorComponent } from './clinical-note-editor.component';
import { ConsultationApiService } from './consultation-api.service';
import { ConsultationFeedbackStore } from './consultation-feedback.store';
import { ConsultationPrescriptionFacade } from './consultation-prescription.facade';
import { ConsultationUiLabelsService } from './consultation-ui-labels.service';
import { Consultation } from './consultation.models';
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
  providers: [
    ConsultationFeedbackStore,
    ConsultationPrescriptionFacade,
    ConsultationUiLabelsService,
  ],
  templateUrl: './consultation.component.html',
})
export class ConsultationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly patientApi = inject(PatientApiService);
  private readonly labOrderApi = inject(LabOrderApiService);
  private readonly feedback = inject(ConsultationFeedbackStore);
  readonly prescriptions = inject(ConsultationPrescriptionFacade);
  readonly uiLabels = inject(ConsultationUiLabelsService);
  readonly i18n = inject(I18nService);

  readonly isLoading = this.feedback.isLoading;
  readonly isSaving = this.feedback.isSaving;
  readonly isClosing = this.feedback.isClosing;
  readonly successMessage = this.feedback.successMessage;
  readonly errorMessage = this.feedback.errorMessage;
  readonly vitals = signal<Vitals | null>(null);
  readonly pendingVitalsProposal = signal<Vitals | null>(null);
  readonly consultation = signal<Consultation | null>(null);
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
    diagnosis: ['', Validators.required],
    conclusion: [''],
    advice: [''],
    followUp: [''],
    prescription: this.prescriptions.items,
    exams: this.fb.array([]),
    labPriority: ['NORMALE'],
    labReason: [''],
  });

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
      vitals: this.vitals() ?? {},
    };
  }

  commonExamLabel(exam: CommonExam): string {
    return this.i18n.t(exam.labelKey);
  }

  applyAiDraft(draft: AiConsultationDraft): void {
    const acceptedDraft: Record<string, string> = {};
    const textFields: Array<keyof AiConsultationDraft> = [
      'symptoms',
      'clinicalExam',
      'diagnosis',
      'conclusion',
      'advice',
      'followUp',
    ];
    textFields.forEach((field) => {
      const value = draft[field];
      if (typeof value === 'string') acceptedDraft[field] = value.trim();
    });

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
    const fieldCount = Object.keys(acceptedDraft).length;
    this.successMessage.set(
      this.i18n.t('consultation.ai.applySuccessReview') +
        (fieldCount > 0
          ? ' (' + fieldCount + ' ' + this.i18n.t('consultation.ai.fieldsUpdated', 'champs') + ')'
          : ''),
    );
    this.errorMessage.set('');
  }

  confirmVitalsProposal(): void {
    const vitals = this.pendingVitalsProposal();
    if (!vitals) return;
    this.visitApi.saveVitals(this.visitId, vitals).subscribe({
      next: (saved) => {
        this.vitals.set(saved);
        this.pendingVitalsProposal.set(null);
        this.successMessage.set(
          this.i18n.t('consultation.ai.vitalsApplied', 'Constantes enregistrées.'),
        );
      },
      error: (err) => {
        this.errorMessage.set(
          err.error?.detail || err.error?.title || this.i18n.t('consultation.ai.vitalsSaveFailed'),
        );
      },
    });
  }

  dismissVitalsProposal(): void {
    this.pendingVitalsProposal.set(null);
  }

  vitalsProposalEntries(): Array<[string, number]> {
    const proposal = this.pendingVitalsProposal();
    if (!proposal) return [];
    return Object.entries(proposal).filter(
      (entry): entry is [string, number] => typeof entry[1] === 'number',
    );
  }

  private applyAiPrescription(raw: string): void {
    const parsed = JSON.parse(raw) as AiPrescriptionLine[];
    if (!Array.isArray(parsed)) throw new Error('Invalid prescription');
    const presc = this.prescriptions.current();
    if (presc && presc.status !== 'DRAFT') throw new Error('Prescription finalized');

    this.prescriptions.items.clear();
    parsed.forEach((item) => {
      if (!item || typeof item.drugName !== 'string' || !item.drugName.trim()) return;
      this.prescriptions.items.push(
        this.fb.group({
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
        }),
      );
    });
  }

  private applyAiLabOrders(raw: string): void {
    const parsed = JSON.parse(raw) as unknown[];
    if (!Array.isArray(parsed)) throw new Error('Invalid lab orders');
    this.labExams.clear();
    parsed.forEach((item) => {
      if (typeof item === 'string' && item.trim()) this.addLabExam(item.trim());
    });
  }

  private applyAiVitals(raw: string): void {
    const parsed = JSON.parse(raw) as AiVitalsDraft;
    if (!parsed || typeof parsed !== 'object') throw new Error('Invalid vitals');
    const vitals: Vitals = {};
    const numericFields: Array<keyof AiVitalsDraft> = [
      'temperature',
      'weight',
      'height',
      'pulse',
      'systolic',
      'diastolic',
      'spo2',
      'glycemia',
      'respiratoryRate',
      'painScale',
    ];
    numericFields.forEach((field) => {
      const value = parsed[field];
      if (typeof value === 'number' && Number.isFinite(value)) vitals[field] = value;
    });
    if (Object.keys(vitals).length === 0) return;
    this.pendingVitalsProposal.set(vitals);
  }

  private loadData(): void {
    if (!this.visitId) return;
    this.isLoading.set(true);

    this.visitApi.getVitals(this.visitId).subscribe({
      next: (data) => {
        this.vitals.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });

    this.visitApi.getById(this.visitId).subscribe({
      next: (visit) => {
        if (visit?.visitNumber) this.visitNumber.set(visit.visitNumber);
        this.visitReason.set(visit?.reason || '');

        if (visit?.patientId) {
          this.patientId = visit.patientId;
          this.loadPatientContext(this.patientId);
          this.labOrderApi.getPatientLabOrders(this.patientId).subscribe({
            next: (orders) => {
              const currentVisitOrder = orders.find((order) => order.visitId === this.visitId);
              if (!currentVisitOrder) return;
              this.labExams.clear();
              currentVisitOrder.exams.forEach((exam) => this.addLabExam(exam));
              this.form.patchValue({
                labPriority: currentVisitOrder.priority,
                labReason: currentVisitOrder.reason || '',
              });
            },
          });
        }
      },
      error: () => this.errorMessage.set(this.i18n.t('consultation.errors.loadVisit')),
    });

    this.consultationApi.getConsultation(this.visitId).subscribe({
      next: (existing) => {
        this.consultation.set(existing);
        this.form.patchValue({
          symptoms: existing.symptoms,
          clinicalExam: existing.clinicalExam ?? '',
          diagnosis: existing.diagnosis ?? '',
          conclusion: existing.conclusion ?? '',
          advice: existing.advice ?? '',
          followUp: existing.followUp ?? '',
        });
        this.prescriptions.load(existing.id);
      },
      error: () => undefined,
    });
  }

  private loadPatientContext(patientId: string): void {
    this.patientApi.getById(patientId).subscribe({
      next: (patient) => this.patient.set(patient),
      error: () => this.patient.set(null),
    });

    this.patientApi.getAllergies(patientId).subscribe({
      next: (allergies) => this.patientAllergies.set(allergies),
      error: () => this.patientAllergies.set([]),
    });

    this.patientApi.getMedicalHistory(patientId).subscribe({
      next: (history) => this.patientMedicalHistory.set(history),
      error: () => this.patientMedicalHistory.set([]),
    });
  }

  addLabExam(examName: string = ''): void {
    if (!examName.trim()) return;
    const exists = this.labExams.controls.some(
      (ctrl) => ctrl.value.toLowerCase() === examName.trim().toLowerCase(),
    );
    if (!exists) this.labExams.push(this.fb.control(examName.trim(), Validators.required));
  }

  removeLabExam(index: number): void {
    this.labExams.removeAt(index);
  }

  onSave(closeVisitAfter: boolean = false): void {
    if (this.form.invalid || this.isSaving() || this.isClosing()) return;

    this.shouldCloseAfterSave = closeVisitAfter;
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const { symptoms, clinicalExam, diagnosis, conclusion, advice, followUp } =
      this.form.getRawValue();
    const normalizedDiagnosis = typeof diagnosis === 'string' ? diagnosis.trim() : diagnosis;

    this.consultationApi
      .saveConsultation(this.visitId, {
        symptoms,
        clinicalExam: clinicalExam || undefined,
        diagnosis: normalizedDiagnosis,
        conclusion: conclusion || undefined,
        advice: advice || undefined,
        followUp: followUp || undefined,
        expectedUpdatedAt: this.consultation()?.updatedAt,
      })
      .subscribe({
        next: (savedConsultation) => {
          this.consultation.set(savedConsultation);

          const prescriptionLines = this.prescriptions.items.getRawValue();
          const examsLines = this.labExams.value;

          let successMsg = this.i18n.t('consultation.success.saved');
          if (prescriptionLines.length > 0 && examsLines.length > 0) {
            successMsg = this.i18n.t('consultation.success.savedAll');
          } else if (prescriptionLines.length > 0) {
            successMsg = this.i18n.t('consultation.success.savedPrescription');
          } else if (examsLines.length > 0) {
            successMsg = this.i18n.t('consultation.success.savedLab');
          }

          const presc = this.prescriptions.current();
          if (prescriptionLines.length > 0 && (!presc || presc.status === 'DRAFT')) {
            this.consultationApi
              .savePrescription(savedConsultation.id, {
                items: prescriptionLines,
              })
              .subscribe({
                next: (savedPresc) => {
                  this.prescriptions.setCurrent(savedPresc);
                  this.saveLabOrderAndComplete(closeVisitAfter, successMsg);
                },
                error: (err) => {
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
        error: (err) => {
          this.isSaving.set(false);
          this.errorMessage.set(
            err.error?.detail ||
              err.error?.title ||
              this.i18n.t('consultation.errors.saveConsultation'),
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
        error: (err) => {
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
        error: (err) => {
          this.isClosing.set(false);
          this.isSaving.set(false);
          this.errorMessage.set(err.error?.detail || this.i18n.t('consultation.errors.closeVisit'));
        },
      });
    } else {
      this.isSaving.set(false);
      this.successMessage.set(successMsg);
      const consultationObj = this.consultation();
      if (consultationObj) this.prescriptions.load(consultationObj.id);
    }
  }

  goBack(): void {
    void this.router.navigate(['/dashboard']);
  }
}
