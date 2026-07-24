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
  StaffActiveOrganizationalUnit,
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
            map((structure) => this.withActiveUnitProjection(member, structure, units, services)),
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

  private withActiveUnitProjection(
    member: StaffMember,
    structure: StaffAssignmentStructure,
    units: readonly OrganizationalUnit[],
    services: readonly HospitalServiceCatalogEntry[],
  ): StaffMember {
    const activeAssignments = structure.unitAssignments.filter((assignment) => assignment.active);
    const activeOrganizationalUnits: StaffActiveOrganizationalUnit[] = activeAssignments.flatMap((assignment) => {
      const unit = units.find((candidate) => candidate.id === assignment.organizationalUnitId);
      if (!unit || !unit.active) return [];

      const service = unit.serviceCatalogCode
        ? services.find((candidate) => candidate.code === unit.serviceCatalogCode)
        : undefined;
      const fallbackName = unit.name?.trim() || unit.code;

      return [{
        id: unit.id,
        code: unit.code,
        nameFr: unit.name?.trim() || service?.nameFr || fallbackName,
        nameEn: unit.name?.trim() || service?.nameEn || fallbackName,
        primary: assignment.primary,
      }];
    });

    activeOrganizationalUnits.sort((left, right) => {
      if (left.primary !== right.primary) return left.primary ? -1 : 1;
      return left.nameFr.localeCompare(right.nameFr);
    });

    return { ...member, activeOrganizationalUnits };
  }
}
