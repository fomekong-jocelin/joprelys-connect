import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BedAssignment } from './patient.models';
import { BedStateChangeHistoryItem, BedStateReasonCode } from '../clinic/spatial/bed-capacity.models';
import {
  BedConfiguration,
  FacilityLocationNode,
  FacilitySpace,
  InpatientSpaceProfile,
  LocationTypeEntry,
  OrganizationalUnitOccupancyView,
  SaveBedPayload,
  SaveInpatientProfilePayload,
  SaveLocationPayload,
  SaveSpacePayload,
  SaveUnitSpaceAssignmentPayload,
  SpaceOccupancyView,
  SpaceTypeEntry,
  TransferPayload,
  UnitSpaceAssignment,
} from '../clinic/spatial/spatial-configuration.models';

@Injectable({ providedIn: 'root' })
export class SpatialApiService {
  private readonly http = inject(HttpClient);

  listLocationTypes(): Observable<LocationTypeEntry[]> {
    return this.http.get<LocationTypeEntry[]>('/api/spatial/configuration/location-types');
  }

  listLocations(organizationId?: string, includeInactive = false): Observable<FacilityLocationNode[]> {
    let params = this.scopeParams(organizationId);
    params = params.set('includeInactive', String(includeInactive));
    return this.http.get<FacilityLocationNode[]>('/api/spatial/configuration/locations', { params });
  }

  createLocation(payload: SaveLocationPayload, organizationId?: string): Observable<FacilityLocationNode> {
    return this.http.post<FacilityLocationNode>('/api/spatial/configuration/locations', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateLocation(id: string, payload: SaveLocationPayload, organizationId?: string): Observable<FacilityLocationNode> {
    return this.http.put<FacilityLocationNode>(`/api/spatial/configuration/locations/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  setLocationActive(id: string, active: boolean, organizationId?: string): Observable<FacilityLocationNode> {
    const action = active ? 'activate' : 'deactivate';
    return this.http.post<FacilityLocationNode>(`/api/spatial/configuration/locations/${id}/${action}`, null, {
      params: this.scopeParams(organizationId),
    });
  }

  listSpaceTypes(): Observable<SpaceTypeEntry[]> {
    return this.http.get<SpaceTypeEntry[]>('/api/spatial/configuration/space-types');
  }

  listSpaces(organizationId?: string, locationNodeId?: string, includeInactive = false): Observable<FacilitySpace[]> {
    let params = this.scopeParams(organizationId).set('includeInactive', String(includeInactive));
    if (locationNodeId) params = params.set('locationNodeId', locationNodeId);
    return this.http.get<FacilitySpace[]>('/api/spatial/configuration/spaces', { params });
  }

  createSpace(payload: SaveSpacePayload, organizationId?: string): Observable<FacilitySpace> {
    return this.http.post<FacilitySpace>('/api/spatial/configuration/spaces', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateSpace(id: string, payload: SaveSpacePayload, organizationId?: string): Observable<FacilitySpace> {
    return this.http.put<FacilitySpace>(`/api/spatial/configuration/spaces/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  setSpaceActive(id: string, active: boolean, organizationId?: string): Observable<FacilitySpace> {
    const action = active ? 'activate' : 'deactivate';
    return this.http.post<FacilitySpace>(`/api/spatial/configuration/spaces/${id}/${action}`, null, {
      params: this.scopeParams(organizationId),
    });
  }

  enableInpatientProfile(id: string, organizationId?: string): Observable<FacilitySpace> {
    return this.http.post<FacilitySpace>(`/api/spatial/configuration/spaces/${id}/inpatient-profile`, null, {
      params: this.scopeParams(organizationId),
    });
  }

  getInpatientProfile(id: string, organizationId?: string): Observable<InpatientSpaceProfile> {
    return this.http.get<InpatientSpaceProfile>(`/api/spatial/configuration/spaces/${id}/inpatient-profile`, {
      params: this.scopeParams(organizationId),
    });
  }

  saveInpatientProfile(
    id: string,
    payload: SaveInpatientProfilePayload,
    organizationId?: string,
  ): Observable<InpatientSpaceProfile> {
    return this.http.put<InpatientSpaceProfile>(`/api/spatial/configuration/spaces/${id}/inpatient-profile`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  listBeds(spaceId: string, organizationId?: string): Observable<BedConfiguration[]> {
    const params = this.scopeParams(organizationId).set('spaceId', spaceId);
    return this.http.get<BedConfiguration[]>('/api/spatial/configuration/beds', { params });
  }

  createBed(payload: SaveBedPayload, organizationId?: string): Observable<BedConfiguration> {
    return this.http.post<BedConfiguration>('/api/spatial/configuration/beds', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateBed(id: string, payload: SaveBedPayload, organizationId?: string): Observable<BedConfiguration> {
    return this.http.put<BedConfiguration>(`/api/spatial/configuration/beds/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  deleteBed(id: string, organizationId?: string): Observable<void> {
    return this.http.delete<void>(`/api/spatial/configuration/beds/${id}`, {
      params: this.scopeParams(organizationId),
    });
  }

  listUnitSpaceAssignments(
    organizationId?: string,
    filters?: { spaceId?: string; unitId?: string; activeAt?: string },
  ): Observable<UnitSpaceAssignment[]> {
    let params = this.scopeParams(organizationId);
    if (filters?.spaceId) params = params.set('spaceId', filters.spaceId);
    if (filters?.unitId) params = params.set('unitId', filters.unitId);
    if (filters?.activeAt) params = params.set('activeAt', filters.activeAt);
    return this.http.get<UnitSpaceAssignment[]>('/api/spatial/configuration/unit-space-assignments', { params });
  }

  createUnitSpaceAssignment(
    payload: SaveUnitSpaceAssignmentPayload,
    organizationId?: string,
  ): Observable<UnitSpaceAssignment> {
    return this.http.post<UnitSpaceAssignment>('/api/spatial/configuration/unit-space-assignments', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateUnitSpaceAssignment(
    id: string,
    payload: SaveUnitSpaceAssignmentPayload,
    organizationId?: string,
  ): Observable<UnitSpaceAssignment> {
    return this.http.put<UnitSpaceAssignment>(`/api/spatial/configuration/unit-space-assignments/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  getSpaceOccupancy(spaceId: string): Observable<SpaceOccupancyView> {
    return this.http.get<SpaceOccupancyView>(`/api/spatial/spaces/${spaceId}/occupancy`);
  }

  getOrganizationalUnitOccupancy(unitId: string): Observable<OrganizationalUnitOccupancyView> {
    return this.http.get<OrganizationalUnitOccupancyView>(`/api/spatial/organizational-units/${unitId}/occupancy`);
  }

  getBedStateHistory(bedId: string): Observable<BedStateChangeHistoryItem[]> {
    return this.http.get<BedStateChangeHistoryItem[]>(`/api/spatial/beds/${bedId}/state-history`);
  }

  updateBedStatus(
    bedId: string,
    status: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE',
    note?: string,
  ): Observable<BedConfiguration> {
    return this.http.post<BedConfiguration>(`/api/spatial/beds/${bedId}/status`, { status, note });
  }

  updateBedCleaningStatus(
    bedId: string,
    status: 'CLEANING' | 'READY',
    reasonCode: BedStateReasonCode = status === 'CLEANING' ? 'CLEANING_ROUTINE' : 'CLEANING_COMPLETED',
    note?: string,
  ): Observable<BedConfiguration> {
    return this.http.post<BedConfiguration>(`/api/spatial/beds/${bedId}/cleaning-status`, {
      status,
      reasonCode,
      note,
    });
  }

  updateBedMaintenanceStatus(
    bedId: string,
    status: 'MAINTENANCE' | 'READY',
    reasonCode: BedStateReasonCode = status === 'MAINTENANCE' ? 'MAINTENANCE_CORRECTIVE' : 'MAINTENANCE_COMPLETED',
    note?: string,
  ): Observable<BedConfiguration> {
    return this.http.post<BedConfiguration>(`/api/spatial/beds/${bedId}/maintenance-status`, {
      status,
      reasonCode,
      note,
    });
  }

  updateBedCapacityStatus(
    bedId: string,
    status: 'OPEN' | 'CLOSED',
    reasonCode: BedStateReasonCode = status === 'OPEN' ? 'CAPACITY_REOPENING' : 'CAPACITY_TEMPORARY_CLOSURE',
    note?: string,
  ): Observable<BedConfiguration> {
    return this.http.post<BedConfiguration>(`/api/spatial/beds/${bedId}/capacity-status`, {
      status,
      reasonCode,
      note,
    });
  }

  transferPatient(payload: TransferPayload): Observable<BedAssignment> {
    return this.http.post<BedAssignment>('/api/spatial/transfers', payload);
  }

  private scopeParams(organizationId?: string): HttpParams {
    return organizationId ? new HttpParams().set('organizationId', organizationId) : new HttpParams();
  }
}
