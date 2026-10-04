import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CreateVisitRequest } from '../visit/visit.models';
import { OTHER_SERVICE } from './visit-admission-options.service';

/** Contrôles de visite communs à l'admission unifiée et à l'admission depuis le dossier patient. */
export function visitDetailsControls(): Record<string, unknown> {
  return {
    reason: [''],
    orientation: ['CONSULTATION', [Validators.required]],
    customOrientation: [''],
    service: [''],
    customService: [''],
    mainPractitionerId: [''],
    arrivalAt: [localDateTimeNow()],
  };
}

export function createVisitDetailsForm(fb: FormBuilder): FormGroup {
  return fb.group(visitDetailsControls());
}

export function hasRequiredVisitDetails(value: Record<string, unknown>): boolean {
  return Boolean(String(value['reason'] ?? '').trim() && String(value['orientation'] ?? '').trim());
}

export function toCreateVisitRequest(patientId: string, value: Record<string, unknown>): CreateVisitRequest {
  const orientation = String(value['orientation'] ?? '');
  const service = String(value['service'] ?? '');
  const arrivalAt = String(value['arrivalAt'] ?? '');
  return {
    patientId,
    reason: String(value['reason'] ?? '').trim(),
    orientation: orientation === 'OTHER' ? optional(value['customOrientation']) ?? 'OTHER' : orientation,
    service: service === OTHER_SERVICE ? optional(value['customService']) : optional(service),
    mainPractitionerId: optional(value['mainPractitionerId']),
    arrivalAt: arrivalAt ? new Date(arrivalAt).toISOString() : new Date().toISOString(),
  };
}

/** Date et heure locales au format attendu par `<input type="datetime-local">`. */
export function localDateTimeNow(): string {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

function optional(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined;
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : undefined;
}
