import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

describe('AuthApiService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
  });

  it('should restore a session from the refresh cookie when a new tab starts', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    const request = http.expectOne('/api/auth/refresh');
    expect(request.request.method).toBe('POST');
    request.flush({
      accessToken: 'restored-token',
      tokenType: 'Bearer',
      expiresAt: '2026-07-17T20:00:00Z',
      email: 'doctor@joprelys.local',
      name: 'Doctor',
      role: 'MEDECIN',
    });

    await restored;
    expect(storage.accessToken).toBe('restored-token');
  });

  it('should continue anonymously when no refresh cookie is available', async () => {
    const service = TestBed.inject(AuthApiService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    http.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(restored).resolves.toBeUndefined();
  });

  it('should propagate an unexpected refresh failure instead of masking it', async () => {
    const service = TestBed.inject(AuthApiService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    http.expectOne('/api/auth/refresh').flush(
      { detail: 'Session service unavailable' },
      { status: 500, statusText: 'Internal Server Error' },
    );

    await expect(restored).rejects.toMatchObject({ status: 500 });
  });
});
