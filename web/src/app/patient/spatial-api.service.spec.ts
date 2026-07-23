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

  it('loads spaces with the requested organization scope', () => {
    service.listSpaces('org-1', undefined, true).subscribe((result) => {
      expect(result[0].name).toBe('Chambre 201');
      expect(result[0].inpatientProfile).toBe(true);
    });

    const request = http.expectOne((candidate) =>
      candidate.url === '/api/spatial/configuration/spaces'
      && candidate.params.get('organizationId') === 'org-1'
      && candidate.params.get('includeInactive') === 'true');
    expect(request.request.method).toBe('GET');
    request.flush([{
      id: 'space-1',
      locationNodeId: null,
      code: 'CARDIO_201',
      name: 'Chambre 201',
      spaceTypeCode: 'HOSPITAL_ROOM',
      inpatientProfile: true,
      active: true,
    }]);
  });

  it('creates a typed space through the configuration contract', () => {
    const payload = {
      locationNodeId: 'floor-2',
      code: 'CARDIO_201',
      name: 'Chambre 201',
      spaceTypeCode: 'HOSPITAL_ROOM',
      enableInpatientProfile: true,
    };

    service.createSpace(payload, 'org-1').subscribe();

    const request = http.expectOne((candidate) =>
      candidate.url === '/api/spatial/configuration/spaces'
      && candidate.params.get('organizationId') === 'org-1');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      id: 'space-1',
      ...payload,
      inpatientProfile: true,
      active: true,
    });
  });

  it('creates a bed by spaceId, never by roomId', () => {
    const payload = { spaceId: 'space-1', bedNumber: '201-A' };

    service.createBed(payload, 'org-1').subscribe();

    const request = http.expectOne((candidate) =>
      candidate.url === '/api/spatial/configuration/beds'
      && candidate.params.get('organizationId') === 'org-1');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush({
      id: 'bed-1',
      spaceId: 'space-1',
      bedNumber: '201-A',
      status: 'FREE',
      capacityStatus: 'OPEN',
      readinessStatus: 'READY',
      usageStatus: 'UNASSIGNED',
      available: true,
      version: 0,
    });
  });
});
