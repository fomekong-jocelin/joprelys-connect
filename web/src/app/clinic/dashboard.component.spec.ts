import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { RbacApiService } from './rbac/rbac-api.service';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let mockAuthToken: any;
  let mockVisitApi: any;
  let mockI18n: any;
  let mockRbacApi: any;

  const translations: Record<string, string> = {
    'dashboard.greeting': 'Bonjour',
    'dashboard.welcomeSubtitle': 'Votre espace clinique est prêt pour la journée',
    'dashboard.profileLabel': 'Profil',
    'dashboard.queue.summary.active': 'visites actives',
    'dashboard.queue.summary.vitalsPending': 'constantes à saisir',
    'dashboard.queue.summary.vitalsRecorded': 'constantes saisies',
    'dashboard.queue.summary.maxWait': 'attente max',
    'role.MEDECIN': 'Médecin',
  };

  beforeEach(async () => {
    mockAuthToken = {
      session: signal({
        name: 'Jean Medecin',
        email: 'medecin@joprelys.local',
        role: 'MEDECIN',
        org: 'org-1'
      }),
      registerSessionBoundaryCleanup: vi.fn()
    };

    mockVisitApi = {
      getActiveVisits: vi.fn().mockReturnValue(of([])),
      closeVisit: vi.fn(),
      saveVitals: vi.fn().mockReturnValue(of({ weight: 70, height: 175, bmi: 22.86 }))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key: string, fallback?: string) => translations[key] ?? fallback ?? key),
      locale: signal('fr')
    };
    mockRbacApi = {
      access: signal({
        userId: 'doctor-1',
        roles: ['MEDECIN'],
        permissions: ['VISIT_READ', 'VISIT_VITALS_WRITE', 'VISIT_MANAGE', 'CLINICAL_WRITE'],
      }),
      ensureMyAccess: vi.fn().mockReturnValue(of({
        userId: 'doctor-1',
        roles: ['MEDECIN'],
        permissions: ['VISIT_READ', 'VISIT_VITALS_WRITE', 'VISIT_MANAGE', 'CLINICAL_WRITE'],
      })),
      hasPermission: vi.fn().mockImplementation((permission: string) =>
        ['VISIT_READ', 'VISIT_VITALS_WRITE', 'VISIT_MANAGE', 'CLINICAL_WRITE'].includes(permission)
      )
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthTokenStorageService, useValue: mockAuthToken },
        { provide: VisitApiService, useValue: mockVisitApi },
        { provide: I18nService, useValue: mockI18n },
        { provide: RbacApiService, useValue: mockRbacApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
    fixture.destroy();
  });

  it('should delegate the active queue to its dedicated component for clinical roles', () => {
    expect(fixture.nativeElement.querySelector('app-active-visit-queue.mt-10')).toBeTruthy();
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalledTimes(1);
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalledWith('ALL');
  });

  it('should render a warmer localized welcome and a translated business role', () => {
    expect(component.welcomeLabel()).toBe('Bonjour');
    expect(component.authorizedLabel()).toBe('Votre espace clinique est prêt pour la journée');
    expect(component.roleLabel()).toBe('Profil');
    expect(component.session()?.role).toBe('Médecin');

    const header = fixture.nativeElement.querySelector('.app-container > .mb-8');
    expect(header?.textContent).toContain('Bonjour, Jean Medecin');
    expect(header?.textContent).toContain('Médecin');
  });

  it('should keep the existing queue visually before module cards without a second queue request', () => {
    const componentStyles = ((DashboardComponent as any).ɵcmp.styles as string[]).join('\n');

    expect(componentStyles).toMatch(/\.mt-10[^\{]*\{[^}]*order:\s*2;/s);
    expect(componentStyles).toMatch(/\.grid[^\{]*\{[^}]*order:\s*3;/s);
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalledTimes(1);
  });

  it('should manage audit security modal state', () => {
    expect(component.showAuditSecurityModal()).toBe(false);
    component.openAuditSecurityModal();
    expect(component.showAuditSecurityModal()).toBe(true);
    component.closeAuditSecurityModal();
    expect(component.showAuditSecurityModal()).toBe(false);
  });
});
