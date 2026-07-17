import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { AiConsultationApiService } from './ai-consultation-api.service';

describe('AiConsultationApiService', () => {
  let service: AiConsultationApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AiConsultationApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AiConsultationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('transcrit un audio sans lancer l analyse clinique', () => {
    const audio = new Blob(['audio'], { type: 'audio/webm' });

    service.transcribeAudio('visit-1', audio).subscribe(response => {
      expect(response.transcript).toBe('Douleur à gauche');
      expect(response.status).toBe('PENDING_REVIEW');
    });

    const request = http.expectOne('/api/ai/consultations/visit-1/transcriptions/audio');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Content-Type')).toBe('audio/webm');
    request.flush({
      sessionId: 'session-1',
      transcript: 'Douleur à gauche',
      status: 'PENDING_REVIEW',
      expiresAt: '2026-07-18T10:00:00Z',
    });
  });

  it('envoie la transcription corrigée pour analyse explicite', () => {
    service.analyzeTranscript(
      'visit-1',
      'Douleur à droite et non à gauche',
    ).subscribe(response => {
      expect(response.draft.symptoms).toBe('Douleur à droite et non à gauche');
    });

    const request = http.expectOne('/api/ai/consultations/visit-1/transcriptions/analyze');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      transcript: 'Douleur à droite et non à gauche',
    });
    request.flush({
      sessionId: 'session-1',
      transcript: 'Douleur à droite et non à gauche',
      draft: { symptoms: 'Douleur à droite et non à gauche' },
      changedFields: ['symptoms'],
      assistantMessage: 'Correction prise en compte.',
      needsClarification: false,
      expiresAt: '2026-07-18T10:00:00Z',
    });
  });

  it('permet d abandonner une transcription en attente', () => {
    service.discardPendingTranscript('visit-1').subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/transcriptions/pending');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
