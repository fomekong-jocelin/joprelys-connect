import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { StructuredAdmissionRequest, StructuredHospitalization } from './hospitalization-location.models';

type HospitalizationWire = Omit<StructuredHospitalization, 'roomNumber'>;

@Injectable({ providedIn: 'root' })
export class HospitalizationLocationApiService {
  private readonly http = inject(HttpClient);

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
