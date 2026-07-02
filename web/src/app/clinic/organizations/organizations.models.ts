export interface Organization {
  id: string;
  name: string;
  email: string;
  phone?: string;
  address?: string;
  city: string;
  logoPath?: string;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  email: string;
  phone?: string;
  address?: string;
  city: string;
}

export interface CreateClinicAdminRequest {
  readonly displayName: string;
  readonly email: string;
}

export interface CreateClinicAdminResponse {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly role: string;
  readonly enabled: boolean;
  readonly temporaryPassword: string;
  readonly organizationId: string;
  readonly createdAt: string;
}
