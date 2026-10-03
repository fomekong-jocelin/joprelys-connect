import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateVisitRequest, Visit, Vitals } from './visit.models';

@Injectable({
  providedIn: 'root',
})
export class VisitApiService {
  private readonly http = inject(HttpClient);

  create(dto: CreateVisitRequest): Observable<Visit> {
    return this.http.post<Visit>('/api/visits', dto);
  }

  getById(id: string): Observable<Visit> {
    return this.http.get<Visit>(`/api/visits/${id}`);
  }

  getActiveVisits(): Observable<Visit[]> {
    return this.http.get<Visit[]>('/api/visits/active');
  }

  closeVisit(id: string): Observable<Visit> {
    return this.http.post<Visit>(`/api/visits/${id}/close`, {});
  }

  cancelVisit(id: string): Observable<Visit> {
    return this.http.post<Visit>(`/api/visits/${id}/cancel`, {});
  }

  saveVitals(id: string, vitals: Vitals): Observable<Vitals> {
    return this.http.post<Vitals>(`/api/visits/${id}/vitals`, vitals);
  }

  getVitals(id: string): Observable<Vitals> {
    return this.http.get<Vitals>(`/api/visits/${id}/vitals`);
  }

  getPatientVisits(patientId: string): Observable<Visit[]> {
    return this.http.get<Visit[]>(`/api/visits/patient/${patientId}`);
  }
}
