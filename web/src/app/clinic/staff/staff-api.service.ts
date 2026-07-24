import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CloseStaffAssignmentRequest,
  InviteStaffRequest,
  InviteStaffResponse,
  StaffAssignmentRole,
  StaffAssignmentStructure,
  StaffMember,
  StaffSpecialtyAssignment,
  StaffSpecialtyAssignmentRequest,
  StaffUnitAssignment,
  StaffUnitAssignmentRequest,
  UpdateStaffRequest,
} from './staff.models';

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

  listAssignmentRoles(): Observable<StaffAssignmentRole[]> {
    return this.http.get<StaffAssignmentRole[]>('/api/staff/assignment-roles');
  }

  getAssignments(staffId: string): Observable<StaffAssignmentStructure> {
    return this.http.get<StaffAssignmentStructure>(`/api/staff/${staffId}/assignments`);
  }

  createSpecialtyAssignment(
    staffId: string,
    request: StaffSpecialtyAssignmentRequest,
  ): Observable<StaffSpecialtyAssignment> {
    return this.http.post<StaffSpecialtyAssignment>(`/api/staff/${staffId}/assignments/specialties`, request);
  }

  updateSpecialtyAssignment(
    staffId: string,
    assignmentId: string,
    request: StaffSpecialtyAssignmentRequest,
  ): Observable<StaffSpecialtyAssignment> {
    return this.http.put<StaffSpecialtyAssignment>(
      `/api/staff/${staffId}/assignments/specialties/${assignmentId}`,
      request,
    );
  }

  closeSpecialtyAssignment(
    staffId: string,
    assignmentId: string,
    request: CloseStaffAssignmentRequest,
  ): Observable<StaffSpecialtyAssignment> {
    return this.http.post<StaffSpecialtyAssignment>(
      `/api/staff/${staffId}/assignments/specialties/${assignmentId}/close`,
      request,
    );
  }

  createUnitAssignment(staffId: string, request: StaffUnitAssignmentRequest): Observable<StaffUnitAssignment> {
    return this.http.post<StaffUnitAssignment>(`/api/staff/${staffId}/assignments/units`, request);
  }

  updateUnitAssignment(
    staffId: string,
    assignmentId: string,
    request: StaffUnitAssignmentRequest,
  ): Observable<StaffUnitAssignment> {
    return this.http.put<StaffUnitAssignment>(
      `/api/staff/${staffId}/assignments/units/${assignmentId}`,
      request,
    );
  }

  closeUnitAssignment(
    staffId: string,
    assignmentId: string,
    request: CloseStaffAssignmentRequest,
  ): Observable<StaffUnitAssignment> {
    return this.http.post<StaffUnitAssignment>(
      `/api/staff/${staffId}/assignments/units/${assignmentId}/close`,
      request,
    );
  }
}
