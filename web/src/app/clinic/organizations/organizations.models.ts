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
  adminEmail?: string;
  adminDisplayName?: string;
  country: string;
  type: string;
  responsibleName: string;
  apiEnabled: boolean;
}

export interface CreateOrganizationRequest {
  name: string;
  email: string;
  phone?: string;
  address?: string;
  city: string;
  country: string;
  type: string;
  responsibleName: string;
  apiEnabled: boolean;
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

export interface ApiKey {
  id: string;
  name: string;
  prefix: string;
  rawKey?: string;
  status: 'ACTIVE' | 'REVOKED';
  createdAt: string;
  revokedAt?: string;
}

export interface CreateApiKeyRequest {
  name: string;
}
