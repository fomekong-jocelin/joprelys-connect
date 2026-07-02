import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateLabOrderRequest, LabOrder } from './lab.models';

@Injectable({
  providedIn: 'root',
})
export class LabOrderApiService {
  private readonly http = inject(HttpClient);

  create(request: CreateLabOrderRequest): Observable<LabOrder> {
    return this.http.post<LabOrder>('/api/lab-orders', request);
  }

  getPatientLabOrders(patientId: string): Observable<LabOrder[]> {
    return this.http.get<LabOrder[]>(`/api/lab-orders/patient/${patientId}`);
  }

  getLabOrder(id: string): Observable<LabOrder> {
    return this.http.get<LabOrder>(`/api/lab-orders/${id}`);
  }
}
