import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface RealtimeClinicalIntakeAck {
  id: string;
  visitId: string;
  sequence: number;
  eventId: string;
  itemId: string | null;
  transcript: string;
  confidence: number;
  receivedAt: string;
}

@Injectable({ providedIn: 'root' })
export class RealtimeClinicalIntakeApiService {
  private readonly http = inject(HttpClient);

  ingest(
    visitId: string,
    transcript: string,
    confidence: number,
    eventId: string,
    itemId?: string,
  ): Observable<RealtimeClinicalIntakeAck> {
    return this.http.post<RealtimeClinicalIntakeAck>(
      `/api/ai/consultations/${visitId}/realtime-intake`,
      {
        transcript,
        confidence,
        eventId,
        itemId: itemId || null,
      },
    );
  }
}
