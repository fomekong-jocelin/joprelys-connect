import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type ClinicalNoteSectionCode =
  | 'HISTORY_OF_PRESENT_ILLNESS'
  | 'MEDICAL_HISTORY'
  | 'ALLERGIES'
  | 'VITALS'
  | 'ASSESSMENT'
  | 'MEDICATIONS'
  | 'ORDERS'
  | 'PLAN';

export interface ClinicalLinkedEvidence {
  transcriptItemId: string;
  speakerType: string;
  speakerLabel: string | null;
  startOffsetMs: number;
  endOffsetMs: number;
  quoteStartChar: number;
  quoteEndChar: number;
  quoteText: string;
  primarySupport: boolean;
}

export interface ClinicalNoteEntry {
  factId: string;
  factSequence: number;
  factType: string;
  authority: string;
  conceptCode: string | null;
  conceptText: string | null;
  polarity: string;
  valuePrimary: string | null;
  valueSecondary: string | null;
  unitCode: string | null;
  temporalityText: string | null;
  laterality: string | null;
  frequencyText: string | null;
  routeText: string | null;
  evidence: ClinicalLinkedEvidence[];
}

export interface ClinicalNoteSection {
  code: ClinicalNoteSectionCode;
  entries: ClinicalNoteEntry[];
}

export interface ClinicalNoteProjection {
  visitId: string;
  projectionVersion: string;
  maxFactSequence: number;
  sections: ClinicalNoteSection[];
}

export interface ValidatedClinicalFactRef {
  factId: string;
  factSequence: number;
  sectionCode: string;
  position: number;
}

export interface ClinicalNoteValidation {
  id: string;
  validationId: string;
  visitId: string;
  projectionVersion: string;
  projectionSchemaVersion: string;
  maxFactSequence: number;
  validatedByUserId: string;
  validatedAt: string;
  facts: ValidatedClinicalFactRef[];
}

export interface ClinicalNoteValidationHistory {
  visitId: string;
  validations: ClinicalNoteValidation[];
}

@Injectable({ providedIn: 'root' })
export class ClinicalNoteProjectionApiService {
  private readonly http = inject(HttpClient);

  getProjection(visitId: string): Observable<ClinicalNoteProjection> {
    return this.http.get<ClinicalNoteProjection>(
      `/api/ai/consultations/${encodeURIComponent(visitId)}/facts/note-projection`,
    );
  }

  getValidationHistory(visitId: string): Observable<ClinicalNoteValidationHistory> {
    return this.http.get<ClinicalNoteValidationHistory>(
      `/api/ai/consultations/${encodeURIComponent(visitId)}/facts/note-projection/validations`,
    );
  }

  validateProjection(
    visitId: string,
    validationId: string,
    projectionVersion: string,
  ): Observable<ClinicalNoteValidation> {
    return this.http.post<ClinicalNoteValidation>(
      `/api/ai/consultations/${encodeURIComponent(visitId)}/facts/note-projection/validations`,
      { validationId, projectionVersion },
    );
  }
}
