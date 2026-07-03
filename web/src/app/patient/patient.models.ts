export interface Patient {
  id: string;
  organizationId: string;
  globalPatientNumber: string;
  localPatientNumber: string;
  fullName: string;
  gender: string;
  birthDate: string; // ISO string date YYYY-MM-DD
  phone: string;
  city: string;
  district?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  allergies?: string;
  medicalHistory?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  emergencyAccessActive?: boolean;
}

export interface CreatePatientDto {
  fullName: string;
  gender: string;
  birthDate: string;
  phone: string;
  city: string;
  district?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  allergies?: string;
  medicalHistory?: string;
}

export interface LabOrder {
  id: string;
  examRequestNumber: string;
  patientId: string;
  patientName: string;
  visitId?: string;
  requesterPractitionerId: string;
  requesterPractitionerName: string;
  sourceOrganizationId: string;
  targetOrganizationId?: string;
  examType: string;
  exams: string[];
  reason?: string;
  priority: string;
  status: string;
  createdAt: string;
}

export interface LabResult {
  id: string;
  resultNumber: string;
  examRequestNumber: string;
  patientId: string;
  validatorName: string;
  analyteName: string;
  value: string;
  unit?: string;
  referenceRange?: string;
  interpretation: string;
  comment?: string;
  pdfFilePath?: string;
  sampleCollectedAt?: string;
  resultAt?: string;
  validatedAt?: string;
  createdAt: string;
}

export interface PatientAllergy {
  id?: string;
  patientId: string;
  substance: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reaction?: string;
  status: 'ACTIVE' | 'INACTIVE';
  discoveredAt?: string;
  comment?: string;
  updatedAt?: string;
}

export interface CreatePatientAllergyRequest {
  substance: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reaction?: string;
  status?: 'ACTIVE' | 'INACTIVE';
  discoveredAt?: string;
  comment?: string;
}

export interface PatientMedicalHistory {
  id?: string;
  patientId: string;
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
  updatedAt?: string;
}

export interface CreatePatientMedicalHistoryRequest {
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
}

export interface Hospitalization {
  id: string;
  patientId: string;
  organizationId: string;
  version: number;
  serviceName: string;
  roomNumber: string;
  bedNumber: string;
  admissionReason: string;
  status: 'EN_COURS' | 'SORTI';
  admittedAt: string;
  dischargedAt?: string;
  dischargeDiagnosis?: string;
  dischargeInstructions?: string;
  pdfFilePath?: string;
}

export interface CreateHospitalizationRequest {
  patientId: string;
  serviceName: string;
  roomNumber: string;
  bedNumber: string;
  admissionReason: string;
}

export interface HospitalizationNote {
  id: string;
  hospitalizationId: string;
  organizationId: string;
  authorName: string;
  noteContent: string;
  createdAt: string;
}

export interface CreateHospitalizationNoteRequest {
  noteContent: string;
}

export interface DischargeHospitalizationRequest {
  dischargeDiagnosis: string;
  dischargeInstructions: string;
}


