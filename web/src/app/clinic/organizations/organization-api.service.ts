import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateOrganizationRequest, Organization } from './organizations.models';

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
}
