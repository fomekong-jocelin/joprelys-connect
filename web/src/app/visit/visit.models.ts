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
}

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
}

export interface CreateVisitRequest {
  patientId: string;
  reason: string;
  orientation: string;
  service?: string;
  mainPractitionerId?: string;
  arrivalAt?: string;
}
