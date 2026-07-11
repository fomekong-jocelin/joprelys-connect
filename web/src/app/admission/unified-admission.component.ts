import { CommonModule } from '@angular/common';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize, map, of, switchMap } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { CreateEmergencyRequest } from '../emergency/emergency.models';
import { EmergencyApiService } from '../emergency/emergency-api.service';
import { PatientApiService } from '../patient/patient-api.service';
import { Patient } from '../patient/patient.models';
import { ProvisionalPatientApiService } from '../patient/provisional-patient-api.service';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { VisitApiService } from '../visit/visit-api.service';

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

const ADMISSION_TEXT = {
  fr: {
    title: 'Nouvelle admission',
    subtitle: 'Un parcours guidé en trois étapes pour aller à l’essentiel.',
    stepPatient: 'Patient',
    stepArrival: 'Arrivée',
    stepVisit: 'Visite',
    stepTriage: 'Triage',
    stepConfirm: 'Confirmation',
    stepOf: 'Étape',
    carePath: 'Type de prise en charge',
    normal: 'Admission normale',
    normalHint: 'Consultation ou visite programmée',
    emergency: 'Urgence',
    emergencyHint: 'Prise en charge et triage immédiats',
    patientSituation: 'Situation du patient',
    existing: 'Patient existant',
    new: 'Nouveau patient identifié',
    provisional: 'Patient inconnu / inconscient',
    selectPatient: 'Rechercher et sélectionner le patient',
    selectPatientPlaceholder: '-- Sélectionner un patient --',
    identity: 'Identité et administration',
    fullName: 'Nom complet',
    gender: 'Sexe',
    genderPlaceholder: 'Sélectionner',
    male: 'Masculin',
    female: 'Féminin',
    birthDate: 'Date de naissance',
    phone: 'Téléphone',
    city: 'Ville',
    district: 'Quartier / district',
    address: 'Adresse',
    email: 'Adresse e-mail',
    provisionalTitle: 'Informations observables',
    provisionalHint: 'Aucune identité fictive n’est requise. Un numéro URG-TEMP sera généré automatiquement.',
    apparentGender: 'Sexe apparent',
    unknown: 'Non déterminé',
    estimatedAge: 'Tranche d’âge estimée',
    physicalDescription: 'Description physique / signes distinctifs',
    foundLocation: 'Lieu de découverte ou de prise en charge',
    visit: 'Informations de la visite',
    reason: 'Motif de la visite',
    orientation: 'Orientation',
    service: 'Service',
    emergencyArrival: 'Contexte d’arrivée',
    arrivalMode: 'Mode d’arrivée',
    ambulance: 'Ambulance',
    fireDept: 'Sapeurs-pompiers',
    walkIn: 'Arrivée par ses propres moyens',
    accompanied: 'Amené par un tiers',
    thirdPartyTitle: 'Personne ayant amené le patient',
    thirdPartyHint: 'Cette personne est enregistrée comme source d’information. Elle n’est pas automatiquement considérée comme représentant légal.',
    thirdPartyName: 'Nom complet',
    thirdPartyPhone: 'Téléphone',
    thirdPartyRelationship: 'Lien avec le patient',
    relationshipPlaceholder: 'Sélectionner le lien',
    relationshipFamily: 'Famille',
    relationshipFriend: 'Ami / connaissance',
    relationshipWitness: 'Témoin',
    relationshipDriver: 'Chauffeur / transporteur',
    relationshipPolice: 'Police / autorité',
    relationshipOther: 'Autre',
    thirdPartyIdDocument: 'Pièce d’identité / référence',
    thirdPartyIdDocumentHint: 'Facultatif si indisponible à l’arrivée',
    thirdPartyCircumstances: 'Circonstances de découverte ou de transport',
    consentToContact: 'La personne accepte d’être recontactée si nécessaire',
    emergencySection: 'Triage initial',
    triageLevel: 'Niveau de triage',
    red: 'Rouge — choc / détresse vitale',
    orange: 'Orange — très urgent',
    yellow: 'Jaune — urgent',
    green: 'Vert — non urgent',
    hemodynamic: 'État hémodynamique',
    shock: 'Choc',
    unstable: 'Instable',
    stable: 'Stable',
    temperature: 'Température (°C)',
    bp: 'Tension artérielle',
    pulse: 'Pouls (BPM)',
    complaint: 'Motif d’admission / mécanisme',
    confirmTitle: 'Vérifier avant validation',
    confirmHint: 'Les informations pourront être complétées après la prise en charge.',
    summaryPath: 'Parcours',
    summaryPatient: 'Patient',
    summaryArrival: 'Arrivée',
    summaryThirdParty: 'Tiers',
    summaryVisit: 'Visite',
    cancel: 'Annuler',
    previous: 'Retour',
    next: 'Continuer',
    saveNormal: 'Créer la visite',
    saveEmergency: 'Démarrer l’urgence',
    saving: 'Enregistrement…',
    requiredPatient: 'Sélectionnez un patient existant.',
    requiredIdentity: 'Renseignez les champs obligatoires du nouveau patient.',
    provisionalNormalForbidden: 'Le dossier URG-TEMP est réservé au parcours d’urgence.',
    requiredVisit: 'Renseignez le motif et l’orientation de la visite.',
    requiredArrival: 'Sélectionnez le mode d’arrivée.',
    requiredThirdParty: 'Renseignez le nom, le téléphone et le lien de la personne ayant amené le patient.',
    requiredEmergency: 'Renseignez le motif d’admission de l’urgence.',
    invalidValues: 'Vérifiez les valeurs saisies avant de continuer.',
    loadPatientsError: 'Impossible de charger la liste des patients.',
    saveError: 'Impossible de créer l’admission.',
  },
  en: {
    title: 'New admission',
    subtitle: 'A guided three-step workflow focused on essential information.',
    stepPatient: 'Patient',
    stepArrival: 'Arrival',
    stepVisit: 'Visit',
    stepTriage: 'Triage',
    stepConfirm: 'Confirmation',
    stepOf: 'Step',
    carePath: 'Care pathway',
    normal: 'Standard admission',
    normalHint: 'Consultation or scheduled visit',
    emergency: 'Emergency',
    emergencyHint: 'Immediate triage and care',
    patientSituation: 'Patient situation',
    existing: 'Existing patient',
    new: 'New identified patient',
    provisional: 'Unknown / unconscious patient',
    selectPatient: 'Search and select the patient',
    selectPatientPlaceholder: '-- Select a patient --',
    identity: 'Identity and administration',
    fullName: 'Full name',
    gender: 'Gender',
    genderPlaceholder: 'Select',
    male: 'Male',
    female: 'Female',
    birthDate: 'Date of birth',
    phone: 'Phone',
    city: 'City',
    district: 'District',
    address: 'Address',
    email: 'Email address',
    provisionalTitle: 'Observable information',
    provisionalHint: 'No invented identity is required. An URG-TEMP number will be generated automatically.',
    apparentGender: 'Apparent gender',
    unknown: 'Undetermined',
    estimatedAge: 'Estimated age range',
    physicalDescription: 'Physical description / distinguishing signs',
    foundLocation: 'Location found or picked up',
    visit: 'Visit information',
    reason: 'Reason for visit',
    orientation: 'Orientation',
    service: 'Department',
    emergencyArrival: 'Arrival context',
    arrivalMode: 'Arrival mode',
    ambulance: 'Ambulance',
    fireDept: 'Fire and rescue service',
    walkIn: 'Walk-in',
    accompanied: 'Brought by another person',
    thirdPartyTitle: 'Person who brought the patient',
    thirdPartyHint: 'This person is recorded as an information source and is not automatically treated as the patient’s legal representative.',
    thirdPartyName: 'Full name',
    thirdPartyPhone: 'Phone',
    thirdPartyRelationship: 'Relationship to patient',
    relationshipPlaceholder: 'Select relationship',
    relationshipFamily: 'Family',
    relationshipFriend: 'Friend / acquaintance',
    relationshipWitness: 'Witness',
    relationshipDriver: 'Driver / transporter',
    relationshipPolice: 'Police / authority',
    relationshipOther: 'Other',
    thirdPartyIdDocument: 'Identity document / reference',
    thirdPartyIdDocumentHint: 'Optional when unavailable on arrival',
    thirdPartyCircumstances: 'Circumstances of discovery or transport',
    consentToContact: 'The person agrees to be contacted again if needed',
    emergencySection: 'Initial triage',
    triageLevel: 'Triage level',
    red: 'Red — shock / life-threatening distress',
    orange: 'Orange — very urgent',
    yellow: 'Yellow — urgent',
    green: 'Green — non-urgent',
    hemodynamic: 'Hemodynamic status',
    shock: 'Shock',
    unstable: 'Unstable',
    stable: 'Stable',
    temperature: 'Temperature (°C)',
    bp: 'Blood pressure',
    pulse: 'Pulse (BPM)',
    complaint: 'Admission reason / mechanism',
    confirmTitle: 'Review before confirmation',
    confirmHint: 'The information can be completed after immediate care.',
    summaryPath: 'Pathway',
    summaryPatient: 'Patient',
    summaryArrival: 'Arrival',
    summaryThirdParty: 'Third party',
    summaryVisit: 'Visit',
    cancel: 'Cancel',
    previous: 'Back',
    next: 'Continue',
    saveNormal: 'Create visit',
    saveEmergency: 'Start emergency care',
    saving: 'Saving…',
    requiredPatient: 'Select an existing patient.',
    requiredIdentity: 'Complete the required identity fields.',
    provisionalNormalForbidden: 'URG-TEMP records are only available for emergency care.',
    requiredVisit: 'Enter the visit reason and orientation.',
    requiredArrival: 'Select the arrival mode.',
    requiredThirdParty: 'Enter the name, phone number and relationship of the person who brought the patient.',
    requiredEmergency: 'Enter the emergency admission reason.',
    invalidValues: 'Review the entered values before continuing.',
    loadPatientsError: 'Unable to load the patient list.',
    saveError: 'Unable to create the admission.',
  },
} as const;

type AdmissionTextKey = keyof typeof ADMISSION_TEXT.fr;

@Component({
  selector: 'app-unified-admission',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AlertComponent, ButtonComponent],
  templateUrl: './unified-admission.component.html',
})
export class UnifiedAdmissionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly patientApi = inject(PatientApiService);
  private readonly provisionalPatientApi = inject(ProvisionalPatientApiService);
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly visitApi = inject(VisitApiService);
  private readonly i18n = inject(I18nService);

  readonly initialCarePath = input<AdmissionCarePath>('NORMAL');
  readonly cancelled = output<void>();
  readonly completed = output<AdmissionCompleted>();

  readonly patients = signal<Patient[]>([]);
  readonly isLoadingPatients = signal(false);
  readonly isSubmitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly currentStep = signal<AdmissionStep>(1);
  readonly steps: readonly AdmissionStep[] = [1, 2, 3];

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

    reason: [''],
    orientation: ['CONSULTATION'],
    service: [''],

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
    this.loadPatients();
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
    this.currentStep.set(1);
    this.error.set(null);
  }

  setPatientMode(mode: AdmissionPatientMode): void {
    this.form.patchValue({ patientMode: mode, patientId: '' });
    this.error.set(null);
  }

  nextStep(): void {
    const validationError = this.validateStep(this.currentStep());
    if (validationError) {
      this.error.set(validationError);
      this.form.markAllAsTouched();
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
    if (step === 2) return this.carePath === 'EMERGENCY' ? this.text('stepArrival') : this.text('stepVisit');
    return this.carePath === 'EMERGENCY' ? this.text('stepTriage') : this.text('stepConfirm');
  }

  progressPercent(): number {
    return (this.currentStep() / 3) * 100;
  }

  patientSummary(): string {
    const value = this.form.getRawValue();
    if (this.patientMode === 'EXISTING') {
      const patient = this.patients().find((item) => item.id === value.patientId);
      return patient ? this.displayPatient(patient) : this.text('existing');
    }
    if (this.patientMode === 'NEW') {
      return value.fullName?.trim() || this.text('new');
    }
    return this.text('provisional');
  }

  arrivalSummary(): string {
    const arrivalMode = this.form.get('arrivalMode')?.value;
    const mapping: Record<string, AdmissionTextKey> = {
      AMBULANCE: 'ambulance',
      FIRE_DEPT: 'fireDept',
      WALK_IN: 'walkIn',
      ACCOMPANIED: 'accompanied',
    };
    return mapping[arrivalMode] ? this.text(mapping[arrivalMode]) : '—';
  }

  submit(): void {
    const steps: AdmissionStep[] = [1, 2, 3];
    for (const step of steps) {
      const validationError = this.validateStep(step);
      if (validationError) {
        this.currentStep.set(step);
        this.error.set(validationError);
        this.form.markAllAsTouched();
        return;
      }
    }

    if (this.form.invalid || this.isSubmitting()) {
      this.error.set(this.text('invalidValues'));
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.isSubmitting.set(true);
    this.error.set(null);

    this.resolvePatient().pipe(
      switchMap((patient) => {
        if (this.carePath === 'EMERGENCY') {
          const request: CreateEmergencyRequest = {
            patientId: patient.id,
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
          return this.emergencyApi.create(request).pipe(map((emergency) => ({
            carePath: this.carePath,
            patientId: patient.id,
            patientDisplayName: patient.displayName,
            emergencyId: emergency.id,
          } satisfies AdmissionCompleted)));
        }

        return this.visitApi.create({
          patientId: patient.id,
          reason: value.reason.trim(),
          orientation: value.orientation.trim(),
          service: this.optional(value.service),
          arrivalAt: new Date().toISOString(),
        }).pipe(map((visit) => ({
          carePath: this.carePath,
          patientId: patient.id,
          patientDisplayName: patient.displayName,
          visitId: visit.id,
        } satisfies AdmissionCompleted)));
      }),
      finalize(() => this.isSubmitting.set(false)),
    ).subscribe({
      next: (result) => this.completed.emit(result),
      error: (err) => this.error.set(
        err.error?.detail || err.error?.message || this.text('saveError')
      ),
    });
  }

  displayPatient(patient: Patient): string {
    return patient.displayName || patient.fullName || patient.temporaryPatientNumber || patient.globalPatientNumber;
  }

  text(key: AdmissionTextKey): string {
    const locale = this.i18n.locale() === 'en' ? 'en' : 'fr';
    return ADMISSION_TEXT[locale][key];
  }

  private loadPatients(): void {
    this.isLoadingPatients.set(true);
    this.patientApi.list().pipe(finalize(() => this.isLoadingPatients.set(false))).subscribe({
      next: (patients) => this.patients.set(patients),
      error: () => this.error.set(this.text('loadPatientsError')),
    });
  }

  private resolvePatient(): Observable<{ id: string; displayName: string }> {
    const value = this.form.getRawValue();

    if (this.patientMode === 'EXISTING') {
      const patient = this.patients().find((item) => item.id === value.patientId)!;
      return of({ id: patient.id, displayName: this.displayPatient(patient) });
    }

    if (this.patientMode === 'NEW') {
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

    return this.provisionalPatientApi.create({
      apparentGender: value.apparentGender === 'UNKNOWN' ? undefined : value.apparentGender,
      estimatedAgeRange: this.optional(value.estimatedAgeRange),
      physicalDescription: this.optional(value.physicalDescription),
      foundLocation: this.optional(value.foundLocation),
      foundAt: new Date().toISOString(),
      confidenceLevel: 'NONE',
      identityDeclarations: [],
    }).pipe(map((response) => ({
      id: response.patient.id,
      displayName: response.patient.displayName,
    })));
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
      if (this.carePath === 'NORMAL' && (!value.reason?.trim() || !value.orientation?.trim())) {
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

  private optional(value: unknown): string | undefined {
    if (typeof value !== 'string') return undefined;
    const normalized = value.trim();
    return normalized.length > 0 ? normalized : undefined;
  }
}
