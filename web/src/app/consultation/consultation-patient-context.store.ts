import { Injectable, computed, inject, signal } from '@angular/core';
import { ClinicalDataLoadStatus } from '../patient/clinical-data-load-status';
import { PatientApiService } from '../patient/patient-api.service';
import { Patient, PatientAllergy, PatientMedicalHistory } from '../patient/patient.models';

/** Clé i18n et état d'une section du contexte patient dont le chargement a échoué. */
export interface FailedPatientContextSection {
  readonly messageKey: string;
}

/**
 * Contexte patient de la consultation (identité, allergies, antécédents) avec un état de
 * chargement explicite : un échec n'est jamais confondu avec « aucune allergie ».
 */
@Injectable()
export class ConsultationPatientContextStore {
  private readonly patientApi = inject(PatientApiService);
  private patientId = '';

  readonly patient = signal<Patient | null>(null);
  readonly allergies = signal<PatientAllergy[]>([]);
  readonly medicalHistory = signal<PatientMedicalHistory[]>([]);
  readonly patientStatus = signal<ClinicalDataLoadStatus>('idle');
  readonly allergiesStatus = signal<ClinicalDataLoadStatus>('idle');
  readonly historyStatus = signal<ClinicalDataLoadStatus>('idle');

  /** Signature, clôture et ordonnance exigent des allergies effectivement vérifiées. */
  readonly allergiesVerified = computed(() => this.allergiesStatus() === 'loaded');

  readonly failedSections = computed<FailedPatientContextSection[]>(() => [
    ...(this.patientStatus() === 'error' ? [{ messageKey: 'clinicalSafety.patient.unverified' }] : []),
    ...(this.allergiesStatus() === 'error' ? [{ messageKey: 'clinicalSafety.allergies.unverified' }] : []),
    ...(this.historyStatus() === 'error' ? [{ messageKey: 'clinicalSafety.history.unverified' }] : []),
  ]);

  readonly retrying = computed(() =>
    this.patientStatus() === 'loading' || this.allergiesStatus() === 'loading' || this.historyStatus() === 'loading',
  );

  load(patientId: string): void {
    this.patientId = patientId;
    this.loadPatient();
    this.loadAllergies();
    this.loadHistory();
  }

  /** Relance uniquement les sections en échec. */
  retryFailed(): void {
    if (!this.patientId) return;
    if (this.patientStatus() === 'error') this.loadPatient();
    if (this.allergiesStatus() === 'error') this.loadAllergies();
    if (this.historyStatus() === 'error') this.loadHistory();
  }

  private loadPatient(): void {
    this.patientStatus.set('loading');
    this.patientApi.getById(this.patientId).subscribe({
      next: (patient) => {
        this.patient.set(patient);
        this.patientStatus.set('loaded');
      },
      error: () => this.patientStatus.set('error'),
    });
  }

  private loadAllergies(): void {
    this.allergiesStatus.set('loading');
    this.patientApi.getAllergies(this.patientId).subscribe({
      next: (allergies) => {
        this.allergies.set(allergies);
        this.allergiesStatus.set('loaded');
      },
      error: () => {
        this.allergies.set([]);
        this.allergiesStatus.set('error');
      },
    });
  }

  private loadHistory(): void {
    this.historyStatus.set('loading');
    this.patientApi.getMedicalHistory(this.patientId).subscribe({
      next: (history) => {
        this.medicalHistory.set(history);
        this.historyStatus.set('loaded');
      },
      error: () => {
        this.medicalHistory.set([]);
        this.historyStatus.set('error');
      },
    });
  }
}
