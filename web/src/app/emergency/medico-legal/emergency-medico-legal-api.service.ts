import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateEmergencyBelongingRequest,
  CreateEmergencyLegalBasisRequest,
  CreateEmergencyThirdPartyRequest,
  EmergencyMedicoLegalDossier,
  RecordEmergencyCapacityRequest,
  TransferEmergencyBelongingRequest,
} from './emergency-medico-legal.models';

@Injectable({ providedIn: 'root' })
export class EmergencyMedicoLegalApiService {
  private readonly http = inject(HttpClient);

  getDossier(emergencyId: string): Observable<EmergencyMedicoLegalDossier> {
    return this.http.get<EmergencyMedicoLegalDossier>(this.baseUrl(emergencyId));
  }

  addThirdParty(
    emergencyId: string,
    request: CreateEmergencyThirdPartyRequest,
  ): Observable<EmergencyMedicoLegalDossier> {
    return this.http.post<EmergencyMedicoLegalDossier>(
      `${this.baseUrl(emergencyId)}/third-parties`,
      request,
    );
  }

  recordCapacity(
    emergencyId: string,
    request: RecordEmergencyCapacityRequest,
  ): Observable<EmergencyMedicoLegalDossier> {
    return this.http.post<EmergencyMedicoLegalDossier>(
      `${this.baseUrl(emergencyId)}/capacity-events`,
      request,
    );
  }

  addLegalBasis(
    emergencyId: string,
    request: CreateEmergencyLegalBasisRequest,
  ): Observable<EmergencyMedicoLegalDossier> {
    return this.http.post<EmergencyMedicoLegalDossier>(
      `${this.baseUrl(emergencyId)}/legal-bases`,
      request,
    );
  }

  addBelonging(
    emergencyId: string,
    request: CreateEmergencyBelongingRequest,
  ): Observable<EmergencyMedicoLegalDossier> {
    return this.http.post<EmergencyMedicoLegalDossier>(
      `${this.baseUrl(emergencyId)}/belongings`,
      request,
    );
  }

  transferBelonging(
    emergencyId: string,
    belongingId: string,
    request: TransferEmergencyBelongingRequest,
  ): Observable<EmergencyMedicoLegalDossier> {
    return this.http.post<EmergencyMedicoLegalDossier>(
      `${this.baseUrl(emergencyId)}/belongings/${belongingId}/transfers`,
      request,
    );
  }

  private baseUrl(emergencyId: string): string {
    return `/api/emergencies/${emergencyId}/medico-legal`;
  }
}
