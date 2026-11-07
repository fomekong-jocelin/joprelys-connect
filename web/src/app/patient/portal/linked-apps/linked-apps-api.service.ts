import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LinkedApp } from './linked-apps.models';

const LINKED_APPS_URL = '/api/patient/linked-apps';

/** Applications reliées au dossier du patient connecté ; le patient est déduit de la session côté serveur. */
@Injectable({ providedIn: 'root' })
export class PatientLinkedAppsApiService {
  private readonly http = inject(HttpClient);

  list(): Observable<LinkedApp[]> {
    return this.http.get<LinkedApp[]>(LINKED_APPS_URL);
  }

  revoke(linkId: string): Observable<void> {
    return this.http.delete<void>(`${LINKED_APPS_URL}/${encodeURIComponent(linkId)}`);
  }
}
