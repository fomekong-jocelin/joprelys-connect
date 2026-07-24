import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AppointmentSlot,
  DoctorDirectoryEntry,
  PatientAppointment,
  PatientBookAppointmentRequest,
} from './patient-appointments.models';

@Injectable({ providedIn: 'root' })
export class PatientAppointmentsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/patient/appointments';

  listDoctors(specialtyCode = '', organizationalUnitId = ''): Observable<DoctorDirectoryEntry[]> {
    let params = new HttpParams();
    if (specialtyCode.trim()) params = params.set('specialtyCode', specialtyCode.trim());
    if (organizationalUnitId.trim()) params = params.set('organizationalUnitId', organizationalUnitId.trim());
    return this.http.get<DoctorDirectoryEntry[]>(`${this.baseUrl}/doctors`, { params });
  }

  listSlots(doctorId: string, from: string, to: string): Observable<AppointmentSlot[]> {
    const params = new HttpParams()
      .set('doctorId', doctorId)
      .set('from', from)
      .set('to', to);
    return this.http.get<AppointmentSlot[]>(`${this.baseUrl}/slots`, { params });
  }

  book(request: PatientBookAppointmentRequest): Observable<PatientAppointment> {
    return this.http.post<PatientAppointment>(this.baseUrl, request);
  }

  listOwn(from?: string, to?: string): Observable<PatientAppointment[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<PatientAppointment[]>(this.baseUrl, { params });
  }

  cancel(appointmentId: string, cancellationReason = ''): Observable<PatientAppointment> {
    return this.http.post<PatientAppointment>(`${this.baseUrl}/${appointmentId}/cancel`, {
      cancellationReason: cancellationReason.trim() || undefined,
    });
  }
}
