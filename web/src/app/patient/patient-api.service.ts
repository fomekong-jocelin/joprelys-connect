import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreatePatientDto, Patient, LabOrder, LabResult } from './patient.models';

@Injectable({
  providedIn: 'root',
})
export class PatientApiService {
  private readonly http = inject(HttpClient);

  list(query?: string): Observable<Patient[]> {
    const url = query ? `/api/patients?q=${encodeURIComponent(query)}` : '/api/patients';
    return this.http.get<Patient[]>(url);
  }

  getById(id: string): Observable<Patient> {
    return this.http.get<Patient>(`/api/patients/${id}`);
  }

  create(dto: CreatePatientDto): Observable<Patient> {
    return this.http.post<Patient>('/api/patients', dto);
  }

  triggerEmergencyAccess(id: string, reason: string): Observable<void> {
    return this.http.post<void>(`/api/patients/${id}/emergency-access`, { reason });
  }

  getPatientLabOrders(patientId: string): Observable<LabOrder[]> {
    return this.http.get<LabOrder[]>(`/api/lab-orders/patient/${patientId}`);
  }

  getPatientLabResults(patientId: string): Observable<LabResult[]> {
    return this.http.get<LabResult[]>(`/api/lab-orders/patient/${patientId}/results`);
  }
}
