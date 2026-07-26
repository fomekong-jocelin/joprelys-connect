import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthSessionKeepAliveService } from './auth-session-keep-alive.service';
import { AuthSessionRecoveryService } from './auth-session-recovery.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

describe('AuthSessionKeepAliveService', () => {
  const tokenStorage = {
    session: vi.fn(),
    isExpired: vi.fn(),
    isPatientSession: vi.fn(),
  };
  const recovery = {
    refreshAccessToken: vi.fn().mockReturnValue(of('fresh-token')),
  };

  beforeEach(() => {
    vi.useFakeTimers();
    tokenStorage.session.mockReset();
    tokenStorage.isExpired.mockReset();
    tokenStorage.isPatientSession.mockReset().mockReturnValue(false);
    recovery.refreshAccessToken.mockReset().mockReturnValue(of('fresh-token'));

    TestBed.configureTestingModule({
      providers: [
        AuthSessionKeepAliveService,
        { provide: AuthTokenStorageService, useValue: tokenStorage },
        { provide: AuthSessionRecoveryService, useValue: recovery },
      ],
    });
  });

  afterEach(() => {
    TestBed.inject(AuthSessionKeepAliveService).stop();
    vi.useRealTimers();
  });

  it('should not refresh a healthy token on every keep-alive check', () => {
    tokenStorage.session.mockReturnValue({ accessToken: 'healthy-token', role: 'MEDECIN' });
    tokenStorage.isExpired.mockReturnValue(false);

    TestBed.inject(AuthSessionKeepAliveService).start();

    expect(tokenStorage.isExpired).toHaveBeenCalledWith(120);
    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
  });

  it('should refresh only when the professional token enters the final two-minute window', () => {
    tokenStorage.session.mockReturnValue({ accessToken: 'expiring-token', role: 'MEDECIN' });
    tokenStorage.isExpired.mockReturnValue(true);

    TestBed.inject(AuthSessionKeepAliveService).start();

    expect(recovery.refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(recovery.refreshAccessToken).toHaveBeenCalledWith('expiring-token');
  });

  it('should never exchange an expiring patient JWT against the professional refresh cookie', () => {
    tokenStorage.session.mockReturnValue({ accessToken: 'patient-token', role: 'PATIENT' });
    tokenStorage.isPatientSession.mockReturnValue(true);
    tokenStorage.isExpired.mockReturnValue(true);

    TestBed.inject(AuthSessionKeepAliveService).start();

    expect(tokenStorage.isExpired).not.toHaveBeenCalled();
    expect(recovery.refreshAccessToken).not.toHaveBeenCalled();
  });
});
