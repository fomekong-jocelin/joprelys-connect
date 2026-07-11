import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateProvisionalPatientRequest,
  ProvisionalPatientResponse,
} from './provisional-patient.models';

@Injectable({ providedIn: 'root' })
export class ProvisionalPatientApiService {
  private readonly http = inject(HttpClient);

  create(request: CreateProvisionalPatientRequest): Observable<ProvisionalPatientResponse> {
    return this.http.post<ProvisionalPatientResponse>('/api/patients/provisional', request);
  }
}
