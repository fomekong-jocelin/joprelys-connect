import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SpatialApiService } from './spatial-api.service';

describe('SpatialApiService configuration', () => {
  let service: SpatialApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), SpatialApiService],
    });
    service = TestBed.inject(SpatialApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the complete hospital structure', () => {
    service.getConfiguration().subscribe((result) => {
      expect(result.wards[0].name).toBe('Cardiologie');
    });

    const request = http.expectOne('/api/spatial/configuration');
    expect(request.request.method).toBe('GET');
    request.flush({ wards: [{ id: 'ward-1', name: 'Cardiologie', rooms: [] }] });
  });

  it('creates a room through the configuration contract', () => {
    const payload = {
      wardId: 'ward-1',
      roomNumber: '201',
      capacity: 2,
      comfortLevel: 'STANDARD',
    };

    service.createRoom(payload, 'org-1').subscribe();

    const request = http.expectOne((candidate) =>
      candidate.url === '/api/spatial/configuration/rooms'
      && candidate.params.get('organizationId') === 'org-1');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({ id: 'room-1', ...payload });
  });
});
