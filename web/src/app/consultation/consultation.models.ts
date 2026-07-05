import { Vitals } from '../visit/visit.models';

export interface Consultation {
  id: string;
  visitId: string;
  visitNumber: string;
  doctorId: string;
  doctorName: string;
  documentNumber: string;
  symptoms: string;
  clinicalExam?: string;
  suspectedDiagnosis?: string;
  diagnosis: string;
  finalDiagnosis?: string;
  conclusion?: string;
  advice?: string;
  followUp?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  documentId?: string;
  documentStatus?: string;
  vitals?: Vitals;
  prescriptionId?: string;
  prescriptionNumber?: string;
  prescriptionStatus?: string;
  prescriptionTransmissionStatus?: string;
  prescriptionTransmittedAt?: string;
  prescriptionItems?: PrescriptionItem[];
}

export interface SaveConsultationRequest {
  symptoms: string;
  clinicalExam?: string;
  suspectedDiagnosis?: string;
  diagnosis: string;
  finalDiagnosis?: string;
  conclusion?: string;
  advice?: string;
  followUp?: string;
}

export interface PrescriptionItem {
  id?: string;
  drugName: string;
  dosage: string;
  posology?: string;
  duration?: string;
  quantity?: string;
  instructions?: string;
  sortOrder?: number;
  form?: string;
  route?: string;
  frequency?: string;
  substitutionAllowed?: boolean;
}

export interface Prescription {
  id: string;
  consultationId: string;
  items: PrescriptionItem[];
  status: string;
  prescriptionNumber?: string;
  pinCode?: string;
  expiresAt?: string;
  transmissionStatus?: string;
  transmittedAt?: string;
  issuedAt?: string;
  documentId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SavePrescriptionRequest {
  items: PrescriptionItem[];
}
