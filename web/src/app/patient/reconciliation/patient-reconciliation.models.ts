export type PatientIdentityStatus = 'PROVISIONAL_URGENCY' | 'DECLARED' | 'VERIFIED' | 'MERGED';
export type IdentityConfidenceLevel = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERIFIED';
export type PatientReconciliationDecision = 'CREATE_NEW_DPU' | 'LINK_EXISTING_DPU' | 'DEFER';
export type PatientReconciliationEventDecision = PatientReconciliationDecision | 'CORRECT_LINK';
export type IdentitySourceType =
  | 'PATIENT'
  | 'ACCOMPANYING_PERSON'
  | 'WITNESS'
  | 'TRANSPORTER'
  | 'HEALTHCARE_PROFESSIONAL'
  | 'DOCUMENT'
  | 'SYSTEM'
  | 'OTHER';

export interface PatientReconciliationQueueItem {
  patientId: string;
  temporaryPatientNumber: string;
  displayName: string;
  identityStatus: PatientIdentityStatus;
  confidenceLevel: IdentityConfidenceLevel;
  apparentGender: string | null;
  estimatedAgeRange: string | null;
  foundAt: string | null;
  foundLocation: string | null;
  createdAt: string;
  canonicalPatientId: string | null;
  decisionEventId: string | null;
  decision: PatientReconciliationEventDecision | null;
  terminal: boolean;
}

export interface PatientReconciliationCandidate {
  patientId: string;
  globalPatientNumber: string;
  localPatientNumber: string;
  displayName: string;
  gender: string | null;
  birthDate: string | null;
  phone: string | null;
  city: string | null;
  score: number;
  reasons: string[];
}

export interface PatientReconciliationDecisionDto {
  decision: PatientReconciliationDecision;
  candidatePatientId: string | null;
  evidenceSourceType: IdentitySourceType;
  evidenceReference: string | null;
  justification: string;
}

export interface PatientReconciliationCorrectionDto {
  correctedEventId: string;
  replacementCanonicalPatientId: string | null;
  evidenceSourceType: IdentitySourceType;
  evidenceReference: string | null;
  justification: string;
}

export interface PatientReconciliationDecisionResult {
  eventId: string;
  decision: PatientReconciliationEventDecision;
  sourcePatientId: string;
  sourceIdentityStatus: PatientIdentityStatus;
  canonicalPatientId: string;
  temporaryPatientNumber: string;
  contributingPatientIds: string[];
  decidedAt: string;
  replayed: boolean;
}

export interface PatientReconciliationEvent {
  eventId: string;
  decision: PatientReconciliationEventDecision;
  sourcePatientId: string;
  candidatePatientId: string | null;
  previousIdentityStatus: PatientIdentityStatus;
  resultingIdentityStatus: PatientIdentityStatus;
  similarityScore: number | null;
  matchReasons: string[];
  evidenceSourceType: IdentitySourceType;
  evidenceReference: string | null;
  justification: string;
  correctedEventId: string | null;
  createdByUserId: string;
  createdAt: string;
}
