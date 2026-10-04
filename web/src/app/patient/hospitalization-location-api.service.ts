import { BedConfiguration } from '../clinic/spatial/spatial-configuration.models';
import { AdmissionVisit, HospitalPractitioner, HospitalPlacementOptions, EligibleMedication } from './hospitalization-workflow.models';
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { StructuredAdmissionRequest, StructuredHospitalization } from './hospitalization-location.models';

type HospitalizationWire = Omit<StructuredHospitalization, 'roomNumber'>;

@Injectable({ providedIn: 'root' })
export class HospitalizationLocationApiService {
  private readonly http = inject(HttpClient);

  placementOptions(): Observable<HospitalPlacementOptions> {
    return this.http.get<HospitalPlacementOptions>('/api/hospitalizations/placement-options');
  }
  placementBeds(spaceId: string): Observable<BedConfiguration[]> {
    return this.http.get<BedConfiguration[]>('/api/hospitalizations/placement-beds', { params: { spaceId } });
  }
  practitioners(): Observable<HospitalPractitioner[]> {
    return this.http.get<HospitalPractitioner[]>('/api/hospitalizations/practitioners');
  }
  admissionVisits(patientId: string): Observable<AdmissionVisit[]> {
    return this.http.get<AdmissionVisit[]>(`/api/hospitalizations/admission-visits/${patientId}`);
  }
  eligibleMedications(stayId: string): Observable<EligibleMedication[]> {
    return this.http.get<EligibleMedication[]>(`/api/hospitalizations/${stayId}/eligible-medications`);
  }

  admit(request: StructuredAdmissionRequest): Observable<StructuredHospitalization> {
    return this.http.post<HospitalizationWire>('/api/hospitalizations', request).pipe(
      map((hospitalization) => this.normalize(hospitalization)),
    );
  }

  listForPatient(patientId: string): Observable<StructuredHospitalization[]> {
    return this.http.get<HospitalizationWire[]>(`/api/hospitalizations/patient/${patientId}`).pipe(
      map((items) => items.map((hospitalization) => this.normalize(hospitalization))),
    );
  }

  get(id: string): Observable<StructuredHospitalization> {
    return this.http.get<HospitalizationWire>(`/api/hospitalizations/${id}`).pipe(
      map((hospitalization) => this.normalize(hospitalization)),
    );
  }

  private normalize(hospitalization: HospitalizationWire): StructuredHospitalization {
    return {
      ...hospitalization,
      roomNumber: hospitalization.spaceName,
    };
  }
}
