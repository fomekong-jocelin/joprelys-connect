export type StaffRole = string;

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
  readonly specialty?: string;
  readonly registrationNumber?: string;
  readonly department?: string;
  readonly bio?: string;
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
  readonly specialty?: string;
  readonly registrationNumber?: string;
  readonly department?: string;
  readonly bio?: string;
}

export type InviteStaffResponse = StaffMember;
