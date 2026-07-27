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

  it('loads the deterministic source-linked projection', () => {
    service.getProjection('visit 1').subscribe();

    const request = http.expectOne('/api/ai/consultations/visit%201/facts/note-projection');
    expect(request.request.method).toBe('GET');
    request.flush({
      visitId: 'visit 1',
      projectionVersion: 'clinical-note-projection-v1:abc',
      maxFactSequence: 4,
      sections: [],
    });
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
