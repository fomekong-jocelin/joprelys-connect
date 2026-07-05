import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Consultation, SaveConsultationRequest, Prescription, SavePrescriptionRequest } from './consultation.models';

@Injectable({ providedIn: 'root' })
export class ConsultationApiService {
  private readonly http = inject(HttpClient);

  saveConsultation(visitId: string, dto: SaveConsultationRequest): Observable<Consultation> {
    return this.http.post<Consultation>(`/api/visits/${visitId}/consultation`, dto);
  }

  getConsultation(visitId: string): Observable<Consultation> {
    return this.http.get<Consultation>(`/api/visits/${visitId}/consultation`);
  }

  savePrescription(consultationId: string, dto: SavePrescriptionRequest): Observable<Prescription> {
    return this.http.post<Prescription>(`/api/consultations/${consultationId}/prescription`, dto);
  }

  getPrescription(consultationId: string): Observable<Prescription> {
    return this.http.get<Prescription>(`/api/consultations/${consultationId}/prescription`);
  }

  getPatientConsultations(patientId: string): Observable<Consultation[]> {
    return this.http.get<Consultation[]>(`/api/patients/${patientId}/consultations`);
  }

  downloadDocument(visitId: string): Observable<Blob> {
    return this.http.get(`/api/visits/${visitId}/document`, { responseType: 'blob' });
  }

  verifyDocumentPublic(documentId: string): Observable<any> {
    return this.http.get<any>(`/api/public/documents/${documentId}/verify`);
  }

  revokeDocument(documentId: string, reason: string): Observable<any> {
    return this.http.patch<any>(`/api/documents/${documentId}/revoke`, { reason });
  }

  cancelDocument(documentId: string, reason: string): Observable<any> {
    return this.http.patch<any>(`/api/documents/${documentId}/cancel`, { reason });
  }

  transmitPrescription(prescriptionId: string): Observable<any> {
    return this.http.post<any>(`/api/prescriptions/${prescriptionId}/transmit`, {});
  }
}
