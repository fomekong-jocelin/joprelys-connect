export interface Vitals {
  temperature?: number;
  weight?: number;
  height?: number;
  pulse?: number;
  systolic?: number;
  diastolic?: number;
  spo2?: number;
  glycemia?: number;
  respiratoryRate?: number;
  painScale?: number;
  bmi?: number;
  /** Heure de la dernière saisie des constantes (renvoyée par le backend). */
  recordedAt?: string;
  /** Alertes calculées par le backend (seuils adultes). */
  alerts?: VitalAlert[];
}

export type VitalAlertSeverity = 'WARNING' | 'CRITICAL';

export interface VitalAlert {
  code: string;
  severity: VitalAlertSeverity;
}

export interface VitalMeasurement extends Vitals {
  id: string;
  recordedAt: string;
  recordedBy?: string;
  recordedByName?: string;
}

/** Étape de prise en charge d'une visite active. */
export type VisitCareStage = 'ATTENTE_CONSTANTES' | 'PRET_MEDECIN' | 'EN_CONSULTATION';

export type ActiveVisitScope = 'ALL' | 'MINE' | 'SERVICE';

export interface Visit {
  id: string;
  visitNumber: string;
  patientId: string;
  patientName: string;
  patientDpu: string;
  reason: string;
  orientation: string;
  service?: string;
  mainPractitionerId?: string;
  status: string;
  arrivalAt?: string;
  createdAt: string;
  closedAt?: string;
  vitals?: Vitals;
  careStage?: VisitCareStage;
  consultingPractitionerId?: string;
  consultingPractitionerName?: string;
  consultationStartedAt?: string;
}

export interface CreateVisitRequest {
  patientId: string;
  reason: string;
  orientation: string;
  service?: string;
  mainPractitionerId?: string;
  arrivalAt?: string;
}

export interface AdmissionPractitionerOption {
  id: string;
  displayName: string;
  role: 'MEDECIN' | 'INFIRMIER';
  unitNames: string[];
}

/** Référentiels d'ouverture de visite (accessibles à l'accueil). */
export interface VisitAdmissionOptions {
  services: string[];
  practitioners: AdmissionPractitionerOption[];
}
