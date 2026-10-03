import { CommonModule } from '@angular/common';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize, map, of, switchMap } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyApiService } from '../emergency/emergency-api.service';
import { CreateEmergencyRequest, EmergencyTriageRequest } from '../emergency/emergency.models';
import { PatientApiService } from '../patient/patient-api.service';
import { Patient } from '../patient/patient.models';
import { PatientSearchPickerComponent } from '../patient/patient-search-picker.component';
import { CreateProvisionalPatientRequest } from '../patient/provisional-patient.models';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { VisitApiService } from '../visit/visit-api.service';
import { VisitAdmissionOptionsService, OTHER_SERVICE } from './visit-admission-options.service';
import { VisitDetailsFieldsComponent } from './visit-details-fields.component';
import { orientationLabel } from '../visit/visit-orientation.util';
import { hasRequiredVisitDetails, toCreateVisitRequest, visitDetailsControls } from './visit-details-form';

export type AdmissionCarePath = 'NORMAL' | 'EMERGENCY';
export type AdmissionPatientMode = 'EXISTING' | 'NEW' | 'PROVISIONAL';
export type AdmissionStep = 1 | 2 | 3;

export interface AdmissionCompleted {
  carePath: AdmissionCarePath;
  patientId: string;
  patientDisplayName: string;
  visitId?: string;
  emergencyId?: string;
}

@Component({
  selector: 'app-unified-admission',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AlertComponent, ButtonComponent, PatientSearchPickerComponent, VisitDetailsFieldsComponent],
  providers: [VisitAdmissionOptionsService],
  templateUrl: './unified-admission.component.html',
})
export class UnifiedAdmissionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly patientApi = inject(PatientApiService);
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly i18n = inject(I18nService);
  private readonly admissionOptions = inject(VisitAdmissionOptionsService);
  private readonly provisionalAdmissionRequestId = globalThis.crypto.randomUUID();

  readonly initialCarePath = input<AdmissionCarePath>('NORMAL');
  readonly cancelled = output<void>();
  readonly completed = output<AdmissionCompleted>();

  readonly selectedPatient = signal<Patient | null>(null);
  readonly isSubmitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly currentStep = signal<AdmissionStep>(1);
  readonly steps: readonly AdmissionStep[] = [1, 2, 3];
  readonly isDraftRestored = signal(false);
  private readonly ADMISSION_DRAFT_KEY = 'joprelys_admission_draft';

  readonly form: FormGroup = this.fb.group({
    carePath: ['NORMAL'],
    patientMode: ['EXISTING'],
    patientId: [''],
    fullName: [''],
    gender: [''],
    birthDate: [''],
    phone: [''],
    city: [''],
    district: [''],
    address: [''],
    email: [''],
    apparentGender: ['UNKNOWN'],
    estimatedAgeRange: [''],
    physicalDescription: [''],
    foundLocation: [''],
    ...visitDetailsControls(),
    arrivalMode: ['AMBULANCE'],
    thirdPartyName: [''],
    thirdPartyPhone: [''],
    thirdPartyRelationship: [''],
    thirdPartyIdDocument: [''],
    thirdPartyCircumstances: [''],
    thirdPartyConsentToContact: [false],
    triageLevel: ['RED'],
    hemodynamicStatus: ['SHOCK'],
    chiefComplaint: [''],
    initialBpSystolic: [null, [Validators.min(30), Validators.max(300)]],
    initialBpDiastolic: [null, [Validators.min(20), Validators.max(200)]],
    initialHr: [null, [Validators.min(20), Validators.max(250)]],
    initialTemp: [null, [Validators.min(30), Validators.max(45)]],
  });

  ngOnInit(): void {
    this.form.patchValue({ carePath: this.initialCarePath() });
    this.restoreDraftIfAvailable();
    this.form.valueChanges.subscribe(() => this.saveDraft());
  }

  private saveDraft(): void {
    try {
      const draft = {
        currentStep: this.currentStep(),
        carePath: this.carePath,
        patientMode: this.patientMode,
        selectedPatient: this.selectedPatient(),
        formValue: this.form.getRawValue(),
      };
      sessionStorage.setItem(this.ADMISSION_DRAFT_KEY, JSON.stringify(draft));
    } catch {}
  }

  private restoreDraftIfAvailable(): void {
    try {
      const raw = sessionStorage.getItem(this.ADMISSION_DRAFT_KEY);
      if (!raw) return;
      const draft = JSON.parse(raw);
      if (draft && draft.formValue) {
        if (draft.carePath) this.form.patchValue({ carePath: draft.carePath });
        if (draft.patientMode) this.form.patchValue({ patientMode: draft.patientMode });
        this.form.patchValue(draft.formValue);
        if (draft.selectedPatient) this.selectedPatient.set(draft.selectedPatient);
        if (draft.currentStep && [1, 2, 3].includes(draft.currentStep)) {
          this.currentStep.set(draft.currentStep as AdmissionStep);
        }
        this.isDraftRestored.set(true);
      }
    } catch {}
  }

  clearDraft(): void {
    try {
      sessionStorage.removeItem(this.ADMISSION_DRAFT_KEY);
    } catch {}
    this.isDraftRestored.set(false);
  }

  get carePath(): AdmissionCarePath {
    return this.form.get('carePath')?.value as AdmissionCarePath;
  }

  get patientMode(): AdmissionPatientMode {
    return this.form.get('patientMode')?.value as AdmissionPatientMode;
  }

  get isAccompanied(): boolean {
    return this.carePath === 'EMERGENCY' && this.form.get('arrivalMode')?.value === 'ACCOMPANIED';
  }

  setCarePath(path: AdmissionCarePath): void {
    this.form.patchValue({ carePath: path });
    if (path === 'NORMAL' && this.patientMode === 'PROVISIONAL') {
      this.setPatientMode('NEW');
    }
    this.resetNavigation();
  }

  setPatientMode(mode: AdmissionPatientMode): void {
    this.form.patchValue({ patientMode: mode, patientId: '' });
    this.selectedPatient.set(null);
    this.error.set(null);
  }

  onPatientSelected(patient: Patient | null): void {
    this.selectedPatient.set(patient);
    this.form.patchValue({ patientId: patient?.id ?? '' });
    this.error.set(null);
  }

  nextStep(): void {
    const validationError = this.validateStep(this.currentStep());
    if (validationError) {
      this.showValidationError(validationError);
      return;
    }

    this.error.set(null);
    if (this.currentStep() < 3) {
      this.currentStep.update((step) => (step + 1) as AdmissionStep);
    }
  }

  previousStep(): void {
    this.error.set(null);
    if (this.currentStep() > 1) {
      this.currentStep.update((step) => (step - 1) as AdmissionStep);
    }
  }

  stepLabel(step: AdmissionStep): string {
    if (step === 1) return this.text('stepPatient');
    if (step === 2) return this.text(this.carePath === 'EMERGENCY' ? 'stepArrival' : 'stepVisit');
    return this.text(this.carePath === 'EMERGENCY' ? 'stepTriage' : 'stepConfirm');
  }

  progressPercent(): number {
    return (this.currentStep() / this.steps.length) * 100;
  }

  patientSummary(): string {
    const value = this.form.getRawValue();
    if (this.patientMode === 'EXISTING') {
      const patient = this.selectedPatient();
      return patient ? this.displayPatient(patient) : this.text('existing');
    }
    if (this.patientMode === 'NEW') {
      return value.fullName?.trim() || this.text('new');
    }
    return this.text('provisional');
  }

  /** Orientation, service et praticien choisis, pour vérification avant création de la visite. */
  visitChoicesSummary(): string {
    const value = this.form.getRawValue();
    const orientation = value.orientation === 'OTHER' && value.customOrientation?.trim()
      ? value.customOrientation.trim()
      : orientationLabel(value.orientation, this.i18n);
    const service = value.service === OTHER_SERVICE ? value.customService?.trim() : value.service;
    const practitioner = this.admissionOptions.practitionerName(value.mainPractitionerId) ?? this.text('mainPractitionerPlaceholder');
    return [orientation, service || '—', practitioner].join(' · ');
  }

  arrivalSummary(): string {
    const arrivalMode = this.form.get('arrivalMode')?.value;
    const translationKeys: Record<string, string> = {
      AMBULANCE: 'ambulance',
      FIRE_DEPT: 'fireDept',
      WALK_IN: 'walkIn',
      ACCOMPANIED: 'accompanied',
    };
    const key = translationKeys[arrivalMode];
    return key ? this.text(key) : '—';
  }

  submit(): void {
    for (const step of this.steps) {
      const validationError = this.validateStep(step);
      if (validationError) {
        this.currentStep.set(step);
        this.showValidationError(validationError);
        return;
      }
    }

    if (this.form.invalid || this.isSubmitting()) {
      this.showValidationError(this.text('invalidValues'));
      return;
    }

    const value = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.error.set(null);

    const submission$ = this.carePath === 'EMERGENCY' && this.patientMode === 'PROVISIONAL'
      ? this.createProvisionalEmergency(value)
      : this.resolvePatient().pipe(
          switchMap((patient) => this.createCareRecord(patient, value)),
        );

    submission$.pipe(
      finalize(() => this.isSubmitting.set(false)),
    ).subscribe({
      next: (result) => {
        this.clearDraft();
        this.completed.emit(result);
      },
      error: (err) => this.error.set(this.localizedApiError(err, 'saveError')),
    });
  }

  displayPatient(patient: Patient): string {
    return patient.displayName || patient.fullName || patient.temporaryPatientNumber || patient.globalPatientNumber;
  }

  text(key: string): string {
    return this.i18n.t(`admission.${key}`);
  }

  private resetNavigation(): void {
    this.currentStep.set(1);
    this.error.set(null);
  }

  private showValidationError(message: string): void {
    this.error.set(message);
    this.form.markAllAsTouched();
  }

  private createProvisionalEmergency(
    value: ReturnType<FormGroup['getRawValue']>,
  ): Observable<AdmissionCompleted> {
    return this.emergencyApi.createProvisionalAdmission({
      requestId: this.provisionalAdmissionRequestId,
      patient: this.provisionalPatientPayload(value),
      emergency: this.emergencyTriagePayload(value),
    }).pipe(map((emergency) => ({
      carePath: 'EMERGENCY',
      patientId: emergency.patientId,
      patientDisplayName: emergency.patientName,
      emergencyId: emergency.id,
    })));
  }

  private createCareRecord(
    patient: { id: string; displayName: string },
    value: ReturnType<FormGroup['getRawValue']>,
  ): Observable<AdmissionCompleted> {
    if (this.carePath === 'EMERGENCY') {
      const request: CreateEmergencyRequest = {
        patientId: patient.id,
        ...this.emergencyTriagePayload(value),
      };
      return this.emergencyApi.create(request).pipe(map((emergency) => ({
        carePath: this.carePath,
        patientId: patient.id,
        patientDisplayName: patient.displayName,
        emergencyId: emergency.id,
      })));
    }

    return this.visitApi.create(toCreateVisitRequest(patient.id, value)).pipe(map((visit) => ({
      carePath: this.carePath,
      patientId: patient.id,
      patientDisplayName: patient.displayName,
      visitId: visit.id,
    })));
  }

  private resolvePatient(): Observable<{ id: string; displayName: string }> {
    const value = this.form.getRawValue();

    if (this.patientMode === 'EXISTING') {
      const patient = this.selectedPatient();
      return of({
        id: patient?.id ?? value.patientId,
        displayName: patient ? this.displayPatient(patient) : this.text('existing'),
      });
    }

    return this.patientApi.create({
      fullName: value.fullName.trim(),
      gender: value.gender,
      birthDate: value.birthDate,
      phone: value.phone.trim(),
      city: value.city.trim(),
      district: this.optional(value.district),
      address: this.optional(value.address),
      email: this.optional(value.email),
    }).pipe(map((patient) => ({
      id: patient.id,
      displayName: this.displayPatient(patient),
    })));
  }

  private provisionalPatientPayload(
    value: ReturnType<FormGroup['getRawValue']>,
  ): CreateProvisionalPatientRequest {
    return {
      apparentGender: value.apparentGender === 'UNKNOWN' ? undefined : value.apparentGender,
      estimatedAgeRange: this.optional(value.estimatedAgeRange),
      physicalDescription: this.optional(value.physicalDescription),
      foundLocation: this.optional(value.foundLocation),
      foundAt: new Date().toISOString(),
      confidenceLevel: 'NONE',
      identityDeclarations: [],
    };
  }

  private emergencyTriagePayload(
    value: ReturnType<FormGroup['getRawValue']>,
  ): EmergencyTriageRequest {
    return {
      arrivalMode: value.arrivalMode,
      triageLevel: value.triageLevel,
      hemodynamicStatus: value.hemodynamicStatus,
      chiefComplaint: value.chiefComplaint.trim(),
      initialBpSystolic: value.initialBpSystolic,
      initialBpDiastolic: value.initialBpDiastolic,
      initialHr: value.initialHr,
      initialTemp: value.initialTemp,
      thirdPartyName: this.isAccompanied ? this.optional(value.thirdPartyName) : undefined,
      thirdPartyPhone: this.isAccompanied ? this.optional(value.thirdPartyPhone) : undefined,
      thirdPartyRelationship: this.isAccompanied ? this.optional(value.thirdPartyRelationship) : undefined,
      thirdPartyIdDocument: this.isAccompanied ? this.optional(value.thirdPartyIdDocument) : undefined,
      thirdPartyCircumstances: this.isAccompanied ? this.optional(value.thirdPartyCircumstances) : undefined,
      thirdPartyConsentToContact: this.isAccompanied && Boolean(value.thirdPartyConsentToContact),
    };
  }

  private validateStep(step: AdmissionStep): string | null {
    const value = this.form.getRawValue();

    if (step === 1) {
      if (this.patientMode === 'EXISTING' && !value.patientId) {
        return this.text('requiredPatient');
      }
      if (this.patientMode === 'NEW' && (
        !value.fullName?.trim() || !value.gender || !value.birthDate || !value.phone?.trim() || !value.city?.trim()
      )) {
        return this.text('requiredIdentity');
      }
      if (this.patientMode === 'PROVISIONAL' && this.carePath !== 'EMERGENCY') {
        return this.text('provisionalNormalForbidden');
      }
    }

    if (step === 2) {
      if (this.carePath === 'NORMAL' && !hasRequiredVisitDetails(value)) {
        return this.text('requiredVisit');
      }
      if (this.carePath === 'EMERGENCY' && !value.arrivalMode) {
        return this.text('requiredArrival');
      }
      if (this.carePath === 'EMERGENCY' && value.arrivalMode === 'ACCOMPANIED' && (
        !value.thirdPartyName?.trim()
        || !value.thirdPartyPhone?.trim()
        || !value.thirdPartyRelationship?.trim()
      )) {
        return this.text('requiredThirdParty');
      }
    }

    if (step === 3 && this.carePath === 'EMERGENCY' && !value.chiefComplaint?.trim()) {
      return this.text('requiredEmergency');
    }

    return null;
  }

  private localizedApiError(error: unknown, fallbackKey: string): string {
    const apiError = error as { error?: { code?: unknown; detail?: unknown; message?: unknown } };
    const candidate = apiError.error?.code ?? apiError.error?.detail ?? apiError.error?.message;
    if (typeof candidate === 'string' && /^[A-Z0-9_]+$/.test(candidate)) {
      const translationKey = `admission.error.${candidate}`;
      const translated = this.i18n.t(translationKey);
      if (translated !== translationKey) {
        return translated;
      }
    }
    return this.text(fallbackKey);
  }

  private optional(value: unknown): string | undefined {
    if (typeof value !== 'string') return undefined;
    const normalized = value.trim();
    return normalized.length > 0 ? normalized : undefined;
  }
}
