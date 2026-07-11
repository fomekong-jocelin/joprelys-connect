import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AddResuscitationLogRequest,
  CreateEmergencyRequest,
  CreateProvisionalEmergencyAdmissionRequest,
  EmergencyRecord,
  ResuscitationLog,
} from './emergency.models';

@Injectable({ providedIn: 'root' })
export class EmergencyApiService {
  private readonly http = inject(HttpClient);

  create(dto: CreateEmergencyRequest): Observable<EmergencyRecord> {
    return this.http.post<EmergencyRecord>('/api/emergencies', dto);
  }

  createProvisionalAdmission(dto: CreateProvisionalEmergencyAdmissionRequest): Observable<EmergencyRecord> {
    return this.http.post<EmergencyRecord>('/api/emergencies/provisional', dto);
  }

  getActive(): Observable<EmergencyRecord[]> {
    return this.http.get<EmergencyRecord[]>('/api/emergencies/active');
  }

  getById(id: string): Observable<EmergencyRecord> {
    return this.http.get<EmergencyRecord>(`/api/emergencies/${id}`);
  }

  addResuscitationLog(id: string, dto: AddResuscitationLogRequest): Observable<ResuscitationLog> {
    return this.http.post<ResuscitationLog>(`/api/emergencies/${id}/resuscitation`, dto);
  }

  stabilize(id: string, orientation: string): Observable<EmergencyRecord> {
    return this.http.post<EmergencyRecord>(
      `/api/emergencies/${id}/stabilize?orientation=${encodeURIComponent(orientation)}`,
      {},
    );
  }

  getPatientEmergencies(patientId: string): Observable<EmergencyRecord[]> {
    return this.http.get<EmergencyRecord[]>(`/api/emergencies/patient/${patientId}`);
  }
}
