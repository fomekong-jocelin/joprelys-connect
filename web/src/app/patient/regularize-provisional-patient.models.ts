export type IdentitySourceType =
  | 'PATIENT'
  | 'ACCOMPANYING_PERSON'
  | 'WITNESS'
  | 'TRANSPORTER'
  | 'HEALTHCARE_PROFESSIONAL'
  | 'DOCUMENT'
  | 'SYSTEM'
  | 'OTHER';

export interface RegularizeProvisionalPatientDto {
  fullName: string;
  gender: string;
  birthDate: string;
  phone?: string;
  city: string;
  district?: string;
  address?: string;
  email?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  sourceType: IdentitySourceType;
  sourceDetails: string;
  reason: string;
}
