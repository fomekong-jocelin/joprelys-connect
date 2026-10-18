import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthApiService } from '../../auth/auth-api.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { ThemeService } from '../../core/theme/theme.service';
import { PatientPortalService } from '../../patient/portal/services/patient-portal.service';
import { PatientApiService } from '../../patient/patient-api.service';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { DialogService } from '../ui/dialog.service';
import { AppShellComponent } from './app-shell.component';

describe('AppShellComponent', () => {
  let fixture: ComponentFixture<AppShellComponent>;
  let component: AppShellComponent;
  let mockAuthApi: { logout: any };
  let mockDialog: { confirm: any; alert: any };
  let router: Router;

  beforeEach(async () => {
    mockAuthApi = {
      logout: vi.fn().mockReturnValue(of(void 0)),
    };

    mockDialog = {
      confirm: vi.fn(),
      alert: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthApiService, useValue: mockAuthApi },
        {
          provide: AuthTokenStorageService,
          useValue: { session: signal({ role: 'MEDECIN', name: 'Dr Martin' }) },
        },
        {
          provide: I18nService,
          useValue: { t: (key: string) => key, locale: signal('fr') },
        },
        {
          provide: ThemeService,
          useValue: { theme: signal('light') },
        },
        {
          provide: PatientPortalService,
          useValue: { getUnreadNotificationCount: vi.fn().mockReturnValue(of(0)) },
        },
        {
          provide: PatientApiService,
          useValue: { getPendingPreRegistrations: vi.fn().mockReturnValue(of({ totalElements: 0 })) },
        },
        {
          provide: RbacApiService,
          useValue: {
            access: signal({ userId: 'u1', roles: ['MEDECIN'], permissions: [] }),
            ensureMyAccess: vi.fn().mockReturnValue(of(null)),
            hasPermission: vi.fn().mockReturnValue(true),
          },
        },
        { provide: DialogService, useValue: mockDialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('prompts confirmation when logout is requested, and aborts if dismissed', async () => {
    mockDialog.confirm.mockResolvedValue(false);

    await component.logout();

    expect(mockDialog.confirm).toHaveBeenCalledWith({
      title: 'shell.logout.confirmTitle',
      message: 'shell.logout.confirmMessage',
      confirmLabel: 'shell.logout.confirmAction',
      cancelLabel: 'common.cancel',
      tone: 'warning',
    });
    expect(mockAuthApi.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('proceeds with logout and navigation when confirmed', async () => {
    mockDialog.confirm.mockResolvedValue(true);

    await component.logout();

    expect(mockDialog.confirm).toHaveBeenCalled();
    expect(mockAuthApi.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });
});
