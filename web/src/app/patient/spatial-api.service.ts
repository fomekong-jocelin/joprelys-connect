import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Ward, WardOccupancy, Bed, BedAssignment } from './patient.models';

@Injectable({
  providedIn: 'root',
})
export class SpatialApiService {
  private readonly http = inject(HttpClient);

  listWards(): Observable<Ward[]> {
    return this.http.get<Ward[]>('/api/spatial/wards');
  }

  getWardOccupancy(wardId: string): Observable<WardOccupancy> {
    return this.http.get<WardOccupancy>(`/api/spatial/wards/${wardId}/occupancy`);
  }

  updateBedStatus(bedId: string, status: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE'): Observable<Bed> {
    return this.http.post<Bed>(`/api/spatial/beds/${bedId}/status`, { status });
  }

  transferPatient(hospitalizationId: string, newBedId: string): Observable<BedAssignment> {
    return this.http.post<BedAssignment>('/api/spatial/transfers', { hospitalizationId, newBedId });
  }
}
