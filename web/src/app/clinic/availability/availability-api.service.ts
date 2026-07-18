import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AvailabilityException,
  AvailabilityRule,
  CreateAvailabilityExceptionRequest,
  UpsertAvailabilityRuleRequest,
} from './availability.models';

/**
 * Accès aux endpoints `/api/availabilities` (STORY-2602, contrat §2).
 * Sans `doctorId`, le backend cible le médecin connecté (MVP : aucun sélecteur admin).
 */
@Injectable({
  providedIn: 'root',
})
export class AvailabilityApiService {
  private readonly http = inject(HttpClient);

  /** Liste les règles récurrentes (actives ET désactivées — regrouper/filtrer côté client). */
  listRules(doctorId?: string): Observable<AvailabilityRule[]> {
    return this.http.get<AvailabilityRule[]>('/api/availabilities', { params: this.buildParams(doctorId) });
  }

  createRule(request: UpsertAvailabilityRuleRequest): Observable<AvailabilityRule> {
    return this.http.post<AvailabilityRule>('/api/availabilities', request);
  }

  updateRule(id: string, request: UpsertAvailabilityRuleRequest): Observable<AvailabilityRule> {
    return this.http.put<AvailabilityRule>(`/api/availabilities/${id}`, request);
  }

  /** Désactivation logique (`active=false`) — jamais de suppression physique (RM-07). */
  deactivateRule(id: string): Observable<AvailabilityRule> {
    return this.http.delete<AvailabilityRule>(`/api/availabilities/${id}`);
  }

  /** Liste les indisponibilités bornées par une période (Instants ISO-8601 UTC). */
  listExceptions(from: string, to: string, doctorId?: string): Observable<AvailabilityException[]> {
    const params = this.buildParams(doctorId).set('from', from).set('to', to);
    return this.http.get<AvailabilityException[]>('/api/availabilities/exceptions', { params });
  }

  createException(request: CreateAvailabilityExceptionRequest): Observable<AvailabilityException> {
    return this.http.post<AvailabilityException>('/api/availabilities/exceptions', request);
  }

  /** Suppression physique d'une indisponibilité. */
  deleteException(id: string): Observable<void> {
    return this.http.delete<void>(`/api/availabilities/exceptions/${id}`);
  }

  private buildParams(doctorId?: string): HttpParams {
    return doctorId ? new HttpParams().set('doctorId', doctorId) : new HttpParams();
  }
}
