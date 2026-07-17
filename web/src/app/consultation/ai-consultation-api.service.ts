import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type AiField =
  | 'symptoms'
  | 'clinicalExam'
  | 'suspectedDiagnosis'
  | 'diagnosis'
  | 'finalDiagnosis'
  | 'conclusion'
  | 'advice'
  | 'followUp';

export type AiConsultationDraft = Partial<Record<AiField, string>>;
export type AiTranscriptStatus = 'NONE' | 'PENDING_REVIEW' | 'ANALYZED';

export interface AiSessionResponse {
  sessionId: string;
  visitId: string;
  status: string;
  expiresAt: string;
  draft: AiConsultationDraft;
  transcript: string | null;
  pendingTranscript: string | null;
  transcriptStatus: AiTranscriptStatus;
  assistantMessage: string | null;
  needsClarification: boolean;
}

export interface AiMessageResponse {
  sessionId: string;
  transcript: string | null;
  draft: AiConsultationDraft;
  changedFields: AiField[];
  assistantMessage: string;
  needsClarification: boolean;
  expiresAt: string;
}

export interface AiTranscriptionResponse {
  sessionId: string;
  transcript: string;
  status: 'PENDING_REVIEW';
  expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class AiConsultationApiService {
  private readonly http = inject(HttpClient);

  startSession(visitId: string, draft: AiConsultationDraft): Observable<AiSessionResponse> {
    return this.http.post<AiSessionResponse>(
      `/api/ai/consultations/${visitId}/sessions`,
      { draft },
    );
  }

  getSession(visitId: string): Observable<AiSessionResponse | null> {
    return this.http.get<AiSessionResponse | null>(
      `/api/ai/consultations/${visitId}/session`,
    );
  }

  sendText(visitId: string, text: string): Observable<AiMessageResponse> {
    return this.http.post<AiMessageResponse>(
      `/api/ai/consultations/${visitId}/messages/text`,
      { text },
    );
  }

  transcribeAudio(visitId: string, audio: Blob): Observable<AiTranscriptionResponse> {
    const headers = new HttpHeaders({ 'Content-Type': audio.type || 'audio/webm' });
    return this.http.post<AiTranscriptionResponse>(
      `/api/ai/consultations/${visitId}/transcriptions/audio`,
      audio,
      { headers },
    );
  }

  analyzeTranscript(visitId: string, transcript: string): Observable<AiMessageResponse> {
    return this.http.post<AiMessageResponse>(
      `/api/ai/consultations/${visitId}/transcriptions/analyze`,
      { transcript },
    );
  }

  discardPendingTranscript(visitId: string): Observable<void> {
    return this.http.delete<void>(
      `/api/ai/consultations/${visitId}/transcriptions/pending`,
    );
  }

  deleteSession(visitId: string): Observable<void> {
    return this.http.delete<void>(`/api/ai/consultations/${visitId}/session`);
  }

  loadQrCode(visitId: string): Observable<Blob> {
    return this.http.get(`/api/visits/${visitId}/qrcode`, { responseType: 'blob' });
  }
}
