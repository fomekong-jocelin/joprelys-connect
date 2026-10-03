import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActiveVisitScope, CreateVisitRequest, Visit, VitalMeasurement, Vitals } from './visit.models';

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

  getActiveVisits(scope: ActiveVisitScope = 'ALL'): Observable<Visit[]> {
    return this.http.get<Visit[]>('/api/visits/active', { params: { scope } });
  }

  /** Prend le patient en consultation ; `takeOver` reprend explicitement le patient d'un confrère. */
  takeCharge(id: string, takeOver = false): Observable<Visit> {
    return this.http.post<Visit>(`/api/visits/${id}/take-charge`, {}, { params: { takeOver } });
  }

  /** Remet le patient dans la file sans clôturer la visite. */
  releaseCharge(id: string): Observable<Visit> {
    return this.http.post<Visit>(`/api/visits/${id}/release`, {});
  }

  getVitalsHistory(id: string): Observable<VitalMeasurement[]> {
    return this.http.get<VitalMeasurement[]>(`/api/visits/${id}/vitals/history`);
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
