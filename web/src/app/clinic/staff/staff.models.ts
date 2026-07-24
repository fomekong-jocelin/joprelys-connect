export type StaffRole = string;

export interface StaffActiveOrganizationalUnit {
  readonly id: string;
  readonly code: string;
  readonly nameFr: string;
  readonly nameEn: string;
  readonly primary: boolean;
}

export interface StaffMember {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly role: StaffRole;
  readonly enabled: boolean;
  readonly createdAt: string;
  readonly photoPath?: string;
  readonly signaturePath?: string;
  readonly stampPath?: string;
  readonly phone?: string;
  readonly registrationNumber?: string;
  readonly bio?: string;
  readonly activeOrganizationalUnits?: readonly StaffActiveOrganizationalUnit[];
  /**
   * @deprecated Snapshot de présentation dérivé de l'unité principale active.
   * Jamais persisté dans users et jamais envoyé dans un payload d'écriture.
   * Conservé uniquement pour le formulaire historique de visite, dont le modèle
   * `visits.service_name` reste textuel et relève d'un cutover séparé.
   */
  readonly department?: string;
}

export interface InviteStaffRequest {
  readonly email: string;
  readonly displayName: string;
  readonly roles: readonly StaffRole[];
}

export interface UpdateStaffRequest {
  readonly displayName: string;
  readonly roles: readonly StaffRole[];
  readonly photoPath?: string;
  readonly signaturePath?: string;
  readonly stampPath?: string;
  readonly phone?: string;
  readonly registrationNumber?: string;
  readonly bio?: string;
}

export interface StaffAssignmentRole {
  readonly code: string;
  readonly nameFr: string;
  readonly nameEn: string;
}

export interface StaffSpecialtyAssignment {
  readonly id: string;
  readonly specialtyCode: string;
  readonly primary: boolean;
  readonly validFrom: string;
  readonly validTo?: string;
  readonly active: boolean;
}

export interface StaffUnitAssignment {
  readonly id: string;
  readonly organizationalUnitId: string;
  readonly assignmentRoleCode: string;
  readonly primary: boolean;
  readonly validFrom: string;
  readonly validTo?: string;
  readonly active: boolean;
}

export interface StaffAssignmentStructure {
  readonly specialties: readonly StaffSpecialtyAssignment[];
  readonly unitAssignments: readonly StaffUnitAssignment[];
}

export interface StaffSpecialtyAssignmentRequest {
  readonly specialtyCode: string;
  readonly primary: boolean;
  readonly validFrom: string;
  readonly validTo?: string;
}

export interface StaffUnitAssignmentRequest {
  readonly organizationalUnitId: string;
  readonly assignmentRoleCode: string;
  readonly primary: boolean;
  readonly validFrom: string;
  readonly validTo?: string;
}

export interface CloseStaffAssignmentRequest {
  readonly closedAt: string;
}

export type InviteStaffResponse = StaffMember;
