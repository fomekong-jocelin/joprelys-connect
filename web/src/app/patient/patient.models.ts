export type PatientIdentityStatus = 'PROVISIONAL_URGENCY' | 'DECLARED' | 'VERIFIED' | 'MERGED';

export interface Patient {
  id: string;
  organizationId: string;
  globalPatientNumber: string;
  localPatientNumber: string;
  fullName: string;
  gender: string;
  birthDate: string;
  phone?: string;
  city: string;
  district?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  allergies?: string;
  medicalHistory?: string;
  bloodGroup?: string;
  email?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  emergencyAccessActive?: boolean;
  identityStatus?: PatientIdentityStatus;
  temporaryPatientNumber?: string;
  displayName?: string;
  identityConfidenceLevel?: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERIFIED';
  apparentGender?: string;
  estimatedAgeRange?: string;
  physicalDescription?: string;
  foundAt?: string;
  foundLocation?: string;
}

export interface CreatePatientDto {
  fullName: string;
  gender: string;
  birthDate: string;
  phone?: string;
  city: string;
  district?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  allergies?: string;
  medicalHistory?: string;
  bloodGroup?: string;
  email?: string;
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
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER' | 'ALLERGIC' | 'SOCIAL';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
  important?: boolean;
  updatedAt?: string;
}

export interface CreatePatientMedicalHistoryRequest {
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER' | 'ALLERGIC' | 'SOCIAL';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
  important?: boolean;
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
  status: 'EN_COURS' | 'SORTI' | 'SORTI_CONTRE_AVIS';
  admittedAt: string;
  dischargedAt?: string;
  dischargeDiagnosis?: string;
  dischargeInstructions?: string;
  dischargeDecidedAt?: string;
  dischargeDecidedBy?: string;
  dischargeAgainstMedicalAdvice?: boolean;
  physicalDepartureAt?: string;
  physicalDepartureBy?: string;
  physicalDepartureNote?: string;
  pdfFilePath?: string;
  hospitalizationNumber: string;
  visitId: string;
  responsiblePractitionerId: string;
  documentId?: string;
}

export interface CreateHospitalizationRequest {
  patientId: string;
  serviceName: string;
  roomNumber: string;
  bedNumber: string;
  admissionReason: string;
  visitId: string;
  responsiblePractitionerId: string;
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
  againstMedicalAdvice?: boolean;
}

export interface ConfirmPhysicalDepartureRequest {
  confirmed: true;
  note?: string;
}

export interface PatientDuplicateCandidate {
  id: string;
  sourcePatient: Patient;
  targetPatient: Patient;
  similarityScore: number;
  status: 'PENDING' | 'RESOLVED' | 'IGNORED';
  createdAt: string;
}

export interface PatientVaccination {
  id?: string;
  patientId: string;
  vaccineName: string;
  batchNumber?: string;
  administeredAt: string;
  administeredBy?: string;
  notes?: string;
  nextDoseAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePatientVaccinationRequest {
  vaccineName: string;
  batchNumber?: string;
  administeredAt: string;
  administeredBy?: string;
  notes?: string;
  nextDoseAt?: string;
}

export interface MedicalCaptchaResponse {
  captchaId: string;
  question: string;
}

export interface PatientPreRegistrationRequest {
  organizationId: string;
  firstName: string;
  lastName: string;
  birthDate?: string;
  gender?: string;
  phone?: string;
  email?: string;
  city?: string;
  district?: string;
  address?: string;
  captchaId: string;
  captchaAnswer: string;
}

export interface PatientPreRegistrationResponse {
  id: string;
  organizationId: string;
  status: string;
  firstName: string;
  lastName: string;
  birthDate?: string;
  gender?: string;
  phone?: string;
  email?: string;
  city?: string;
  district?: string;
  address?: string;
  submittedAt: string;
  processedAt?: string;
  processedBy?: string;
  patientId?: string;
}

export interface PreRegistrationValidationRequest {
  localPatientNumber?: string;
}

export interface PreRegistrationPage {
  content: PatientPreRegistrationResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
