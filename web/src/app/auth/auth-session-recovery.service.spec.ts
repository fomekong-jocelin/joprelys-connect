import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';
import { LoginResponse } from './auth.models';

describe('AuthSessionRecoveryService', () => {
  let service: AuthSessionRecoveryService;
  let storage: AuthTokenStorageService;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: { url: '/consultation/visit-1', navigate: vi.fn().mockResolvedValue(true) },
        },
      ],
    });
    service = TestBed.inject(AuthSessionRecoveryService);
    storage = TestBed.inject(AuthTokenStorageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    service.ngOnDestroy();
    http.verify();
    sessionStorage.clear();
    vi.useRealTimers();
  });

  it('should share one refresh request inside the same tab', async () => {
    storage.save(loginResponse('old-token', '2999-07-26T12:00:00Z'));

    const first = firstValueFrom(service.refreshAccessToken('old-token'));
    const second = firstValueFrom(service.refreshAccessToken('old-token'));

    const request = http.expectOne('/api/auth/refresh');
    request.flush(loginResponse('fresh-token', '2999-07-26T12:15:00Z'));

    await expect(first).resolves.toBe('fresh-token');
    await expect(second).resolves.toBe('fresh-token');
    http.expectNone('/api/auth/refresh');
  });

  it('should retry a 409 concurrent refresh without clearing the clinician session', async () => {
    storage.save(loginResponse('old-token', '2999-07-26T12:00:00Z'));

    const refresh = firstValueFrom(service.refreshAccessToken('old-token'));
    http.expectOne('/api/auth/refresh')
      .flush({ detail: 'AUTH_REFRESH_CONCURRENT' }, { status: 409, statusText: 'Conflict' });

    expect(storage.accessToken).toBe('old-token');
    await vi.advanceTimersByTimeAsync(350);

    const retry = http.expectOne('/api/auth/refresh');
    retry.flush(loginResponse('fresh-token', '2999-07-26T12:15:00Z'));

    await expect(refresh).resolves.toBe('fresh-token');
    expect(storage.accessToken).toBe('fresh-token');
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
