import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  PatientReconciliationCandidate,
  PatientReconciliationCorrectionDto,
  PatientReconciliationDecisionDto,
  PatientReconciliationDecisionResult,
  PatientReconciliationEvent,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';

@Injectable({ providedIn: 'root' })
export class PatientReconciliationApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/patient-reconciliations';

  getQueue(): Observable<PatientReconciliationQueueItem[]> {
    return this.http.get<PatientReconciliationQueueItem[]>(`${this.baseUrl}/queue`);
  }

  getCandidates(patientId: string): Observable<PatientReconciliationCandidate[]> {
    return this.http.get<PatientReconciliationCandidate[]>(`${this.baseUrl}/${patientId}/candidates`);
  }

  getHistory(patientId: string): Observable<PatientReconciliationEvent[]> {
    return this.http.get<PatientReconciliationEvent[]>(`${this.baseUrl}/${patientId}/history`);
  }

  decide(
    patientId: string,
    dto: PatientReconciliationDecisionDto,
    idempotencyKey: string,
  ): Observable<PatientReconciliationDecisionResult> {
    return this.http.post<PatientReconciliationDecisionResult>(
      `${this.baseUrl}/${patientId}/decisions`,
      dto,
      this.idempotencyOptions(idempotencyKey),
    );
  }

  correct(
    patientId: string,
    dto: PatientReconciliationCorrectionDto,
    idempotencyKey: string,
  ): Observable<PatientReconciliationDecisionResult> {
    return this.http.post<PatientReconciliationDecisionResult>(
      `${this.baseUrl}/${patientId}/corrections`,
      dto,
      this.idempotencyOptions(idempotencyKey),
    );
  }

  private idempotencyOptions(idempotencyKey: string): { headers: HttpHeaders } {
    return { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) };
  }
}
