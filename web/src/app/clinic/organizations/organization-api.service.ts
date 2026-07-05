import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateClinicAdminRequest,
  CreateClinicAdminResponse,
  CreateOrganizationRequest,
  Organization,
  ApiKey,
  CreateApiKeyRequest
} from './organizations.models';

@Injectable({
  providedIn: 'root',
})
export class OrganizationApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<Organization[]> {
    return this.http.get<Organization[]>('/api/organizations');
  }

  create(request: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>('/api/organizations', request);
  }

  updateStatus(id: string, status: 'ACTIVE' | 'INACTIVE'): Observable<Organization> {
    return this.http.put<Organization>(`/api/organizations/${id}/status`, `"${status}"`, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  createClinicAdmin(organizationId: string, request: CreateClinicAdminRequest): Observable<CreateClinicAdminResponse> {
    return this.http.post<CreateClinicAdminResponse>(`/api/organizations/${organizationId}/admin`, request);
  }

  update(id: string, request: CreateOrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`/api/organizations/${id}`, request);
  }

  listApiKeys(organizationId: string): Observable<ApiKey[]> {
    return this.http.get<ApiKey[]>(`/api/organizations/${organizationId}/api-keys`);
  }

  generateApiKey(organizationId: string, request: CreateApiKeyRequest): Observable<ApiKey> {
    return this.http.post<ApiKey>(`/api/organizations/${organizationId}/api-keys`, request);
  }

  revokeApiKey(organizationId: string, apiKeyId: string): Observable<void> {
    return this.http.delete<void>(`/api/organizations/${organizationId}/api-keys/${apiKeyId}`);
  }
}
