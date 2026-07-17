import { Component, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ConsultationApiService } from './consultation-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Consultation, Prescription } from './consultation.models';
import { Vitals } from '../visit/visit.models';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { ExamType } from '../clinic/lab/lab.models';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationDraft,
  VoiceAssistantPanelComponent,
} from './voice-assistant-panel.component';

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShellComponent, VoiceAssistantPanelComponent],
  templateUrl: './consultation.component.html'
})
export class ConsultationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly visitApi = inject(VisitApiService);
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

  visitId = '';
  private patientId = '';
  shouldCloseAfterSave = false;

  readonly commonExams = [
    { name: 'NFS / Hémogramme', code: 'NFS' },
    { name: 'Glycémie à jeun', code: 'Glycémie à jeun' },
    { name: 'Créatininémie (Bilan Rénal)', code: 'Créatinine' },
    { name: 'Urée', code: 'Urée' },
    { name: 'Bilan Lipidique (EAL)', code: 'Bilan Lipidique' },
    { name: 'Transaminases (SGOT/SGPT)', code: 'Transaminases' },
    { name: 'CRP (Protéine C-Réactive)', code: 'CRP' },
    { name: 'ECBU (Urine)', code: 'ECBU' },
    { name: 'Hémoglobine Glyquée (HbA1c)', code: 'HbA1c' }
  ];

  readonly form: FormGroup = this.fb.group({
    symptoms: ['', Validators.required],
    clinicalExam: [''],
    suspectedDiagnosis: [''],
    diagnosis: ['', Validators.required],
    finalDiagnosis: [''],
    conclusion: [''],
    advice: [''],
    followUp: [''],
    prescription: this.fb.array([]),
    exams: this.fb.array([]),
    labPriority: ['NORMALE'],
    labReason: ['']
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

  applyAiDraft(draft: AiConsultationDraft): void {
    const allowedDraft: AiConsultationDraft = {};
    const fields: Array<keyof AiConsultationDraft> = [
      'symptoms',
      'clinicalExam',
      'suspectedDiagnosis',
      'diagnosis',
      'finalDiagnosis',
      'conclusion',
      'advice',
      'followUp',
    ];
    fields.forEach(field => {
      const value = draft[field];
      if (typeof value === 'string' && value.trim()) {
        allowedDraft[field] = value.trim();
      }
    });
    this.form.patchValue(allowedDraft);
    this.form.markAsDirty();
    this.successMessage.set(
      this.i18n.t(
        'consultation.ai.applied',
        'Le brouillon IA a été copié dans le formulaire. Relisez-le avant de sauvegarder.',
      ),
    );
    this.errorMessage.set('');
  }

  private loadData(): void {
    if (!this.visitId) return;
    this.isLoading.set(true);

    this.http.get<{ visitNumber?: string } & Vitals>(`/api/visits/${this.visitId}/vitals`).subscribe({
      next: (data) => {
        this.vitals.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });

    this.http.get<any>(`/api/visits/${this.visitId}`).subscribe({
      next: (visit) => {
        if (visit?.visitNumber) {
          this.visitNumber.set(visit.visitNumber);
        }
        if (visit?.patientId) {
          this.patientId = visit.patientId;
          this.labOrderApi.getPatientLabOrders(this.patientId).subscribe({
            next: (orders) => {
              const currentVisitOrder = orders.find(o => o.visitId === this.visitId);
              if (currentVisitOrder) {
                this.labExams.clear();
                currentVisitOrder.exams.forEach(ex => this.addLabExam(ex));
                this.form.patchValue({
                  labPriority: currentVisitOrder.priority,
                  labReason: currentVisitOrder.reason || ''
                });
              }
            }
          });
        }
      },
      error: () => {}
    });

    this.consultationApi.getConsultation(this.visitId).subscribe({
      next: (existing) => {
        this.consultation.set(existing);
        this.form.patchValue({
          symptoms: existing.symptoms,
          clinicalExam: existing.clinicalExam ?? '',
          suspectedDiagnosis: existing.suspectedDiagnosis ?? '',
          diagnosis: existing.diagnosis,
          finalDiagnosis: existing.finalDiagnosis ?? '',
          conclusion: existing.conclusion ?? '',
          advice: existing.advice ?? '',
          followUp: existing.followUp ?? '',
        });
        this.loadPrescription(existing.id);
      },
      error: () => {}
    });
  }

  loadPrescription(consultationId: string): void {
    this.consultationApi.getPrescription(consultationId).subscribe({
      next: (presc) => {
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
      error: () => {
        this.prescription.set(null);
      }
    });
  }

  addPrescriptionLine(): void {
    const presc = this.prescription();
    if (presc && presc.status !== 'DRAFT') return;

    const line = this.fb.group({
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
    });
    this.prescriptionItems.push(line);
  }

  removePrescriptionLine(index: number): void {
    const presc = this.prescription();
    if (presc && presc.status !== 'DRAFT') return;
    this.prescriptionItems.removeAt(index);
  }

  addLabExam(examName: string = ''): void {
    if (!examName.trim()) return;
    const exists = this.labExams.controls.some(
      (ctrl) => ctrl.value.toLowerCase() === examName.trim().toLowerCase()
    );
    if (!exists) {
      this.labExams.push(this.fb.control(examName.trim(), Validators.required));
    }
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
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.detail || 'Error finalizing prescription.');
      }
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
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.detail || 'Error cancelling prescription.');
      }
    });
  }

  downloadPrescriptionPdf(): void {
    const presc = this.prescription();
    if (!presc || !presc.documentId) return;

    this.isLoading.set(true);
    this.consultationApi.downloadDocumentById(presc.documentId).subscribe({
      next: (blob) => {
        this.isLoading.set(false);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ordonnance-${presc.prescriptionNumber}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Error downloading PDF.');
      }
    });
  }

  onSave(closeVisitAfter: boolean = false): void {
    if (this.form.invalid || this.isSaving() || this.isClosing()) return;

    this.shouldCloseAfterSave = closeVisitAfter;
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const { symptoms, clinicalExam, suspectedDiagnosis, diagnosis, finalDiagnosis, conclusion, advice, followUp } = this.form.value;

    this.consultationApi.saveConsultation(this.visitId, {
      symptoms,
      clinicalExam: clinicalExam || undefined,
      suspectedDiagnosis: suspectedDiagnosis || undefined,
      diagnosis,
      finalDiagnosis: finalDiagnosis || undefined,
      conclusion: conclusion || undefined,
      advice: advice || undefined,
      followUp: followUp || undefined,
    }).subscribe({
      next: (savedConsultation) => {
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
            next: (savedPresc) => {
              this.prescription.set(savedPresc);
              this.saveLabOrderAndComplete(closeVisitAfter, successMsg);
            },
            error: (err) => {
              this.isSaving.set(false);
              this.errorMessage.set(this.i18n.t('consultation.success.saved') + ' - Error: ' + (err.error?.detail || err.message || 'Prescription error'));
            }
          });
        } else {
          this.saveLabOrderAndComplete(closeVisitAfter, successMsg);
        }
      },
      error: (err) => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.detail || err.error?.title || 'An error occurred during save.');
      }
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
        priority: this.form.get('labPriority')?.value || 'NORMALE'
      };

      this.labOrderApi.create(request).subscribe({
        next: () => {
          this.handleAfterSaveSuccess(closeVisitAfter, successMsg);
        },
        error: (err) => {
          this.isSaving.set(false);
          this.errorMessage.set(this.i18n.t('consultation.success.saved') + ' - Error: ' + (err.error?.detail || err.message || 'Lab order error'));
        }
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
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.isClosing.set(false);
          this.isSaving.set(false);
          this.errorMessage.set('Saved but failed to close visit: ' + (err.error?.detail || err.message));
        }
      });
    } else {
      this.isSaving.set(false);
      this.successMessage.set(successMsg);
      const consultationObj = this.consultation();
      if (consultationObj) {
        this.loadPrescription(consultationObj.id);
      }
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
