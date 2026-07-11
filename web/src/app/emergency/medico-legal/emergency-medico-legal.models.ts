export type EmergencyThirdPartyQuality =
  | 'ACCOMPANYING_PERSON'
  | 'DECLARANT'
  | 'WITNESS'
  | 'TRANSPORTER'
  | 'PRESUMED_REPRESENTATIVE'
  | 'GUARANTOR'
  | 'POLICE_OR_AUTHORITY'
  | 'OTHER';

export type EmergencyInformationSourceType =
  | 'PATIENT'
  | 'ACCOMPANYING_PERSON'
  | 'FAMILY'
  | 'WITNESS'
  | 'TRANSPORTER'
  | 'POLICE_OR_AUTHORITY'
  | 'ID_DOCUMENT'
  | 'HEALTH_PROFESSIONAL'
  | 'OTHER';

export type IdentityConfidenceLevel = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERIFIED';
export type EmergencyCapacityStatus = 'INCAPABLE' | 'CAPABLE';
export type EmergencyLegalBasisType =
  | 'VITAL_EMERGENCY'
  | 'PRESUMED_CONSENT'
  | 'LEGAL_REPRESENTATIVE_CONSENT'
  | 'PUBLIC_SAFETY'
  | 'COURT_OR_AUTHORITY_ORDER'
  | 'OTHER';
export type EmergencyBelongingStatus = 'IN_CUSTODY' | 'TRANSFERRED' | 'RELEASED' | 'DISPOSED';
export type EmergencyBelongingTransferAction =
  | 'DEPOSITED'
  | 'SEALED'
  | 'TRANSFERRED'
  | 'RELEASED'
  | 'RETURNED'
  | 'DISPOSED';

export interface EmergencyIdentityStatementRequest {
  fieldName: string;
  value: string;
  confidenceLevel: IdentityConfidenceLevel;
  proofReference?: string;
  declaredAt?: string;
}

export interface CreateEmergencyThirdPartyRequest {
  fullName: string;
  phone?: string;
  email?: string;
  idDocument?: string;
  relationshipToPatient?: string;
  circumstances?: string;
  consentToContact: boolean;
  legalRepresentativeClaimed: boolean;
  sourceType: EmergencyInformationSourceType;
  confidenceLevel: IdentityConfidenceLevel;
  proofReference?: string;
  qualities: EmergencyThirdPartyQuality[];
  identityStatements: EmergencyIdentityStatementRequest[];
}

export interface RecordEmergencyCapacityRequest {
  status: EmergencyCapacityStatus;
  consciousnessLevel?: string;
  clinicalReason: string;
  effectiveAt?: string;
}

export interface CreateEmergencyLegalBasisRequest {
  basisType: EmergencyLegalBasisType;
  justification: string;
  startsAt?: string;
  expiresAt?: string;
  coveredActs: string[];
}

export interface CreateEmergencyBelongingRequest {
  category: string;
  description: string;
  quantity: number;
  itemCondition?: string;
  sealNumber?: string;
  depositedByName?: string;
}

export interface TransferEmergencyBelongingRequest {
  action: EmergencyBelongingTransferAction;
  fromCustodian?: string;
  recipientName?: string;
  recipientIdDocument?: string;
  notes?: string;
  occurredAt?: string;
}

export interface EmergencyThirdParty {
  id: string;
  fullName: string;
  phone?: string;
  email?: string;
  idDocument?: string;
  relationshipToPatient?: string;
  circumstances?: string;
  consentToContact: boolean;
  legalRepresentativeClaimed: boolean;
  sourceType: EmergencyInformationSourceType;
  confidenceLevel: IdentityConfidenceLevel;
  proofReference?: string;
  qualities: EmergencyThirdPartyQuality[];
  createdByUserId?: string;
  createdAt: string;
}

export interface EmergencyIdentityStatement {
  id: string;
  thirdPartyId?: string;
  fieldName: string;
  value: string;
  confidenceLevel: IdentityConfidenceLevel;
  proofReference?: string;
  declaredAt: string;
  createdByUserId?: string;
}

export interface EmergencyCapacityEvent {
  id: string;
  status: EmergencyCapacityStatus;
  consciousnessLevel?: string;
  clinicalReason: string;
  effectiveAt: string;
  recordedByUserId?: string;
}

export interface EmergencyLegalBasis {
  id: string;
  basisType: EmergencyLegalBasisType;
  justification: string;
  startsAt: string;
  expiresAt?: string;
  closedAt?: string;
  closureReason?: string;
  coveredActs: string[];
  active: boolean;
  createdByUserId?: string;
}

export interface EmergencyBelongingTransfer {
  id: string;
  action: EmergencyBelongingTransferAction;
  fromCustodian?: string;
  recipientName?: string;
  recipientIdDocument?: string;
  notes?: string;
  occurredAt: string;
  performedByUserId?: string;
}

export interface EmergencyBelonging {
  id: string;
  category: string;
  description: string;
  quantity: number;
  itemCondition?: string;
  sealNumber?: string;
  custodyStatus: EmergencyBelongingStatus;
  depositedByName?: string;
  receivedByUserId?: string;
  createdAt: string;
  transfers: EmergencyBelongingTransfer[];
}

export interface EmergencyMedicoLegalDossier {
  emergencyId: string;
  thirdParties: EmergencyThirdParty[];
  identityStatements: EmergencyIdentityStatement[];
  capacityHistory: EmergencyCapacityEvent[];
  currentCapacity?: EmergencyCapacityEvent;
  legalBases: EmergencyLegalBasis[];
  activeLegalBasis?: EmergencyLegalBasis;
  belongings: EmergencyBelonging[];
}
