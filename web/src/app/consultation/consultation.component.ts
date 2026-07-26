import {
  AfterViewInit,
  ApplicationRef,
  Component,
  ComponentRef,
  EnvironmentInjector,
  OnDestroy,
  OnInit,
  createComponent,
  inject,
  signal,
} from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ConsultationApiService } from './consultation-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Consultation, Prescription } from './consultation.models';
import { Visit, Vitals } from '../visit/visit.models';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { ExamType } from '../clinic/lab/lab.models';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationDraft,
  AiPrescriptionLine,
  AiVitalsDraft,
} from './ai-consultation-api.service';
import { VoiceAssistantPanelComponent } from './voice-assistant-panel.component';
import { ClinicalNoteEditorComponent } from './clinical-note-editor.component';
import { PatientApiService } from '../patient/patient-api.service';
import {
  Patient,
  PatientAllergy,
  PatientMedicalHistory,
} from '../patient/patient.models';

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AppShellComponent],
  templateUrl: './consultation.component.html'
})
export class ConsultationComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly applicationRef = inject(ApplicationRef);
  private readonly environmentInjector = inject(EnvironmentInjector);
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly patientApi = inject(PatientApiService);
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
  private voiceAssistantRef: ComponentRef<VoiceAssistantPanelComponent> | null = null;
  private clinicalNoteRef: ComponentRef<ClinicalNoteEditorComponent> | null = null;
  private legacyClinicalDetailsElement: HTMLElement | null = null;
  private voiceDraftSubscription: Subscription | null = null;
  private voiceMountTimer: ReturnType<typeof setTimeout> | null = null;
  private voiceMountAttempts = 0;
  private clinicalNoteMountTimer: ReturnType<typeof setTimeout> | null = null;
  private clinicalNoteMountAttempts = 0;
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
    // Conservé pour compatibilité API / données historiques. Le nouvel écran
    // expose un seul "Diagnostic retenu" et synchronise cette valeur à la sauvegarde.
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
    this.voiceDraftSubscription = this.form.valueChanges.subscribe(() => this.syncVoiceDraft());
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.scheduleVoiceAssistantMount();
    this.scheduleClinicalNoteMount();
  }

  ngOnDestroy(): void {
    this.voiceDraftSubscription?.unsubscribe();
    if (this.voiceMountTimer) {
      clearTimeout(this.voiceMountTimer);
      this.voiceMountTimer = null;
    }
    if (this.clinicalNoteMountTimer) {
      clearTimeout(this.clinicalNoteMountTimer);
      this.clinicalNoteMountTimer = null;
    }
    if (this.voiceAssistantRef) {
      this.applicationRef.detachView(this.voiceAssistantRef.hostView);
      this.voiceAssistantRef.destroy();
      this.voiceAssistantRef = null;
    }
    if (this.clinicalNoteRef) {
      this.applicationRef.detachView(this.clinicalNoteRef.hostView);
      this.clinicalNoteRef.destroy();
      this.clinicalNoteRef = null;
    }
    if (this.legacyClinicalDetailsElement) {
      this.legacyClinicalDetailsElement.hidden = false;
      this.legacyClinicalDetailsElement = null;
    }
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

    // Le modèle historique distingue diagnosis/finalDiagnosis. L'UX clinique
    // n'affiche plus ce doublon : finalDiagnosis prévaut lorsqu'il est fourni,
    // sinon diagnosis alimente l'unique "Diagnostic retenu".
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
      this.errorMessage.set(
        'Une proposition structurée de l’IA est illisible. Elle n’a pas été appliquée.',
      );
      return;
    }

    this.form.markAsDirty();
    this.syncVoiceDraft();
    this.successMessage.set(
      'Les propositions IA validées ont été appliquées. Relisez la note clinique, l’ordonnance, les examens et les constantes avant la sauvegarde finale.',
    );
    this.errorMessage.set('');
  }

  private applyAiPrescription(raw: string): void {
    const parsed = JSON.parse(raw) as AiPrescriptionLine[];
    if (!Array.isArray(parsed)) throw new Error('Invalid prescription');
    const presc = this.prescription();
    if (presc && presc.status !== 'DRAFT') {
      throw new Error('Prescription finalized');
    }
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
      if (typeof value === 'number' && Number.isFinite(value)) {
        vitals[field] = value;
      }
    });
    if (Object.keys(vitals).length === 0) return;
    this.visitApi.saveVitals(this.visitId, vitals).subscribe({
      next: saved => {
        this.vitals.set(saved);
        this.syncVoiceDraft();
      },
      error: err => {
        this.errorMessage.set(
          err.error?.detail || err.error?.title || 'Les constantes proposées n’ont pas pu être enregistrées.',
        );
      },
    });
  }

  private scheduleVoiceAssistantMount(): void {
    if (this.voiceAssistantRef || this.voiceMountAttempts >= 40) return;
    this.voiceMountAttempts += 1;
    this.voiceMountTimer = setTimeout(() => {
      this.voiceMountTimer = null;
      if (!this.mountVoiceAssistant()) {
        this.scheduleVoiceAssistantMount();
      }
    }, this.voiceMountAttempts === 1 ? 0 : 250);
  }

  private scheduleClinicalNoteMount(): void {
    if (this.clinicalNoteRef || this.clinicalNoteMountAttempts >= 40) return;
    this.clinicalNoteMountAttempts += 1;
    this.clinicalNoteMountTimer = setTimeout(() => {
      this.clinicalNoteMountTimer = null;
      if (!this.mountClinicalNoteEditor()) {
        this.scheduleClinicalNoteMount();
      }
    }, this.clinicalNoteMountAttempts === 1 ? 0 : 250);
  }

  private mountVoiceAssistant(): boolean {
    if (!this.visitId || this.voiceAssistantRef || typeof document === 'undefined') {
      return !!this.voiceAssistantRef;
    }
    const formElement = document.querySelector('app-consultation form');
    const target = formElement?.parentElement;
    if (!formElement || !target) return false;

    const componentRef = createComponent(VoiceAssistantPanelComponent, {
      environmentInjector: this.environmentInjector,
    });
    componentRef.setInput('visitId', this.visitId);
    componentRef.setInput('currentDraft', this.currentVoiceDraft());
    componentRef.instance.applyDraft.subscribe(draft => this.applyAiDraft(draft));
    this.applicationRef.attachView(componentRef.hostView);
    target.insertBefore(componentRef.location.nativeElement, formElement);
    this.voiceAssistantRef = componentRef;
    return true;
  }

  private mountClinicalNoteEditor(): boolean {
    if (this.clinicalNoteRef || typeof document === 'undefined') {
      return !!this.clinicalNoteRef;
    }
    const formElement = document.querySelector('app-consultation form') as HTMLFormElement | null;
    if (!formElement) return false;

    const legacyClinicalDetails = formElement.firstElementChild as HTMLElement | null;
    if (!legacyClinicalDetails) return false;

    const componentRef = createComponent(ClinicalNoteEditorComponent, {
      environmentInjector: this.environmentInjector,
    });
    componentRef.setInput('form', this.form);
    componentRef.setInput('patient', this.patient());
    componentRef.setInput('visitReason', this.visitReason());
    componentRef.setInput('allergies', this.patientAllergies());
    componentRef.setInput('medicalHistory', this.patientMedicalHistory());
    this.applicationRef.attachView(componentRef.hostView);
    formElement.insertBefore(componentRef.location.nativeElement, legacyClinicalDetails);

    legacyClinicalDetails.hidden = true;
    this.legacyClinicalDetailsElement = legacyClinicalDetails;
    this.clinicalNoteRef = componentRef;
    return true;
  }

  private syncVoiceDraft(): void {
    this.voiceAssistantRef?.setInput('currentDraft', this.currentVoiceDraft());
  }

  private syncClinicalNoteContext(): void {
    if (!this.clinicalNoteRef) return;
    this.clinicalNoteRef.setInput('patient', this.patient());
    this.clinicalNoteRef.setInput('visitReason', this.visitReason());
    this.clinicalNoteRef.setInput('allergies', this.patientAllergies());
    this.clinicalNoteRef.setInput('medicalHistory', this.patientMedicalHistory());
  }

  private currentVoiceDraft(): Record<string, unknown> {
    const raw = this.form.getRawValue();
    return {
      ...raw,
      finalDiagnosis: raw.diagnosis || '',
      vitals: this.vitals() ?? {},
    };
  }

  private loadData(): void {
    if (!this.visitId) return;
    this.isLoading.set(true);

    this.http.get<{ visitNumber?: string } & Vitals>(`/api/visits/${this.visitId}/vitals`).subscribe({
      next: (data) => {
        this.vitals.set(data);
        this.isLoading.set(false);
        this.scheduleVoiceAssistantMount();
        this.scheduleClinicalNoteMount();
        this.syncVoiceDraft();
      },
      error: () => {
        this.isLoading.set(false);
        this.scheduleVoiceAssistantMount();
        this.scheduleClinicalNoteMount();
      }
    });

    this.http.get<Visit>(`/api/visits/${this.visitId}`).subscribe({
      next: (visit) => {
        if (visit?.visitNumber) {
          this.visitNumber.set(visit.visitNumber);
        }
        this.visitReason.set(visit?.reason || '');
        this.syncClinicalNoteContext();

        if (visit?.patientId) {
          this.patientId = visit.patientId;
          this.loadPatientContext(this.patientId);
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
        this.syncVoiceDraft();
        this.loadPrescription(existing.id);
      },
      error: () => {}
    });
  }

  private loadPatientContext(patientId: string): void {
    this.patientApi.getById(patientId).subscribe({
      next: patient => {
        this.patient.set(patient);
        this.syncClinicalNoteContext();
      },
      error: () => this.patient.set(null),
    });

    this.patientApi.getAllergies(patientId).subscribe({
      next: allergies => {
        this.patientAllergies.set(allergies);
        this.syncClinicalNoteContext();
      },
      error: () => this.patientAllergies.set([]),
    });

    this.patientApi.getMedicalHistory(patientId).subscribe({
      next: history => {
        this.patientMedicalHistory.set(history);
        this.syncClinicalNoteContext();
      },
      error: () => this.patientMedicalHistory.set([]),
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
        this.syncVoiceDraft();
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
      // Compatibilité historique : une seule valeur clinique est désormais
      // saisie, mais elle reste persistée dans l'ancien champ finalDiagnosis.
      finalDiagnosis: retainedDiagnosis || undefined,
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
