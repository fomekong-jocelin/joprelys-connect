import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ClinicalNoteProjectionApiService } from './clinical-note-projection-api.service';

describe('ClinicalNoteProjectionApiService', () => {
  let service: ClinicalNoteProjectionApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClinicalNoteProjectionApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ClinicalNoteProjectionApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('extracts new FINAL facts before loading the deterministic projection', () => {
    service.getProjection('visit 1').subscribe();

    const extraction = http.expectOne('/api/ai/consultations/visit%201/facts/extract');
    expect(extraction.request.method).toBe('POST');
    expect(extraction.request.body).toEqual({});
    expect(http.match('/api/ai/consultations/visit%201/facts/note-projection')).toHaveLength(0);

    extraction.flush({
      visitId: 'visit 1',
      processedItems: 1,
      alreadyProcessedItems: 2,
      unspecifiedSpeakerItems: 0,
      candidateCount: 1,
      acceptedCount: 1,
      rejectedCount: 0,
      model: 'test-model',
    });

    const projection = http.expectOne('/api/ai/consultations/visit%201/facts/note-projection');
    expect(projection.request.method).toBe('GET');
    projection.flush({
      visitId: 'visit 1',
      projectionVersion: 'clinical-note-projection-v1:abc',
      maxFactSequence: 4,
      sections: [],
    });
  });

  it('fails closed and never displays a projection when fact extraction fails', () => {
    let status = 0;
    service.getProjection('visit-1').subscribe({ error: error => status = error.status });

    const extraction = http.expectOne('/api/ai/consultations/visit-1/facts/extract');
    extraction.flush(
      { detail: 'AI_CLINICAL_FACT_EXTRACTION_UPSTREAM_FAILED' },
      { status: 503, statusText: 'Service Unavailable' },
    );

    expect(status).toBe(503);
    expect(http.match('/api/ai/consultations/visit-1/facts/note-projection')).toHaveLength(0);
  });

  it('validates exactly the reviewed projection version with an idempotency id', () => {
    service.validateProjection(
      'visit-1',
      '11111111-1111-4111-8111-111111111111',
      'clinical-note-projection-v1:abc',
    ).subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/facts/note-projection/validations');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      validationId: '11111111-1111-4111-8111-111111111111',
      projectionVersion: 'clinical-note-projection-v1:abc',
    });
    request.flush({
      id: 'validation-row-1',
      validationId: '11111111-1111-4111-8111-111111111111',
      visitId: 'visit-1',
      projectionVersion: 'clinical-note-projection-v1:abc',
      projectionSchemaVersion: 'clinical-note-projection-v1',
      maxFactSequence: 4,
      validatedByUserId: 'doctor-1',
      validatedAt: '2026-07-27T16:00:00Z',
      facts: [],
    });
  });

  it('loads validation history for audit and current-version status', () => {
    service.getValidationHistory('visit-1').subscribe();

    const request = http.expectOne('/api/ai/consultations/visit-1/facts/note-projection/validations');
    expect(request.request.method).toBe('GET');
    request.flush({ visitId: 'visit-1', validations: [] });
  });
});
