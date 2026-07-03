import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthTokenStorageService } from '../../../auth/auth-token-storage.service';
import { LoginResponse } from '../../../auth/auth.models';

export interface RequestOtpPayload {
  globalPatientNumber: string;
  phone: string;
  birthDate: string;
}

export interface VerifyOtpPayload {
  globalPatientNumber: string;
  otpCode: string;
}

import { Vitals } from '../../../visit/visit.models';
import { PrescriptionItem } from '../../../consultation/consultation.models';

export interface PatientPortalConsultation {
  visitId: string;
  visitNumber: string;
  visitDate: string;
  doctorName: string;
  clinicName: string;
  diagnosis: string;
  documentId: string | null;
  documentStatus: string | null;
  symptoms?: string;
  clinicalExam?: string;
  advice?: string;
  followUp?: string;
  vitals?: Vitals;
  prescriptionItems?: PrescriptionItem[];
}

export interface PatientPortalMeResponse {
  id: string;
  globalPatientNumber: string;
  localPatientNumber: string;
  fullName: string;
  gender: string;
  birthDate: string;
  phone: string;
  city: string;
  district: string;
  address: string;
  emergencyContactName: string;
  emergencyContactPhone: string;
  allergies: string;
  medicalHistory: string;
  consultations: PatientPortalConsultation[];
}

@Injectable({
  providedIn: 'root'
})
export class PatientPortalService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(AuthTokenStorageService);

  requestOtp(payload: RequestOtpPayload): Observable<void> {
    return this.http.post<void>('/api/public/patient/auth/otp', payload);
  }

  verifyOtp(payload: VerifyOtpPayload): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/public/patient/auth/verify', payload).pipe(
      tap((res) => {
        this.tokenStorage.save(res);
      })
    );
  }

  getMe(): Observable<PatientPortalMeResponse> {
    return this.http.get<PatientPortalMeResponse>('/api/patient/me');
  }

  downloadDocument(visitId: string): Observable<Blob> {
    return this.http.get(`/api/patient/visits/${visitId}/document`, {
      responseType: 'blob'
    });
  }

  getConsents(): Observable<PatientConsent[]> {
    return this.http.get<PatientConsent[]>('/api/patient/consents');
  }

  updateConsent(orgId: string, status: string): Observable<void> {
    return this.http.post<void>(`/api/patient/consents/${orgId}?status=${status}`, {});
  }

  getAuditLogs(): Observable<PatientAuditLog[]> {
    return this.http.get<PatientAuditLog[]>('/api/patient/audit-logs');
  }

  getAccessRequests(): Observable<ExternalAccessResponse[]> {
    return this.http.get<ExternalAccessResponse[]>('/api/patient/access-requests');
  }

  approveAccessRequest(id: string): Observable<ExternalAccessResponse> {
    return this.http.post<ExternalAccessResponse>(`/api/patient/access-requests/${id}/approve`, {});
  }

  rejectAccessRequest(id: string): Observable<ExternalAccessResponse> {
    return this.http.post<ExternalAccessResponse>(`/api/patient/access-requests/${id}/reject`, {});
  }
}

export interface PatientConsent {
  organizationId: string;
  organizationName: string;
  status: string;
  isCreator: boolean;
}

export interface PatientAuditLog {
  id: string;
  action: string;
  reason: string;
  ipAddress: string | null;
  userAgent: string | null;
  status: string;
  createdAt: string;
  organizationName: string;
}

export interface ExternalAccessResponse {
  id: string;
  patientId: string;
  requesterUserId: string;
  requesterOrganizationId: string;
  requesterOrganizationName: string;
  reason: string;
  durationHours: number;
  status: string;
  createdAt: string;
  expiresAt: string | null;
}
