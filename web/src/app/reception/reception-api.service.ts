import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateReceptionLogRequest, ReceptionLog } from './reception.models';

@Injectable({
  providedIn: 'root',
})
export class ReceptionApiService {
  private readonly http = inject(HttpClient);

  create(dto: CreateReceptionLogRequest): Observable<ReceptionLog> {
    return this.http.post<ReceptionLog>('/api/reception', dto);
  }

  getAll(): Observable<ReceptionLog[]> {
    return this.http.get<ReceptionLog[]>('/api/reception');
  }

  getById(id: string): Observable<ReceptionLog> {
    return this.http.get<ReceptionLog>(`/api/reception/${id}`);
  }

  markDeparture(id: string): Observable<ReceptionLog> {
    return this.http.post<ReceptionLog>(`/api/reception/${id}/departure`, {});
  }
}
