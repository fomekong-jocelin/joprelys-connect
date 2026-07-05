import { Component, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ConsultationApiService } from './consultation-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Consultation } from './consultation.models';
import { Vitals } from '../visit/visit.models';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShellComponent],
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
  readonly visitNumber = signal('');

  private visitId = '';
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

  private loadData(): void {
    if (!this.visitId) return;
    this.isLoading.set(true);

    // Load vitals
    this.http.get<{ visitNumber?: string } & Vitals>(`/api/visits/${this.visitId}/vitals`).subscribe({
      next: (data) => {
        this.vitals.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });

    // Load visit info for visit number and patient details
    this.http.get<any>(`/api/visits/${this.visitId}`).subscribe({
      next: (visit) => {
        if (visit?.visitNumber) {
          this.visitNumber.set(visit.visitNumber);
        }
        if (visit?.patientId) {
          this.patientId = visit.patientId;
          // Load existing lab orders for this patient to find one linked to this visit
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

    // Load existing consultation (ignore 404 silently)
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
      },
      error: () => {}
    });
  }

  addPrescriptionLine(): void {
    const line = this.fb.group({
      drugName: ['', Validators.required],
      dosage: ['', Validators.required],
      posology: [''],
      duration: [''],
      quantity: [''],
    });
    this.prescriptionItems.push(line);
  }

  removePrescriptionLine(index: number): void {
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

        const prescriptionLines = this.prescriptionItems.value;
        const examsLines = this.labExams.value;

        let successMsg = this.i18n.t('consultation.success.saved');
        if (prescriptionLines.length > 0 && examsLines.length > 0) {
          successMsg = this.i18n.t('consultation.success.savedAll');
        } else if (prescriptionLines.length > 0) {
          successMsg = this.i18n.t('consultation.success.savedPrescription');
        } else if (examsLines.length > 0) {
          successMsg = this.i18n.t('consultation.success.savedLab');
        }

        if (prescriptionLines.length > 0) {
          this.consultationApi.savePrescription(savedConsultation.id, {
            items: prescriptionLines,
          }).subscribe({
            next: () => {
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
        examType: 'LABORATOIRE',
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
          this.isSaving.set(false);
          this.isClosing.set(false);
          this.successMessage.set(this.i18n.t('consultation.success.closed'));
          setTimeout(() => this.goBack(), 1500);
        },
        error: (err) => {
          this.isSaving.set(false);
          this.isClosing.set(false);
          this.errorMessage.set(this.i18n.t('consultation.success.saved') + ' - Error: ' + (err.error?.detail || err.message || 'Close visit error'));
        }
      });
    } else {
      this.isSaving.set(false);
      this.successMessage.set(successMsg);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
