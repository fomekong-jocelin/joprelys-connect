import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateExternalAccessRequestDto {
  patientDpu: string;
  reason: string;
  durationHours: number;
  scopes?: string;
}

export interface ExternalAccessRequestResponse {
  id: string;
  patientId: string;
  requesterUserId: string;
  requesterOrganizationId: string;
  requesterOrganizationName: string;
  reason: string;
  durationHours: number;
  status: string;
  scopes?: string;
  createdAt: string;
  expiresAt: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class ExternalAccessApiService {
  private readonly http = inject(HttpClient);

  createRequest(dto: CreateExternalAccessRequestDto): Observable<ExternalAccessRequestResponse> {
    return this.http.post<ExternalAccessRequestResponse>('/api/external-access/requests', dto);
  }
}
