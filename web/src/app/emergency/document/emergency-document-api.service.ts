import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type EmergencyDocumentType =
  | 'FICHE_URGENCE'
  | 'FEUILLE_REANIMATION'
  | 'CONSTAT_INCAPACITE_URGENCE'
  | 'FICHE_TIERS_URGENCE'
  | 'INVENTAIRE_EFFETS_URGENCE'
  | 'FICHE_HOSPITALISATION';

export interface EmergencyDocument {
  readonly id: string;
  readonly visitId: string;
  readonly originPatientId: string;
  readonly documentNumber: string;
  readonly documentType: EmergencyDocumentType;
  readonly status: string;
  readonly hash: string;
  readonly version: number;
  readonly verificationUrl: string;
  readonly createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class EmergencyDocumentApiService {
  private readonly http = inject(HttpClient);

  generateBundle(emergencyId: string): Observable<EmergencyDocument[]> {
    return this.http.post<EmergencyDocument[]>(
      `/api/emergencies/${emergencyId}/documents`,
      {},
    );
  }

  listByEmergency(emergencyId: string): Observable<EmergencyDocument[]> {
    return this.http.get<EmergencyDocument[]>(`/api/emergencies/${emergencyId}/documents`);
  }

  listByCanonicalPatient(patientId: string): Observable<EmergencyDocument[]> {
    return this.http.get<EmergencyDocument[]>(`/api/patients/${patientId}/documents`);
  }
}
