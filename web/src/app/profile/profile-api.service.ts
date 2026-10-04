import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
export interface StaffProfile {
  displayName: string; email: string; role: string; phone?: string;
  registrationNumber?: string; bio?: string; photoPath?: string; signaturePath?: string; stampPath?: string;
}
@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly http = inject(HttpClient);
  load() { return this.http.get<StaffProfile>('/api/profile'); }
  save(profile: { displayName: string; phone: string | null; photoPath: string | null;
    signaturePath: string | null; stampPath: string | null; registrationNumber: string | null; bio: string | null }) {
    return this.http.put<StaffProfile>('/api/profile', profile);
  }
  upload(body: FormData) { return this.http.post<{ filePath: string; viewUrl: string }>('/api/files/upload', body); }
}
