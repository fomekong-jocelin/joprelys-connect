import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import {
  HospitalServiceCatalogEntry,
  OrganizationalUnit,
} from '../hospital-organization/hospital-organization.models';
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
    return forkJoin({
      staff: this.http.get<StaffMember[]>('/api/staff'),
      units: this.http.get<OrganizationalUnit[]>('/api/hospital-organization/units'),
      services: this.http.get<HospitalServiceCatalogEntry[]>('/api/hospital-organization/catalogs/services'),
    }).pipe(
      switchMap(({ staff, units, services }) => {
        if (staff.length === 0) return of([] as StaffMember[]);
        return forkJoin(staff.map((member) =>
          this.getAssignments(member.id).pipe(
            map((structure) => this.withPrimaryUnitProjection(member, structure, units, services)),
          ),
        ));
      }),
    );
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

  private withPrimaryUnitProjection(
    member: StaffMember,
    structure: StaffAssignmentStructure,
    units: readonly OrganizationalUnit[],
    services: readonly HospitalServiceCatalogEntry[],
  ): StaffMember {
    const activeUnits = structure.unitAssignments.filter((assignment) => assignment.active);
    const assignment = activeUnits.find((candidate) => candidate.primary) ?? activeUnits[0];
    if (!assignment) return member;

    const unit = units.find((candidate) => candidate.id === assignment.organizationalUnitId);
    if (!unit) return member;

    const service = unit.serviceCatalogCode
      ? services.find((candidate) => candidate.code === unit.serviceCatalogCode)
      : undefined;
    const department = unit.name?.trim() || service?.nameFr || unit.code;
    return { ...member, department };
  }
}
