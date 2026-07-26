import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';

describe('RealtimeClinicalIntakeApiService', () => {
  let service: RealtimeClinicalIntakeApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RealtimeClinicalIntakeApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(RealtimeClinicalIntakeApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('should send transcript confidence event and item provenance to the durable intake endpoint', () => {
    service.ingest(
      'visit-1',
      'Patient sans fièvre',
      0.91,
      'event-42',
      'item-9',
    ).subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/realtime-intake');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      transcript: 'Patient sans fièvre',
      confidence: 0.91,
      eventId: 'event-42',
      itemId: 'item-9',
    });
    request.flush({
      id: 'ack-1',
      visitId: 'visit-1',
      sequence: 1,
      eventId: 'event-42',
      itemId: 'item-9',
      transcript: 'Patient sans fièvre',
      confidence: 0.91,
      receivedAt: '2026-07-26T18:00:00Z',
    });
  });
});
