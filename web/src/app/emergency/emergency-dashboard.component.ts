import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AdmissionCompleted, UnifiedAdmissionComponent } from '../admission/unified-admission.component';
import { I18nService } from '../core/i18n/i18n.service';
import { AlertComponent } from '../shared/ui/alert.component';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { EmergencyApiService } from './emergency-api.service';
import { EmergencyRecord } from './emergency.models';

type EmergencyDetailTab = 'OVERVIEW' | 'IDENTITY' | 'CARE';
type EmergencyUiTextKey = keyof typeof EMERGENCY_UI_TEXT.fr;

const EMERGENCY_UI_TEXT = {
  fr: {
    viewRecord: "Voir le dossier d'urgence",
    activeFile: "Dossier d'urgence actif",
    overviewTab: 'Résumé',
    identityTab: 'Identité & tiers',
    careTab: 'Soins',
    patientIdentity: 'Identité du patient',
    emergencySummary: "Résumé de l'urgence",
    observableInfo: 'Informations observables',
    thirdParty: 'Personne ayant amené le patient',
    noThirdParty: "Aucun tiers n'est associé à ce dossier.",
    temporaryNumber: 'N° URG-TEMP',
    globalNumber: 'N° DPU',
    localNumber: 'N° local',
    identityStatus: "Statut d'identité",
    confidence: 'Niveau de confiance',
    apparentGender: 'Sexe apparent',
    estimatedAge: "Tranche d'âge estimée",
    physicalDescription: 'Description physique',
    foundLocation: 'Lieu de découverte',
    foundAt: 'Découvert / pris en charge le',
    arrivalMode: "Mode d'arrivée",
    arrivalAt: "Heure d'arrivée",
    triage: 'Niveau de triage',
    hemodynamic: 'État hémodynamique',
    complaint: "Motif d'admission",
    vitals: 'Constantes initiales',
    bloodPressure: 'Tension',
    pulse: 'Pouls',
    temperature: 'Température',
    name: 'Nom complet',
    phone: 'Téléphone',
    relationship: 'Lien avec le patient',
    idDocument: "Pièce d'identité / référence",
    circumstances: 'Circonstances déclarées',
    contactConsent: 'Autorisation de recontact',
    yes: 'Oui',
    no: 'Non',
    unknown: 'Non renseigné',
    provisional: 'Identité provisoire',
    declared: 'Identité déclarée',
    verified: 'Identité vérifiée',
    merged: 'Dossier rapproché',
    noneConfidence: 'Non vérifiée',
    lowConfidence: 'Faible',
    mediumConfidence: 'Moyenne',
    highConfidence: 'Élevée',
    verifiedConfidence: 'Vérifiée',
    loadingDetails: 'Chargement du dossier complet…',
    detailsError: "Impossible de charger le détail complet de l'urgence.",
    broughtBy: 'Amené par',
    registeredAt: 'Enregistré le',
  },
  en: {
    viewRecord: 'View emergency record',
    activeFile: 'Active emergency record',
    overviewTab: 'Summary',
    identityTab: 'Identity & third party',
    careTab: 'Care',
    patientIdentity: 'Patient identity',
    emergencySummary: 'Emergency summary',
    observableInfo: 'Observable information',
    thirdParty: 'Person who brought the patient',
    noThirdParty: 'No third party is associated with this record.',
    temporaryNumber: 'URG-TEMP No.',
    globalNumber: 'DPU No.',
    localNumber: 'Local No.',
    identityStatus: 'Identity status',
    confidence: 'Confidence level',
    apparentGender: 'Apparent gender',
    estimatedAge: 'Estimated age range',
    physicalDescription: 'Physical description',
    foundLocation: 'Location found',
    foundAt: 'Found / admitted at',
    arrivalMode: 'Arrival mode',
    arrivalAt: 'Arrival time',
    triage: 'Triage level',
    hemodynamic: 'Hemodynamic status',
    complaint: 'Admission reason',
    vitals: 'Initial vital signs',
    bloodPressure: 'Blood pressure',
    pulse: 'Pulse',
    temperature: 'Temperature',
    name: 'Full name',
    phone: 'Phone',
    relationship: 'Relationship to patient',
    idDocument: 'Identity document / reference',
    circumstances: 'Reported circumstances',
    contactConsent: 'Permission to contact',
    yes: 'Yes',
    no: 'No',
    unknown: 'Not provided',
    provisional: 'Provisional identity',
    declared: 'Declared identity',
    verified: 'Verified identity',
    merged: 'Merged record',
    noneConfidence: 'Unverified',
    lowConfidence: 'Low',
    mediumConfidence: 'Medium',
    highConfidence: 'High',
    verifiedConfidence: 'Verified',
    loadingDetails: 'Loading complete record…',
    detailsError: 'Unable to load the complete emergency record.',
    broughtBy: 'Brought by',
    registeredAt: 'Registered at',
  },
} as const;

@Component({
  selector: 'app-emergency-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe,
    AppShellComponent,
    PageHeaderComponent,
    ButtonComponent,
    AlertComponent,
    EmptyStateComponent,
    UnifiedAdmissionComponent,
  ],
  templateUrl: './emergency-dashboard.component.html',
})
export class EmergencyDashboardComponent implements OnInit {
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly emergencies = signal<EmergencyRecord[]>([]);
  readonly isLoading = signal(false);
  readonly isProcessing = signal(false);
  readonly isDetailLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly detailError = signal<string | null>(null);

  readonly isAdmissionModalOpen = signal(false);
  readonly selectedEmergency = signal<EmergencyRecord | null>(null);
  readonly isDrawerOpen = signal(false);
  readonly detailTab = signal<EmergencyDetailTab>('OVERVIEW');

  careForm!: FormGroup;

  readonly isStabilizeModalOpen = signal(false);
  readonly stabilizeOrientation = signal<string>('ADMISSION');

  ngOnInit(): void {
    this.initCareForm();
    this.loadEmergencies();
  }

  private initCareForm(): void {
    this.careForm = this.fb.group({
      actionType: ['VASCULAR_ACCESS', [Validators.required]],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      quantity: [null],
      unit: [''],
    });

    this.careForm.get('actionType')?.valueChanges.subscribe(type => {
      let unit = '';
      const qtyControl = this.careForm.get('quantity');

      if (type === 'FLUID_BOLUS') {
        unit = 'ml';
        qtyControl?.setValidators([Validators.required, Validators.min(1)]);
      } else if (type === 'MEDICATION') {
        unit = 'mg';
        qtyControl?.setValidators([Validators.required, Validators.min(0.01)]);
      } else {
        qtyControl?.clearValidators();
        qtyControl?.setValue(null);
      }
      qtyControl?.updateValueAndValidity();
      this.careForm.patchValue({ unit });
    });
  }

  loadEmergencies(): void {
    this.isLoading.set(true);
    this.emergencyApi.getActive().subscribe({
      next: (data) => {
        this.emergencies.set(data);
        this.isLoading.set(false);
        const selected = this.selectedEmergency();
        if (selected) {
          const updated = data.find(item => item.id === selected.id);
          if (updated) this.selectedEmergency.set(updated);
          else this.closeDrawer();
        }
      },
      error: () => {
        this.error.set(this.t('emergency.error.load'));
        this.isLoading.set(false);
      },
    });
  }

  openAdmissionModal(): void {
    this.isAdmissionModalOpen.set(true);
  }

  closeAdmissionModal(): void {
    this.isAdmissionModalOpen.set(false);
  }

  onAdmissionCompleted(result: AdmissionCompleted): void {
    this.closeAdmissionModal();
    if (result.carePath === 'EMERGENCY') {
      this.loadEmergencies();
      return;
    }
    void this.router.navigate(['/patients', result.patientId]);
  }

  openDrawer(record: EmergencyRecord): void {
    this.selectedEmergency.set(record);
    this.detailTab.set('OVERVIEW');
    this.detailError.set(null);
    this.initCareForm();
    this.isDrawerOpen.set(true);
    this.isDetailLoading.set(true);

    this.emergencyApi.getById(record.id).subscribe({
      next: detail => {
        this.selectedEmergency.set(detail);
        this.isDetailLoading.set(false);
      },
      error: () => {
        this.detailError.set(this.ui('detailsError'));
        this.isDetailLoading.set(false);
      },
    });
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedEmergency.set(null);
    this.detailError.set(null);
  }

  setDetailTab(tab: EmergencyDetailTab): void {
    this.detailTab.set(tab);
  }

  onSubmitCare(): void {
    const record = this.selectedEmergency();
    if (!record || this.careForm.invalid) {
      this.careForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const dto = this.careForm.value;

    this.emergencyApi.addResuscitationLog(record.id, dto).subscribe({
      next: () => {
        this.careForm.get('description')?.reset();
        this.careForm.get('quantity')?.reset();
        this.isProcessing.set(false);
        this.loadEmergencies();
      },
      error: () => {
        this.error.set(this.t('emergency.error.addCare'));
        this.isProcessing.set(false);
      },
    });
  }

  openStabilizeModal(): void {
    this.isStabilizeModalOpen.set(true);
  }

  closeStabilizeModal(): void {
    this.isStabilizeModalOpen.set(false);
  }

  onSubmitStabilize(): void {
    const record = this.selectedEmergency();
    if (!record) return;

    this.isProcessing.set(true);
    this.emergencyApi.stabilize(record.id, this.stabilizeOrientation()).subscribe({
      next: () => {
        this.loadEmergencies();
        this.closeStabilizeModal();
        this.closeDrawer();
        this.isProcessing.set(false);
      },
      error: () => {
        this.error.set(this.t('emergency.error.stabilize'));
        this.isProcessing.set(false);
      },
    });
  }

  patientDisplayName(record: EmergencyRecord): string {
    return record.patientName || record.temporaryPatientNumber || record.globalPatientNumber || this.ui('unknown');
  }

  patientPrimaryNumber(record: EmergencyRecord): string {
    return record.temporaryPatientNumber || record.globalPatientNumber || record.localPatientNumber;
  }

  isProvisional(record: EmergencyRecord): boolean {
    return record.identityStatus === 'PROVISIONAL_URGENCY' || Boolean(record.temporaryPatientNumber);
  }

  hasThirdParty(record: EmergencyRecord): boolean {
    return Boolean(record.thirdPartyName || record.thirdPartyPhone || record.thirdPartyRelationship);
  }

  arrivalLabel(mode: string): string {
    const labels: Record<string, [string, string]> = {
      AMBULANCE: ['Ambulance', 'Ambulance'],
      FIRE_DEPT: ['Sapeurs-pompiers', 'Fire and rescue service'],
      WALK_IN: ['Arrivée autonome', 'Walk-in'],
      ACCOMPANIED: ['Amené par un tiers', 'Brought by another person'],
    };
    const value = labels[mode];
    return value ? value[this.i18n.locale() === 'en' ? 1 : 0] : mode;
  }

  triageLabel(level: string): string {
    const labels: Record<string, [string, string]> = {
      RED: ['Rouge — détresse vitale', 'Red — life-threatening'],
      ORANGE: ['Orange — très urgent', 'Orange — very urgent'],
      YELLOW: ['Jaune — urgent', 'Yellow — urgent'],
      GREEN: ['Vert — non urgent', 'Green — non-urgent'],
    };
    const value = labels[level];
    return value ? value[this.i18n.locale() === 'en' ? 1 : 0] : level;
  }

  hemodynamicLabel(status: string): string {
    const labels: Record<string, [string, string]> = {
      SHOCK: ['Choc', 'Shock'],
      UNSTABLE: ['Instable', 'Unstable'],
      STABLE: ['Stable', 'Stable'],
    };
    const value = labels[status];
    return value ? value[this.i18n.locale() === 'en' ? 1 : 0] : status;
  }

  identityStatusLabel(status?: string): string {
    const keys: Record<string, EmergencyUiTextKey> = {
      PROVISIONAL_URGENCY: 'provisional',
      DECLARED: 'declared',
      VERIFIED: 'verified',
      MERGED: 'merged',
    };
    return status && keys[status] ? this.ui(keys[status]) : this.ui('unknown');
  }

  confidenceLabel(confidence?: string): string {
    const keys: Record<string, EmergencyUiTextKey> = {
      NONE: 'noneConfidence',
      LOW: 'lowConfidence',
      MEDIUM: 'mediumConfidence',
      HIGH: 'highConfidence',
      VERIFIED: 'verifiedConfidence',
    };
    return confidence && keys[confidence] ? this.ui(keys[confidence]) : this.ui('unknown');
  }

  valueOrUnknown(value?: string | null): string {
    return value?.trim() || this.ui('unknown');
  }

  ui(key: EmergencyUiTextKey): string {
    const locale = this.i18n.locale() === 'en' ? 'en' : 'fr';
    return EMERGENCY_UI_TEXT[locale][key];
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}
