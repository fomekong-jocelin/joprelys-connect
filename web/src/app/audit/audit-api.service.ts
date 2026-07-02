import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLog } from './audit.models';

@Injectable({
  providedIn: 'root',
})
export class AuditApiService {
  private readonly http = inject(HttpClient);

  getPatientLogs(patientId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`/api/audit/patients/${patientId}`);
  }

  getOrganizationLogs(organizationId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`/api/audit/organizations/${organizationId}`);
  }
}
