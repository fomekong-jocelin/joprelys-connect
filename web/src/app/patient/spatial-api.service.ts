import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Ward, Bed, BedAssignment } from './patient.models';
import {
  BedCapacityStatus,
  BedCapacityView,
  WardCapacityView,
} from '../clinic/spatial/bed-capacity.models';
import {
  SaveBedPayload,
  SaveRoomPayload,
  SaveWardPayload,
  SpatialConfiguration,
} from '../clinic/spatial/spatial-configuration.models';

@Injectable({
  providedIn: 'root',
})
export class SpatialApiService {
  private readonly http = inject(HttpClient);

  listWards(): Observable<Ward[]> {
    return this.http.get<Ward[]>('/api/spatial/wards');
  }

  getWardOccupancy(wardId: string): Observable<WardCapacityView> {
    return this.http.get<WardCapacityView>(`/api/spatial/wards/${wardId}/occupancy`);
  }

  updateBedStatus(
    bedId: string,
    status: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE',
  ): Observable<BedCapacityView> {
    return this.http.post<BedCapacityView>(`/api/spatial/beds/${bedId}/status`, { status });
  }

  updateBedCleaningStatus(
    bedId: string,
    status: 'CLEANING' | 'READY',
  ): Observable<BedCapacityView> {
    return this.http.post<BedCapacityView>(`/api/spatial/beds/${bedId}/cleaning-status`, { status });
  }

  updateBedMaintenanceStatus(
    bedId: string,
    status: 'MAINTENANCE' | 'READY',
  ): Observable<BedCapacityView> {
    return this.http.post<BedCapacityView>(`/api/spatial/beds/${bedId}/maintenance-status`, { status });
  }

  updateBedCapacityStatus(bedId: string, status: BedCapacityStatus): Observable<BedCapacityView> {
    return this.http.post<BedCapacityView>(`/api/spatial/beds/${bedId}/capacity-status`, { status });
  }

  transferPatient(hospitalizationId: string, newBedId: string): Observable<BedAssignment> {
    return this.http.post<BedAssignment>('/api/spatial/transfers', { hospitalizationId, newBedId });
  }

  getConfiguration(organizationId?: string): Observable<SpatialConfiguration> {
    return this.http.get<SpatialConfiguration>('/api/spatial/configuration', {
      params: this.scopeParams(organizationId),
    });
  }

  createWard(payload: SaveWardPayload, organizationId?: string): Observable<Ward> {
    return this.http.post<Ward>('/api/spatial/configuration/wards', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateWard(id: string, payload: SaveWardPayload, organizationId?: string): Observable<Ward> {
    return this.http.put<Ward>(`/api/spatial/configuration/wards/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  deleteWard(id: string, organizationId?: string): Observable<void> {
    return this.http.delete<void>(`/api/spatial/configuration/wards/${id}`, {
      params: this.scopeParams(organizationId),
    });
  }

  createRoom(payload: SaveRoomPayload, organizationId?: string): Observable<unknown> {
    return this.http.post('/api/spatial/configuration/rooms', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateRoom(id: string, payload: SaveRoomPayload, organizationId?: string): Observable<unknown> {
    return this.http.put(`/api/spatial/configuration/rooms/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  deleteRoom(id: string, organizationId?: string): Observable<void> {
    return this.http.delete<void>(`/api/spatial/configuration/rooms/${id}`, {
      params: this.scopeParams(organizationId),
    });
  }

  createBed(payload: SaveBedPayload, organizationId?: string): Observable<Bed> {
    return this.http.post<Bed>('/api/spatial/configuration/beds', payload, {
      params: this.scopeParams(organizationId),
    });
  }

  updateBed(id: string, payload: SaveBedPayload, organizationId?: string): Observable<Bed> {
    return this.http.put<Bed>(`/api/spatial/configuration/beds/${id}`, payload, {
      params: this.scopeParams(organizationId),
    });
  }

  deleteBed(id: string, organizationId?: string): Observable<void> {
    return this.http.delete<void>(`/api/spatial/configuration/beds/${id}`, {
      params: this.scopeParams(organizationId),
    });
  }

  private scopeParams(organizationId?: string): HttpParams {
    return organizationId ? new HttpParams().set('organizationId', organizationId) : new HttpParams();
  }
}
