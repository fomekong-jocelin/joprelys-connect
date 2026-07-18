import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DoctorAppointment } from './doctor-appointments.models';

@Injectable({ providedIn: 'root' })
export class DoctorAppointmentsApiService {
  private readonly http = inject(HttpClient);

  listOwn(from: string, to: string): Observable<DoctorAppointment[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<DoctorAppointment[]>('/api/doctor/appointments', { params });
  }
}
