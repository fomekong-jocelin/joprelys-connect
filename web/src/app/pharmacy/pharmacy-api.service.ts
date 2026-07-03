import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  PharmacyDispensationHistoryEntry,
  PharmacyDispenseRequest,
  PharmacyVerifyRequest,
  PharmacyVerifyResponse,
} from './pharmacy.models';

@Injectable({
  providedIn: 'root',
})
export class PharmacyApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/public/pharmacy/prescriptions';

  verifyPrescription(request: PharmacyVerifyRequest): Observable<PharmacyVerifyResponse> {
    return this.http.post<PharmacyVerifyResponse>(`${this.baseUrl}/verify`, request);
  }

  dispensePrescription(request: PharmacyDispenseRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/dispense`, request);
  }

  getDispensationHistory(request: PharmacyVerifyRequest): Observable<PharmacyDispensationHistoryEntry[]> {
    return this.http.post<PharmacyDispensationHistoryEntry[]>(`${this.baseUrl}/history`, request);
  }
}
