import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  AmbientSpeakerReviewService,
  AmbientTranscriptItem,
} from './ambient-speaker-review.service';

describe('AmbientSpeakerReviewService', () => {
  let service: AmbientSpeakerReviewService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AmbientSpeakerReviewService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AmbientSpeakerReviewService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('should load only the effective ambient transcript endpoint', () => {
    service.transcript('visit-1').subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/ambient/transcript');
    expect(request.request.method).toBe('GET');
    request.flush({ visitId: 'visit-1', items: [] });
  });

  it('should assign speaker by creating an append-only correction with unchanged verbatim', () => {
    const item = transcriptItem();
    service.assignSpeaker('visit-1', item, 'DOCTOR').subscribe();

    const request = http.expectOne(
      `/api/ai/consultations/visit-1/ambient/transcript/${item.id}/corrections`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      correctionId: `speaker-${item.id}-doctor`,
      speakerType: 'DOCTOR',
      text: item.text,
    });
    request.flush({ ...item, speakerType: 'DOCTOR', speakerLabel: 'human:DOCTOR' });
  });

  it('should generate a stable correction id for idempotent retries', () => {
    const id = '11111111-1111-1111-1111-111111111111';

    expect(service.correctionId(id, 'DOCTOR')).toBe(
      'speaker-11111111-1111-1111-1111-111111111111-doctor',
    );
    expect(service.correctionId(id, 'PATIENT')).toBe(
      'speaker-11111111-1111-1111-1111-111111111111-patient',
    );
    expect(service.correctionId(id, 'DOCTOR')).toBe(service.correctionId(id, 'DOCTOR'));
  });

  function transcriptItem(): AmbientTranscriptItem {
    return {
      id: '11111111-1111-1111-1111-111111111111',
      sequence: 1,
      sourceEventId: 'chunk-1:seg-1',
      source: 'AMBIENT_DIARIZED',
      speakerType: 'UNSPECIFIED',
      speakerLabel: 'A',
      text: 'Verbatim clinique inchangé',
      locale: 'fr',
      startOffsetMs: 1_000,
      endOffsetMs: 2_000,
      status: 'FINAL',
      supersedesItemId: null,
      createdAt: '2026-07-26T17:00:00Z',
    };
  }
});
