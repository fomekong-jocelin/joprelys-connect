import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateLabOrderRequest, LabOrder, LabResult, LabResultUploadRequest } from './lab.models';

@Injectable({
  providedIn: 'root',
})
export class LabOrderApiService {
  private readonly http = inject(HttpClient);

  create(request: CreateLabOrderRequest): Observable<LabOrder> {
    return this.http.post<LabOrder>('/api/lab-orders', request);
  }

  getLabOrders(): Observable<LabOrder[]> {
    return this.http.get<LabOrder[]>('/api/lab-orders');
  }

  getPatientLabOrders(patientId: string): Observable<LabOrder[]> {
    return this.http.get<LabOrder[]>(`/api/lab-orders/patient/${patientId}`);
  }

  getLabOrder(id: string): Observable<LabOrder> {
    return this.http.get<LabOrder>(`/api/lab-orders/${id}`);
  }

  updateStatus(id: string, status: string): Observable<LabOrder> {
    return this.http.patch<LabOrder>(`/api/lab-orders/${id}/status`, { status });
  }

  getPatientResults(patientId: string): Observable<LabResult[]> {
    return this.http.get<LabResult[]>(`/api/lab-orders/patient/${patientId}/results`);
  }

  uploadResults(request: LabResultUploadRequest, apiKey: string): Observable<void> {
    return this.http.post<void>('/api/public/lab-integration/upload', request, {
      headers: { 'X-API-KEY': apiKey },
    });
  }
}
