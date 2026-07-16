import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map } from 'rxjs';
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
  prescriptionId?: string | null;
  prescriptionNumber?: string | null;
  prescriptionStatus?: string | null;
  prescriptionTransmissionStatus?: string | null;
  prescriptionTransmittedAt?: string | null;
  prescriptionDocumentId?: string | null;
  pinCode?: string | null;
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
  bloodGroup?: string;
  email?: string;
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

  downloadDocumentById(documentId: string): Observable<Blob> {
    return this.http.get(`/api/patient/documents/${documentId}/download`, { responseType: 'blob' });
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

  // TICKET-1306: Mise à jour des scopes granulaires d'un consentement
  updateConsentScopes(orgId: string, scopes: string, validationChannel: string): Observable<void> {
    return this.http.post<void>(`/api/patient/consents/${orgId}?status=ACTIVE&scopes=${encodeURIComponent(scopes)}&validationChannel=${encodeURIComponent(validationChannel)}`, {});
  }

  // TICKET-1307: Approbation d'une demande d'accès avec scopes restreints
  approveAccessRequestWithScopes(id: string, scopes: string): Observable<ExternalAccessResponse> {
    return this.http.post<ExternalAccessResponse>(`/api/patient/access-requests/${id}/approve?scopes=${encodeURIComponent(scopes)}`, {});
  }

  getUnreadNotificationCount(): Observable<number> {
    return this.http.get<{ count: number }>('/api/notifications/unread-count').pipe(
      map(res => res.count)
    );
  }

  getNotifications(): Observable<PatientNotification[]> {
    return this.http.get<PatientNotification[]>('/api/patient/notifications');
  }

  markNotificationAsRead(id: string): Observable<PatientNotification> {
    return this.http.post<PatientNotification>(`/api/patient/notifications/${id}/read`, {});
  }

  markAllNotificationsAsRead(): Observable<void> {
    return this.http.post<void>('/api/patient/notifications/read-all', {});
  }

  transmitPrescription(prescriptionId: string): Observable<any> {
    return this.http.post<any>(`/api/patient/me/prescriptions/${prescriptionId}/transmit`, {});
  }

  getMedicalSummary(): Observable<any> {
    return this.http.get<any>('/api/patient/medical-summary');
  }

  downloadSummaryPdf(): Observable<Blob> {
    return this.http.get('/api/patient/summary-pdf', {
      responseType: 'blob'
    });
  }

  getOwnResults(): Observable<any[]> {
    return this.http.get<any[]>('/api/patient/results');
  }

  exportResults(patientId: string, format: string): Observable<Blob> {
    return this.http.get(`/api/patients/${patientId}/exam-results/export?format=${format}`, {
      responseType: 'blob'
    });
  }

  downloadResultPdf(resultId: string): Observable<Blob> {
    return this.http.get(`/api/patient/results/${resultId}/pdf`, {
      responseType: 'blob'
    });
  }

  // ─── STORY-1909 : Méthodes CDC de gestion des consentements ───────────────

  /** Historique complet des consentements avec types et statuts CDC. */
  getConsentHistory(): Observable<ConsentHistoryItem[]> {
    return this.http.get<ConsentHistoryItem[]>('/api/patient/consents/history');
  }

  /** FR-CONSENT-004 : Révocation d'un consentement approuvé. */
  revokeConsent(consentId: string): Observable<ConsentHistoryItem> {
    return this.http.post<ConsentHistoryItem>(`/api/patient/consents/${consentId}/revoke`, {});
  }

  /** Approbation d'un consentement en attente. */
  approveConsent(consentId: string): Observable<ConsentHistoryItem> {
    return this.http.post<ConsentHistoryItem>(`/api/patient/consents/${consentId}/approve`, {});
  }

  /** Rejet d'un consentement en attente. */
  rejectConsent(consentId: string): Observable<ConsentHistoryItem> {
    return this.http.post<ConsentHistoryItem>(`/api/patient/consents/${consentId}/reject`, {});
  }

  /** STORY-1909 : Révocation d'un accès externe approuvé par le patient. */
  revokeApprovedAccessRequest(id: string): Observable<ExternalAccessResponse> {
    return this.http.post<ExternalAccessResponse>(`/api/patient/access-requests/${id}/revoke`, {});
  }

  /** STORY-1909 : Génère un OTP pour approuver un consentement par canal OTP. */
  requestConsentOtp(globalPatientNumber: string, consentId: string): Observable<void> {
    return this.http.post<void>(
      `/api/public/patient/auth/consent-otp?globalPatientNumber=${encodeURIComponent(globalPatientNumber)}&consentId=${encodeURIComponent(consentId)}`,
      {}
    );
  }

  /** STORY-1909 : Vérifie l'OTP de consentement. */
  verifyConsentOtp(globalPatientNumber: string, consentId: string, otpCode: string): Observable<void> {
    return this.http.post<void>(
      `/api/public/patient/auth/consent-otp/verify?globalPatientNumber=${encodeURIComponent(globalPatientNumber)}&consentId=${encodeURIComponent(consentId)}&otpCode=${encodeURIComponent(otpCode)}`,
      {}
    );
  }
}

export interface PatientConsent {
  organizationId: string;
  organizationName: string;
  status: string;
  isCreator: boolean;
  scopes?: string;            // ex: "medical_records,prescriptions,lab_results,allergies_history"
  validationChannel?: string; // PORTAL | OTP_SMS | OTP_EMAIL | AGENT_HABILITE
}

/**
 * STORY-1909 — Consentement CDC complet avec type, durée, statut et motif.
 */
export interface ConsentHistoryItem {
  id: string;
  patientId: string;
  organizationId: string;
  /** Statut CDC : REQUESTED | APPROVED | REJECTED | EXPIRED | REVOKED */
  status: string;
  /** Type CDC : PONCTUEL | TEMPORAIRE | ETABLISSEMENT | PROFESSIONNEL | LIMITE | URGENCE */
  consentType: string;
  requesterUserId?: string;
  requesterOrganizationId?: string;
  reason?: string;
  scopes?: string;
  validationChannel?: string;
  requestedAt?: string;
  approvedAt?: string;
  /** FR-CONSENT-003 : date d'expiration */
  expiresAt?: string | null;
  createdAt: string;
  updatedAt: string;
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
  scopes?: string;   // ex: "medical_records,prescriptions"
  createdAt: string;
  expiresAt: string | null;
}

export interface PatientNotification {
  id: string;
  patientId: string;
  title: string;
  message: string;
  type: string; // INFO, SECURITY, EMERGENCY
  status: string; // LU, NON_LU
  createdAt: string;
}
