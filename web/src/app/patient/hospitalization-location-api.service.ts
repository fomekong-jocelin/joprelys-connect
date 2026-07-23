import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { StructuredAdmissionRequest, StructuredHospitalization } from './hospitalization-location.models';

@Injectable({ providedIn: 'root' })
export class HospitalizationLocationApiService {
  private readonly http = inject(HttpClient);

  admit(request: StructuredAdmissionRequest): Observable<StructuredHospitalization> {
    return this.http.post<StructuredHospitalization>('/api/hospitalizations', request);
  }

  listForPatient(patientId: string): Observable<StructuredHospitalization[]> {
    return this.http.get<StructuredHospitalization[]>(`/api/hospitalizations/patient/${patientId}`);
  }

  get(id: string): Observable<StructuredHospitalization> {
    return this.http.get<StructuredHospitalization>(`/api/hospitalizations/${id}`);
  }
}
