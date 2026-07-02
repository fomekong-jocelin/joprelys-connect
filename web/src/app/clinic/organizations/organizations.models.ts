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
