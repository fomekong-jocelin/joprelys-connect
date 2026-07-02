import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { InviteStaffRequest, InviteStaffResponse, StaffMember, UpdateStaffRequest } from './staff.models';

@Injectable({
  providedIn: 'root',
})
export class StaffApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<StaffMember[]> {
    return this.http.get<StaffMember[]>('/api/staff');
  }

  invite(request: InviteStaffRequest): Observable<InviteStaffResponse> {
    return this.http.post<InviteStaffResponse>('/api/staff', request);
  }

  update(id: string, request: UpdateStaffRequest): Observable<StaffMember> {
    return this.http.put<StaffMember>(`/api/staff/${id}`, request);
  }

  toggleStatus(id: string): Observable<StaffMember> {
    return this.http.post<StaffMember>(`/api/staff/${id}/toggle`, {});
  }
}
