import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ConsultationApiService } from './consultation-api.service';

describe('ConsultationApiService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('should represent an explicit 404 as an empty stream without emitting null', () => {
    const service = TestBed.inject(ConsultationApiService);
    const http = TestBed.inject(HttpTestingController);
    const next = vi.fn();
    const complete = vi.fn();

    service.getConsultation('visit-123').subscribe({ next, complete });

    http.expectOne('/api/visits/visit-123/consultation').flush(
      { detail: 'Aucune consultation trouvée pour cette visite.' },
      { status: 404, statusText: 'Not Found' },
    );

    expect(next).not.toHaveBeenCalled();
    expect(complete).toHaveBeenCalledOnce();
  });

  it('should propagate backend failures instead of masking them', () => {
    const service = TestBed.inject(ConsultationApiService);
    const http = TestBed.inject(HttpTestingController);
    const error = vi.fn();

    service.getConsultation('visit-123').subscribe({ error });

    http.expectOne('/api/visits/visit-123/consultation').flush(
      { detail: 'Database unavailable' },
      { status: 500, statusText: 'Internal Server Error' },
    );

    expect(error).toHaveBeenCalledOnce();
    expect(error.mock.calls[0][0].status).toBe(500);
  });
});
