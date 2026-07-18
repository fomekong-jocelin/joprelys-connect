import { Injectable, signal } from '@angular/core';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { Patient } from './patient.models';

@Injectable({
  providedIn: 'root'
})
export class ActivePatientService {
  readonly patient = signal<Patient | null>(null);

  constructor(tokenStorage: AuthTokenStorageService) {
    tokenStorage.registerSessionBoundaryCleanup(() => this.patient.set(null));
  }
}
