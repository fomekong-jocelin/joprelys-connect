import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';

export type AmbientNoteTemplate = 'SOAP' | 'APSO' | 'MULTI_SECTION';
export type AmbientNoteStatus = 'GENERATED' | 'ACCEPTED' | 'REJECTED';
export type AmbientSpeakerType = 'DOCTOR' | 'PATIENT' | 'UNSPECIFIED';

export interface AmbientTranscriptItem {
  id: string;
  sequence: number;
  sourceEventId: string;
  source: 'AMBIENT_DIARIZED' | 'MANUAL_CORRECTION';
  speakerType: AmbientSpeakerType;
  speakerLabel: string | null;
  text: string;
  locale: string;
  startOffsetMs: number;
  endOffsetMs: number;
  status: 'FINAL' | 'REJECTED';
  supersedesItemId: string | null;
  createdAt: string;
}

export interface AmbientTranscriptLedger {
  visitId: string;
  items: AmbientTranscriptItem[];
}

export interface AmbientNoteStatement {
  id: string;
  section: string;
  order: number;
  text: string;
  critical: boolean;
  evidenceItemIds: string[];
}

export interface AmbientNoteRevision {
  id: string;
  visitId: string;
  revision: number;
  template: AmbientNoteTemplate;
  locale: string;
  status: AmbientNoteStatus;
  transcriptMaxSequence: number;
  model: string | null;
  tokensUsed: number | null;
  supersedesNoteId: string | null;
  createdAt: string;
  decidedAt: string | null;
  decidedByUserId: string | null;
  statements: AmbientNoteStatement[];
}

@Injectable({ providedIn: 'root' })
export class AmbientNoteApiService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  transcript(visitId: string): Observable<AmbientTranscriptLedger> {
    return this.http.get<AmbientTranscriptLedger>(
      `/api/ai/consultations/${visitId}/ambient/transcript`,
    );
  }

  latestNote(visitId: string): Observable<AmbientNoteRevision | null> {
    return this.http.get<AmbientNoteRevision | null>(
      `/api/ai/consultations/${visitId}/ambient/notes/latest`,
    );
  }

  generateNote(
    visitId: string,
    template: AmbientNoteTemplate,
  ): Observable<AmbientNoteRevision> {
    return this.http.post<AmbientNoteRevision>(
      `/api/ai/consultations/${visitId}/ambient/notes/generate`,
      { template, locale: this.i18n.currentLanguage() },
    );
  }

  decideNote(
    visitId: string,
    noteId: string,
    decision: 'ACCEPT' | 'REJECT',
  ): Observable<AmbientNoteRevision> {
    return this.http.post<AmbientNoteRevision>(
      `/api/ai/consultations/${visitId}/ambient/notes/${noteId}/decision`,
      { decision },
    );
  }
}
