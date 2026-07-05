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
  PatientVaccination,
  CreatePatientVaccinationRequest,
  Hospitalization,
  CreateHospitalizationRequest,
  HospitalizationNote,
  DischargeHospitalizationRequest,
  PatientDuplicateCandidate
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

  deleteAllergy(patientId: string, allergyId: string): Observable<void> {
    return this.http.delete<void>(`/api/patients/${patientId}/allergies/${allergyId}`);
  }

  deleteMedicalHistory(patientId: string, historyId: string): Observable<void> {
    return this.http.delete<void>(`/api/patients/${patientId}/medical-history/${historyId}`);
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

  downloadDischargePdf(id: string): Observable<Blob> {
    return this.http.get(`/api/hospitalizations/${id}/pdf`, { responseType: 'blob' });
  }

  // WT2 (PDF): Téléchargement du PDF de synthèse médicale patient
  downloadSummaryPdf(id: string): Observable<Blob> {
    return this.http.get(`/api/patients/${id}/summary-pdf`, { responseType: 'blob' });
  }

  // WT3 (DUPLICATES): Récupération des candidats doublons
  getDuplicates(): Observable<PatientDuplicateCandidate[]> {
    return this.http.get<PatientDuplicateCandidate[]>('/api/patients/duplicates');
  }

  // WT3 (DUPLICATES): Ignorer un candidat doublon
  ignoreDuplicate(id: string): Observable<void> {
    return this.http.post<void>(`/api/patients/duplicates/${id}/ignore`, {});
  }

  // WT3 (DUPLICATES): Fusionner deux dossiers patients
  mergePatients(primaryId: string, secondaryId: string): Observable<void> {
    return this.http.post<void>('/api/patients/merge', { primaryId, secondaryId });
  }

  getVaccinations(patientId: string): Observable<PatientVaccination[]> {
    return this.http.get<PatientVaccination[]>(`/api/patients/${patientId}/vaccinations`);
  }

  addVaccination(patientId: string, request: CreatePatientVaccinationRequest): Observable<PatientVaccination> {
    return this.http.post<PatientVaccination>(`/api/patients/${patientId}/vaccinations`, request);
  }

  updateVaccination(patientId: string, vaccinationId: string, request: CreatePatientVaccinationRequest): Observable<PatientVaccination> {
    return this.http.put<PatientVaccination>(`/api/patients/${patientId}/vaccinations/${vaccinationId}`, request);
  }

  getMedicalSummary(patientId: string): Observable<any> {
    return this.http.get<any>(`/api/patients/${patientId}/medical-summary`);
  }
}
