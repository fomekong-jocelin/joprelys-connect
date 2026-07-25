import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface AiVitalsProposal {
  transcript: string;
  vitals: Partial<Record<AiVitalField, number>>;
  assistantMessage: string;
  needsConfirmation: boolean;
  confirmationReason: string;
}

export type AiVitalField =
  | 'temperature'
  | 'weight'
  | 'height'
  | 'pulse'
  | 'systolic'
  | 'diastolic'
  | 'spo2'
  | 'glycemia'
  | 'respiratoryRate'
  | 'painScale';

@Injectable({ providedIn: 'root' })
export class AiVitalsApiService {
  private readonly http = inject(HttpClient);

  analyzeText(
    visitId: string,
    text: string,
    locale: 'fr' | 'en',
    currentVitals: Partial<Record<AiVitalField, number>>,
  ): Observable<AiVitalsProposal> {
    return this.http.post<AiVitalsProposal>(`/api/ai/vitals/${visitId}/text`, {
      text,
      locale,
      currentVitals,
    });
  }

  analyzeAudio(
    visitId: string,
    audio: Blob,
    locale: 'fr' | 'en',
    currentVitals: Partial<Record<AiVitalField, number>>,
  ): Observable<AiVitalsProposal> {
    const form = new FormData();
    const extension = this.extensionFor(audio.type);
    form.append('audio', audio, `vitals-${Date.now()}.${extension}`);
    form.append('locale', locale);
    form.append('currentVitals', JSON.stringify(currentVitals));
    return this.http.post<AiVitalsProposal>(`/api/ai/vitals/${visitId}/audio`, form);
  }

  private extensionFor(contentType: string): string {
    if (contentType.includes('mp4')) return 'm4a';
    if (contentType.includes('mpeg')) return 'mp3';
    if (contentType.includes('wav')) return 'wav';
    return 'webm';
  }
}
