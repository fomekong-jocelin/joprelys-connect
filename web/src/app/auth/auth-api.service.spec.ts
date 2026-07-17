import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginResponse } from './auth.models';

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

  it('should keep a valid session without calling the refresh endpoint', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(loginResponse('valid-token', '2999-07-17T20:00:00Z'));

    await expect(firstValueFrom(service.restoreSession())).resolves.toBeUndefined();
    http.expectNone('/api/auth/refresh');
    expect(storage.accessToken).toBe('valid-token');
  });

  it('should restore a session from the refresh cookie when a new tab starts', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    const restored = firstValueFrom(service.restoreSession());
    const request = http.expectOne('/api/auth/refresh');
    expect(request.request.method).toBe('POST');
    request.flush(loginResponse('restored-token', '2999-07-17T20:00:00Z'));

    await restored;
    expect(storage.accessToken).toBe('restored-token');
  });

  it('should replace an expired stored token from the refresh cookie', async () => {
    const service = TestBed.inject(AuthApiService);
    const storage = TestBed.inject(AuthTokenStorageService);
    const http = TestBed.inject(HttpTestingController);

    storage.save(loginResponse('expired-token', '2020-01-01T00:00:00Z'));

    const restored = firstValueFrom(service.restoreSession());
    expect(storage.session()).toBeNull();
    http.expectOne('/api/auth/refresh')
      .flush(loginResponse('renewed-token', '2999-07-17T20:00:00Z'));

    await restored;
    expect(storage.accessToken).toBe('renewed-token');
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

function loginResponse(accessToken: string, expiresAt: string): LoginResponse {
  return {
    accessToken,
    tokenType: 'Bearer',
    expiresAt,
    email: 'doctor@joprelys.local',
    name: 'Doctor',
    role: 'MEDECIN',
  };
}
