export type StaffRole =
  | 'ADMIN_CLINIQUE'
  | 'DAF'
  | 'SECRETAIRE_COMPTABLE'
  | 'CAISSIER'
  | 'AUDITEUR'
  | 'MEDECIN'
  | 'INFIRMIER'
  | 'AGENT_ACCUEIL'
  | 'PHARMACIEN'
  | 'BIOLOGISTE';

export type StaffRoleCategory = 'GOVERNANCE' | 'FINANCE' | 'CLINICAL' | 'OPERATIONS';

export interface StaffRoleDefinition {
  readonly code: StaffRole;
  readonly labelKey: string;
  readonly descriptionKey: string;
  readonly category: StaffRoleCategory;
  readonly sensitive: boolean;
}

export interface StaffMember {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly role: string;
  readonly enabled: boolean;
  readonly createdAt: string;
  readonly photoPath?: string;
  readonly signaturePath?: string;
  readonly stampPath?: string;
  readonly phone?: string;
  readonly specialty?: string;
  readonly registrationNumber?: string;
  readonly department?: string;
  readonly bio?: string;
}

export interface InviteStaffRequest {
  readonly email: string;
  readonly displayName: string;
  readonly role: string;
}

export interface UpdateStaffRequest {
  readonly displayName: string;
  readonly role: string;
  readonly photoPath?: string;
  readonly signaturePath?: string;
  readonly stampPath?: string;
  readonly phone?: string;
  readonly specialty?: string;
  readonly registrationNumber?: string;
  readonly department?: string;
  readonly bio?: string;
}

export interface InviteStaffResponse extends StaffMember {
  readonly temporaryPassword: string;
}
