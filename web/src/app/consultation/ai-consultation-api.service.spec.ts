import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiRevision,
  AiSessionResponse,
} from './ai-consultation-api.service';

describe('AiConsultationApiService', () => {
  let service: AiConsultationApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AiConsultationApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: I18nService,
          useValue: { currentLanguage: () => 'fr' },
        },
      ],
    });
    service = TestBed.inject(AiConsultationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('démarre la session IA avec la langue active', () => {
    service.startSession('visit-1', { symptoms: 'Fièvre' }).subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/sessions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      draft: { symptoms: 'Fièvre' },
      locale: 'fr',
    });
    request.flush(sessionResponse());
  });

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

  it('envoie la transcription corrigée et reçoit une proposition non appliquée', () => {
    service.analyzeTranscript(
      'visit-1',
      'Douleur à droite et non à gauche',
    ).subscribe(response => {
      expect(response.draft.symptoms).toBeUndefined();
      expect(response.revisions[0].proposals[0].proposedValue)
        .toBe('Douleur à droite et non à gauche');
    });

    const request = http.expectOne('/api/ai/consultations/visit-1/transcriptions/analyze');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      transcript: 'Douleur à droite et non à gauche',
    });
    request.flush({
      sessionId: 'session-1',
      transcript: 'Douleur à droite et non à gauche',
      draft: {},
      changedFields: ['symptoms'],
      assistantMessage: 'Correction proposée.',
      needsClarification: false,
      conversation: [],
      clarifications: [],
      revisions: [pendingRevision()],
      expiresAt: '2026-07-18T10:00:00Z',
    });
  });

  it('répond à une clarification avec son identifiant', () => {
    service.answerClarification(
      'visit-1',
      'clarification-1',
      'Depuis deux jours',
    ).subscribe(response => {
      expect(response.clarifications[0].status).toBe('RESOLVED');
      expect(response.clarifications[0].answer).toBe('Depuis deux jours');
    });

    const request = http.expectOne(
      '/api/ai/consultations/visit-1/clarifications/clarification-1/answer',
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ answer: 'Depuis deux jours' });
    request.flush({
      sessionId: 'session-1',
      transcript: null,
      draft: {},
      changedFields: ['symptoms'],
      assistantMessage: 'Durée proposée dans le brouillon.',
      needsClarification: false,
      conversation: [],
      clarifications: [
        {
          id: 'clarification-1',
          field: 'symptoms',
          question: 'Depuis combien de temps ?',
          status: 'RESOLVED',
          options: [],
          createdAt: '2026-07-18T09:59:00Z',
          answer: 'Depuis deux jours',
          resolvedAt: '2026-07-18T10:00:00Z',
        },
      ],
      revisions: [pendingRevision()],
      expiresAt: '2026-07-18T10:30:00Z',
    });
  });

  it('accepte une proposition avec son identifiant', () => {
    service.decideProposal(
      'visit-1',
      'revision-1',
      'proposal-1',
      'ACCEPT',
    ).subscribe(response => {
      expect(response.draft.symptoms).toBe('Douleur à droite et non à gauche');
    });

    const request = http.expectOne(
      '/api/ai/consultations/visit-1/revisions/revision-1/proposals/proposal-1/decision',
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ decision: 'ACCEPT' });
    request.flush(sessionResponse());
  });

  it('décide toutes les propositions d une révision', () => {
    service.decideRevision(
      'visit-1',
      'revision-1',
      'REJECT',
    ).subscribe(response => {
      expect(response.revisions[0].status).toBe('DECIDED');
    });

    const request = http.expectOne(
      '/api/ai/consultations/visit-1/revisions/revision-1/decision',
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ decision: 'REJECT' });
    const response = sessionResponse();
    response.revisions[0].status = 'DECIDED';
    response.revisions[0].proposals[0].status = 'REJECTED';
    request.flush(response);
  });

  it('permet d abandonner une transcription en attente', () => {
    service.discardPendingTranscript('visit-1').subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/transcriptions/pending');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  function pendingRevision(): AiRevision {
    return {
      id: 'revision-1',
      sequence: 1,
      status: 'PENDING',
      createdAt: '2026-07-18T10:00:00Z',
      proposals: [
        {
          id: 'proposal-1',
          field: 'symptoms',
          operation: 'SET',
          previousValue: null,
          proposedValue: 'Douleur à droite et non à gauche',
          reason: 'Latéralité corrigée par le médecin.',
          uncertainty: 'LOW',
          status: 'PENDING',
          createdAt: '2026-07-18T10:00:00Z',
          decidedAt: null,
        },
      ],
    };
  }

  function sessionResponse(): AiSessionResponse {
    const revision = pendingRevision();
    revision.status = 'DECIDED';
    revision.proposals[0].status = 'ACCEPTED';
    revision.proposals[0].decidedAt = '2026-07-18T10:01:00Z';
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
      status: 'ACTIVE',
      expiresAt: '2026-07-18T10:30:00Z',
      draft: { symptoms: 'Douleur à droite et non à gauche' },
      transcript: null,
      pendingTranscript: null,
      transcriptStatus: 'NONE',
      conversation: [],
      clarifications: [],
      revisions: [revision],
      assistantMessage: 'Correction proposée.',
      needsClarification: false,
    };
  }
});
