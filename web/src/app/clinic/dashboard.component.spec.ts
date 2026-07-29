import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { RbacApiService } from './rbac/rbac-api.service';
import { Visit } from '../visit/visit.models';

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

  it('should load active visits on init if clinical role', () => {
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalledTimes(1);
    expect(component.isLoadingQueue()).toBe(false);
    expect(component.activeVisits()).toEqual([]);
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

  it('should summarize the real active queue without inventing additional statuses', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-29T17:00:00Z'));

    component.activeVisits.set([
      visit({ id: 'visit-1', createdAt: '2026-07-29T16:30:00Z' }),
      visit({
        id: 'visit-2',
        createdAt: '2026-07-29T16:45:00Z',
        vitals: { temperature: 37 },
      }),
    ]);

    expect(component.queueWithoutVitalsCount()).toBe(1);
    expect(component.queueWithVitalsCount()).toBe(1);
    expect(component.queueSummaryLabel()).toBe(
      '2 visites actives · 1 constantes à saisir · 1 constantes saisies · attente max 30 min',
    );
    expect(component.t('dashboard.queue.desc')).toBe(component.queueSummaryLabel());
  });

  it('should prioritize arrivalAt over creation time when ordering the existing queue', () => {
    const laterCreatedButEarlierArrival = visit({
      id: 'visit-early-arrival',
      createdAt: '2026-07-29T16:50:00Z',
      arrivalAt: '2026-07-29T16:10:00Z',
    });
    const earlierCreatedButLaterArrival = visit({
      id: 'visit-late-arrival',
      createdAt: '2026-07-29T16:20:00Z',
      arrivalAt: '2026-07-29T16:40:00Z',
    });
    mockVisitApi.getActiveVisits.mockReturnValueOnce(
      of([earlierCreatedButLaterArrival, laterCreatedButEarlierArrival]),
    );

    component.loadQueue();

    expect(component.activeVisits().map((item) => item.id)).toEqual([
      'visit-early-arrival',
      'visit-late-arrival',
    ]);
  });

  it('should calculate BMI correctly when weight and height are provided', () => {
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBe(22.86);
  });

  it('should return null BMI if height or weight is missing', () => {
    component.vitalsWeight = undefined;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBeNull();

    component.vitalsWeight = 70;
    component.vitalsHeight = undefined;
    expect(component.computedBmi).toBeNull();
  });

  it('should save vitals for the selected active visit and reload the queue', () => {
    component.openVitalsModal({
      id: 'visit-1',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Fièvre',
      orientation: 'Tri',
      status: 'EN_COURS',
      createdAt: '2026-07-02T08:00:00Z'
    });
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;

    component.submitVitals();

    expect(mockVisitApi.saveVitals).toHaveBeenCalledWith('visit-1', expect.objectContaining({
      weight: 70,
      height: 175
    }));
    expect(component.showVitalsModal()).toBe(false);
    expect(component.selectedVisitForVitals()).toBeNull();
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalledTimes(2);
  });

  it('should validate pain scale values correctly', () => {
    component.vitalsPain = undefined;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = 0;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = 5;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = 10;
    expect(component.isPainInvalid()).toBe(false);

    component.vitalsPain = -1;
    expect(component.isPainInvalid()).toBe(true);

    component.vitalsPain = 11;
    expect(component.isPainInvalid()).toBe(true);
  });

  it('should submit pain scale with other vitals', () => {
    component.openVitalsModal({
      id: 'visit-1',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Fièvre',
      orientation: 'Tri',
      status: 'EN_COURS',
      createdAt: '2026-07-02T08:00:00Z'
    });
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    component.vitalsPain = 6;

    component.submitVitals();

    expect(mockVisitApi.saveVitals).toHaveBeenCalledWith('visit-1', expect.objectContaining({
      weight: 70,
      height: 175,
      painScale: 6
    }));
  });

  it('should manage audit security modal state', () => {
    expect(component.showAuditSecurityModal()).toBe(false);
    component.openAuditSecurityModal();
    expect(component.showAuditSecurityModal()).toBe(true);
    component.closeAuditSecurityModal();
    expect(component.showAuditSecurityModal()).toBe(false);
  });

  function visit(overrides: Partial<Visit>): Visit {
    return {
      id: 'visit-default',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Motif',
      orientation: 'Médecine générale',
      status: 'EN_COURS',
      createdAt: '2026-07-29T16:00:00Z',
      ...overrides,
    };
  }
});
