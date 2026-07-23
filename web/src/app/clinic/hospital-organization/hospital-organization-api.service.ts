import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  HospitalServiceCatalogEntry,
  MedicalSpecialtyCatalogEntry,
  OrganizationalUnit,
  SaveOrganizationalUnitPayload,
} from './hospital-organization.models';

@Injectable({ providedIn: 'root' })
export class HospitalOrganizationApiService {
  private readonly http = inject(HttpClient);

  listServiceCatalog(): Observable<HospitalServiceCatalogEntry[]> {
    return this.http.get<HospitalServiceCatalogEntry[]>('/api/hospital-organization/catalogs/services');
  }

  listSpecialtyCatalog(): Observable<MedicalSpecialtyCatalogEntry[]> {
    return this.http.get<MedicalSpecialtyCatalogEntry[]>('/api/hospital-organization/catalogs/specialties');
  }

  listUnits(organizationId?: string, includeInactive = false): Observable<OrganizationalUnit[]> {
    let params = this.scopeParams(organizationId);
    params = params.set('includeInactive', String(includeInactive));
    return this.http.get<OrganizationalUnit[]>('/api/hospital-organization/units', { params });
  }

  createUnit(payload: SaveOrganizationalUnitPayload, organizationId?: string): Observable<OrganizationalUnit> {
    return this.http.post<OrganizationalUnit>('/api/hospital-organization/units', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateUnit(
    id: string,
    payload: SaveOrganizationalUnitPayload,
    organizationId?: string,
  ): Observable<OrganizationalUnit> {
    return this.http.put<OrganizationalUnit>(`/api/hospital-organization/units/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  setActive(id: string, active: boolean, organizationId?: string): Observable<OrganizationalUnit> {
    const action = active ? 'activate' : 'deactivate';
    return this.http.post<OrganizationalUnit>(`/api/hospital-organization/units/${id}/${action}`, null, {
      params: this.scopeParams(organizationId),
    });
  }

  private scopeParams(organizationId?: string): HttpParams {
    return organizationId ? new HttpParams().set('organizationId', organizationId) : new HttpParams();
  }
}
