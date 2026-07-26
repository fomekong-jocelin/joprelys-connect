import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

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

@Injectable({ providedIn: 'root' })
export class AmbientSpeakerReviewService {
  private readonly http = inject(HttpClient);

  transcript(visitId: string): Observable<AmbientTranscriptLedger> {
    return this.http.get<AmbientTranscriptLedger>(
      `/api/ai/consultations/${visitId}/ambient/transcript`,
    );
  }

  assignSpeaker(
    visitId: string,
    item: AmbientTranscriptItem,
    speakerType: Exclude<AmbientSpeakerType, 'UNSPECIFIED'>,
  ): Observable<AmbientTranscriptItem> {
    return this.http.post<AmbientTranscriptItem>(
      `/api/ai/consultations/${visitId}/ambient/transcript/${item.id}/corrections`,
      {
        correctionId: this.correctionId(item.id, speakerType),
        speakerType,
        text: item.text,
      },
    );
  }

  correctionId(
    itemId: string,
    speakerType: Exclude<AmbientSpeakerType, 'UNSPECIFIED'>,
  ): string {
    return `speaker-${itemId}-${speakerType.toLowerCase()}`;
  }
}
