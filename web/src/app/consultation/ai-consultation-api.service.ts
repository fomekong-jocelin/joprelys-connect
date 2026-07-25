import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';

export type AiField =
  | 'symptoms'
  | 'clinicalExam'
  | 'suspectedDiagnosis'
  | 'diagnosis'
  | 'finalDiagnosis'
  | 'conclusion'
  | 'advice'
  | 'followUp'
  | 'prescription'
  | 'labOrders'
  | 'vitals';

export type AiConsultationDraft = Partial<Record<AiField, string>>;
export type AiTranscriptStatus = 'NONE' | 'PENDING_REVIEW' | 'ANALYZED';
export type AiConversationRole = 'USER' | 'ASSISTANT';
export type AiConversationSource =
  | 'TEXT'
  | 'AUDIO'
  | 'CLARIFICATION'
  | 'AI'
  | 'SYSTEM';
export type AiDecision = 'ACCEPT' | 'REJECT';

export interface AiPrescriptionLine {
  drugName: string;
  dosage?: string;
  posology?: string;
  duration?: string;
  quantity?: string;
  instructions?: string;
  form?: string;
  route?: string;
  frequency?: string;
  substitutionAllowed?: boolean;
}

export interface AiVitalsDraft {
  temperature?: number;
  weight?: number;
  height?: number;
  pulse?: number;
  systolic?: number;
  diastolic?: number;
  spo2?: number;
  glycemia?: number;
  respiratoryRate?: number;
  painScale?: number;
}

export interface AiConversationMessage {
  id: string;
  role: AiConversationRole;
  content: string;
  source: AiConversationSource;
  createdAt: string;
  needsClarification: boolean;
}

export interface AiClarification {
  id: string;
  field: AiField;
  question: string;
  status: 'PENDING' | 'RESOLVED';
  options: string[];
  createdAt: string;
  answer: string | null;
  resolvedAt: string | null;
}

export interface AiFieldProposal {
  id: string;
  field: AiField;
  operation: 'SET' | 'CLEAR';
  previousValue: string | null;
  proposedValue: string | null;
  reason: string;
  uncertainty: 'LOW' | 'MEDIUM' | 'HIGH' | 'UNKNOWN';
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
  decidedAt: string | null;
}

export interface AiRevision {
  id: string;
  sequence: number;
  status: 'PENDING' | 'DECIDED';
  createdAt: string;
  proposals: AiFieldProposal[];
}

export interface AiSessionResponse {
  sessionId: string;
  visitId: string;
  status: string;
  expiresAt: string;
  draft: AiConsultationDraft;
  transcript: string | null;
  pendingTranscript: string | null;
  transcriptStatus: AiTranscriptStatus;
  conversation: AiConversationMessage[];
  clarifications: AiClarification[];
  revisions: AiRevision[];
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
  conversation: AiConversationMessage[];
  clarifications: AiClarification[];
  revisions: AiRevision[];
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
  private readonly i18n = inject(I18nService);

  startSession(visitId: string, draft: AiConsultationDraft): Observable<AiSessionResponse> {
    return this.http.post<AiSessionResponse>(
      `/api/ai/consultations/${visitId}/sessions`,
      { draft, locale: this.i18n.currentLanguage() },
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

  sendAudio(visitId: string, audio: Blob): Observable<AiMessageResponse> {
    const headers = new HttpHeaders({ 'Content-Type': audio.type || 'audio/webm' });
    return this.http.post<AiMessageResponse>(
      `/api/ai/consultations/${visitId}/messages/audio`,
      audio,
      { headers },
    );
  }

  answerClarification(
    visitId: string,
    clarificationId: string,
    answer: string,
  ): Observable<AiMessageResponse> {
    return this.http.post<AiMessageResponse>(
      `/api/ai/consultations/${visitId}/clarifications/${clarificationId}/answer`,
      { answer },
    );
  }

  answerClarificationAudio(
    visitId: string,
    clarificationId: string,
    audio: Blob,
  ): Observable<AiMessageResponse> {
    const headers = new HttpHeaders({
      'Content-Type': audio.type || 'audio/webm',
      'X-Joprelys-Locale': this.i18n.currentLanguage(),
    });
    return this.http.post<AiMessageResponse>(
      `/api/ai/consultations/${visitId}/clarifications/${clarificationId}/answer/audio`,
      audio,
      { headers },
    );
  }

  decideProposal(
    visitId: string,
    revisionId: string,
    proposalId: string,
    decision: AiDecision,
  ): Observable<AiSessionResponse> {
    return this.http.post<AiSessionResponse>(
      `/api/ai/consultations/${visitId}/revisions/${revisionId}/proposals/${proposalId}/decision`,
      { decision },
    );
  }

  decideRevision(
    visitId: string,
    revisionId: string,
    decision: AiDecision,
  ): Observable<AiSessionResponse> {
    return this.http.post<AiSessionResponse>(
      `/api/ai/consultations/${visitId}/revisions/${revisionId}/decision`,
      { decision },
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

  synthesizeSpeech(text: string): Observable<Blob> {
    return this.http.post('/api/ai/voice/speech', { text }, { responseType: 'blob' });
  }

  deleteSession(visitId: string): Observable<void> {
    return this.http.delete<void>(`/api/ai/consultations/${visitId}/session`);
  }

  loadQrCode(visitId: string): Observable<Blob> {
    return this.http.get(`/api/visits/${visitId}/qrcode`, { responseType: 'blob' });
  }
}
