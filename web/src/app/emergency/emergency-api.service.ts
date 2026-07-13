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
import {
  CreateEmergencyTriageAssessmentRequest,
  EmergencyTriageAssessment,
} from './triage/emergency-triage.models';

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

  getTriageAssessments(id: string): Observable<EmergencyTriageAssessment[]> {
    return this.http.get<EmergencyTriageAssessment[]>(
      `/api/emergencies/${id}/triage-assessments`,
    );
  }

  addTriageAssessment(
    id: string,
    dto: CreateEmergencyTriageAssessmentRequest,
  ): Observable<EmergencyTriageAssessment> {
    return this.http.post<EmergencyTriageAssessment>(
      `/api/emergencies/${id}/triage-assessments`,
      dto,
    );
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