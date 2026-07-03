import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  CreatePatientDto, 
  Patient, 
  LabOrder, 
  LabResult,
  PatientAllergy,
  CreatePatientAllergyRequest,
  PatientMedicalHistory,
  CreatePatientMedicalHistoryRequest,
  Hospitalization,
  CreateHospitalizationRequest,
  HospitalizationNote,
  DischargeHospitalizationRequest
} from './patient.models';

@Injectable({
  providedIn: 'root',
})
export class PatientApiService {
  private readonly http = inject(HttpClient);

  list(query?: string): Observable<Patient[]> {
    const url = query ? `/api/patients?q=${encodeURIComponent(query)}` : '/api/patients';
    return this.http.get<Patient[]>(url);
  }

  getById(id: string): Observable<Patient> {
    return this.http.get<Patient>(`/api/patients/${id}`);
  }

  create(dto: CreatePatientDto): Observable<Patient> {
    return this.http.post<Patient>('/api/patients', dto);
  }

  triggerEmergencyAccess(id: string, reason: string): Observable<void> {
    return this.http.post<void>(`/api/patients/${id}/emergency-access`, { reason });
  }

  getPatientLabOrders(patientId: string): Observable<LabOrder[]> {
    return this.http.get<LabOrder[]>(`/api/lab-orders/patient/${patientId}`);
  }

  getPatientLabResults(patientId: string): Observable<LabResult[]> {
    return this.http.get<LabResult[]>(`/api/lab-orders/patient/${patientId}/results`);
  }

  getAllergies(patientId: string): Observable<PatientAllergy[]> {
    return this.http.get<PatientAllergy[]>(`/api/patients/${patientId}/allergies`);
  }

  addAllergy(patientId: string, request: CreatePatientAllergyRequest): Observable<PatientAllergy> {
    return this.http.post<PatientAllergy>(`/api/patients/${patientId}/allergies`, request);
  }

  updateAllergy(patientId: string, allergyId: string, request: CreatePatientAllergyRequest): Observable<PatientAllergy> {
    return this.http.put<PatientAllergy>(`/api/patients/${patientId}/allergies/${allergyId}`, request);
  }

  getMedicalHistory(patientId: string): Observable<PatientMedicalHistory[]> {
    return this.http.get<PatientMedicalHistory[]>(`/api/patients/${patientId}/medical-history`);
  }

  addMedicalHistory(patientId: string, request: CreatePatientMedicalHistoryRequest): Observable<PatientMedicalHistory> {
    return this.http.post<PatientMedicalHistory>(`/api/patients/${patientId}/medical-history`, request);
  }

  updateMedicalHistory(patientId: string, historyId: string, request: CreatePatientMedicalHistoryRequest): Observable<PatientMedicalHistory> {
    return this.http.put<PatientMedicalHistory>(`/api/patients/${patientId}/medical-history/${historyId}`, request);
  }

  getHospitalizations(patientId: string): Observable<Hospitalization[]> {
    return this.http.get<Hospitalization[]>(`/api/hospitalizations/patient/${patientId}`);
  }

  admitPatient(request: CreateHospitalizationRequest): Observable<Hospitalization> {
    return this.http.post<Hospitalization>('/api/hospitalizations', request);
  }

  getHospitalizationDetails(id: string): Observable<Hospitalization> {
    return this.http.get<Hospitalization>(`/api/hospitalizations/${id}`);
  }

  addHospitalizationNote(id: string, noteContent: string): Observable<HospitalizationNote> {
    return this.http.post<HospitalizationNote>(`/api/hospitalizations/${id}/notes`, { noteContent });
  }

  getHospitalizationNotes(id: string): Observable<HospitalizationNote[]> {
    return this.http.get<HospitalizationNote[]>(`/api/hospitalizations/${id}/notes`);
  }

  dischargePatient(id: string, request: DischargeHospitalizationRequest): Observable<Hospitalization> {
    return this.http.post<Hospitalization>(`/api/hospitalizations/${id}/discharge`, request);
  }

  getDischargePdfUrl(id: string): string {
    return `/api/hospitalizations/${id}/pdf`;
  }
}
