import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface RealtimeClinicalIntakeAck {
  id: string;
  visitId: string;
  source?: 'CONSULTATION' | 'VITALS';
  sequence: number;
  eventId: string;
  itemId: string | null;
  transcript: string;
  originalTranscript: string | null;
  confidence: number;
  reviewRequired: boolean;
  correctionCount: number;
  correctedAt: string | null;
  captureStatus: 'PENDING' | 'ANALYZED' | 'CONSUMED';
  receivedAt: string;
}

@Injectable({ providedIn: 'root' })
export class RealtimeClinicalIntakeApiService {
  private readonly http = inject(HttpClient);

  ingest(
    visitId: string,
    transcript: string,
    confidence: number | null,
    eventId: string,
    itemId?: string,
  ): Observable<RealtimeClinicalIntakeAck> {
    return this.post(
      `/api/ai/consultations/${visitId}/realtime-intake`,
      transcript,
      confidence,
      eventId,
      itemId,
    );
  }

  list(visitId: string): Observable<RealtimeClinicalIntakeAck[]> {
    return this.http.get<RealtimeClinicalIntakeAck[]>(
      `/api/ai/consultations/${visitId}/realtime-intake`,
    );
  }

  correct(
    visitId: string,
    intakeId: string,
    transcript: string,
  ): Observable<RealtimeClinicalIntakeAck> {
    return this.http.post<RealtimeClinicalIntakeAck>(
      `/api/ai/consultations/${visitId}/realtime-intake/${intakeId}/correction`,
      { transcript },
    );
  }

  consume(visitId: string): Observable<void> {
    return this.http.post<void>(
      `/api/ai/consultations/${visitId}/realtime-intake/consume`,
      {},
    );
  }

  ingestVitals(
    visitId: string,
    transcript: string,
    confidence: number | null,
    eventId: string,
    itemId?: string,
  ): Observable<RealtimeClinicalIntakeAck> {
    return this.post(
      `/api/ai/realtime/vitals/${visitId}/intake`,
      transcript,
      confidence,
      eventId,
      itemId,
    );
  }

  private post(
    endpoint: string,
    transcript: string,
    confidence: number | null,
    eventId: string,
    itemId?: string,
  ): Observable<RealtimeClinicalIntakeAck> {
    return this.http.post<RealtimeClinicalIntakeAck>(endpoint, {
      transcript,
      confidence,
      eventId,
      itemId: itemId || null,
    });
  }
}
