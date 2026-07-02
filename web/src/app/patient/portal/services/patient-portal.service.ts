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

export interface PatientPortalConsultation {
  visitId: string;
  visitNumber: string;
  visitDate: string;
  doctorName: string;
  clinicName: string;
  diagnosis: string;
  documentId: string | null;
  documentStatus: string | null;
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
}
