import { Injectable, signal } from '@angular/core';
import { Patient } from './patient.models';

@Injectable({
  providedIn: 'root'
})
export class ActivePatientService {
  readonly patient = signal<Patient | null>(null);
}
