export type IdentityConfidenceLevel = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';

export interface CreateProvisionalPatientRequest {
  apparentGender?: string;
  estimatedAgeRange?: string;
  physicalDescription?: string;
  foundAt?: string;
  foundLocation?: string;
  confidenceLevel?: IdentityConfidenceLevel;
  identityDeclarations?: ProvisionalIdentityDeclarationRequest[];
}

export interface ProvisionalIdentityDeclarationRequest {
  fieldName: string;
  value: string;
  sourceType: string;
  sourceDetails?: string;
  confidenceLevel: IdentityConfidenceLevel;
  declaredAt?: string;
}

export interface ProvisionalPatientSummary {
  id: string;
  organizationId: string;
  globalPatientNumber: string;
  localPatientNumber: string;
  fullName?: string | null;
  identityStatus: 'PROVISIONAL_URGENCY' | 'DECLARED' | 'VERIFIED' | 'MERGED';
  temporaryPatientNumber: string;
  displayName: string;
  apparentGender?: string | null;
  estimatedAgeRange?: string | null;
  physicalDescription?: string | null;
  foundAt?: string | null;
  foundLocation?: string | null;
}

export interface ProvisionalPatientResponse {
  patient: ProvisionalPatientSummary;
  identityDeclarations: unknown[];
}
