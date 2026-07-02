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
  bmi?: number;
}

export interface Visit {
  id: string;
  visitNumber: string;
  patientId: string;
  patientName: string;
  patientDpu: string;
  reason: string;
  orientation: string;
  status: string;
  createdAt: string;
  closedAt?: string;
  vitals?: Vitals;
}

export interface CreateVisitRequest {
  patientId: string;
  reason: string;
  orientation: string;
}
