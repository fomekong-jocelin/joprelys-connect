import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  PharmacyDispensationHistoryEntry,
  PharmacyDispenseRequest,
  PharmacyVerifyRequest,
  PharmacyVerifyResponse,
  DrugStockResponse,
  CreateDrugStockRequest,
} from './pharmacy.models';

@Injectable({
  providedIn: 'root',
})
export class PharmacyApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/public/pharmacy/prescriptions';
  private readonly stocksUrl = '/api/pharmacy/stocks';

  verifyPrescription(request: PharmacyVerifyRequest): Observable<PharmacyVerifyResponse> {
    return this.http.post<PharmacyVerifyResponse>(`${this.baseUrl}/verify`, request);
  }

  dispensePrescription(request: PharmacyDispenseRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/dispense`, request);
  }

  getDispensationHistory(request: PharmacyVerifyRequest): Observable<PharmacyDispensationHistoryEntry[]> {
    return this.http.post<PharmacyDispensationHistoryEntry[]>(`${this.baseUrl}/history`, request);
  }

  getStocks(): Observable<DrugStockResponse[]> {
    return this.http.get<DrugStockResponse[]>(this.stocksUrl);
  }

  createOrUpdateStock(request: CreateDrugStockRequest): Observable<DrugStockResponse> {
    return this.http.post<DrugStockResponse>(this.stocksUrl, request);
  }

  getLowStockAlerts(): Observable<DrugStockResponse[]> {
    return this.http.get<DrugStockResponse[]>(`${this.stocksUrl}/alerts`);
  }
}
