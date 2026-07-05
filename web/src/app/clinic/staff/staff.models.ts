export type StaffRole = string;

export interface StaffMember {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly role: StaffRole;
  readonly enabled: boolean;
  readonly createdAt: string;
}

export interface InviteStaffRequest {
  readonly email: string;
  readonly displayName: string;
  readonly role: StaffRole;
}

export interface UpdateStaffRequest {
  readonly displayName: string;
  readonly role: StaffRole;
}

export interface InviteStaffResponse extends StaffMember {
  readonly temporaryPassword: string;
}
