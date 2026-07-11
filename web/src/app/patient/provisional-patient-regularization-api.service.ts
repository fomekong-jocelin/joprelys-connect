import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Patient } from './patient.models';
import { RegularizeProvisionalPatientDto } from './regularize-provisional-patient.models';

@Injectable({ providedIn: 'root' })
export class ProvisionalPatientRegularizationApiService {
  private readonly http = inject(HttpClient);

  regularize(patientId: string, dto: RegularizeProvisionalPatientDto): Observable<Patient> {
    return this.http.put<Patient>(`/api/patients/provisional/${patientId}/identity`, dto);
  }
}
