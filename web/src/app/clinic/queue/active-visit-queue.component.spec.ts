import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { Visit } from '../../visit/visit.models';
import { VisitApiService } from '../../visit/visit-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { ActiveVisitQueueComponent } from './active-visit-queue.component';

describe('ActiveVisitQueueComponent', () => {
  let fixture: ComponentFixture<ActiveVisitQueueComponent>;
  let component: ActiveVisitQueueComponent;
  let mockVisitApi: Record<string, ReturnType<typeof vi.fn>>;

  const translations: Record<string, string> = {
    'queue.summary.v2.active': 'Actives',
    'queue.summary.v2.waitingVitals': 'Attente constantes',
    'queue.summary.v2.ready': 'Prêts',
    'queue.summary.v2.inConsultation': 'En consultation',
    'queue.summary.v2.critical': 'Alertes critiques',
    'queue.summary.v2.maxWait': 'Attente max',
  };

  beforeEach(async () => {
    mockVisitApi = {
      getActiveVisits: vi.fn().mockReturnValue(of([])),
      closeVisit: vi.fn().mockReturnValue(of({})),
      takeCharge: vi.fn().mockReturnValue(of({})),
      releaseCharge: vi.fn().mockReturnValue(of({})),
    };

    await TestBed.configureTestingModule({
      imports: [ActiveVisitQueueComponent],
      providers: [
        provideRouter([]),
        { provide: VisitApiService, useValue: mockVisitApi },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => translations[key] ?? fallback ?? key,
            locale: signal('fr'),
          },
        },
        {
          provide: RbacApiService,
          useValue: {
            access: signal({ userId: 'doctor-1', roles: ['MEDECIN'], permissions: [] }),
            hasPermission: () => true,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveVisitQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('should load the whole queue by default and reload on scope change', () => {
    expect(mockVisitApi['getActiveVisits']).toHaveBeenCalledWith('ALL');

    component.setScope('MINE');

    expect(mockVisitApi['getActiveVisits']).toHaveBeenLastCalledWith('MINE');
  });

  it('should order the queue by arrival time rather than creation time', () => {
    mockVisitApi['getActiveVisits'].mockReturnValueOnce(of([
      visit({ id: 'late', createdAt: '2026-07-29T16:20:00Z', arrivalAt: '2026-07-29T16:40:00Z' }),
      visit({ id: 'early', createdAt: '2026-07-29T16:50:00Z', arrivalAt: '2026-07-29T16:10:00Z' }),
    ]));

    component.loadQueue();

    expect(component.activeVisits().map((item) => item.id)).toEqual(['early', 'late']);
  });

  it('should summarize the queue by care stage and critical alerts', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-29T17:00:00Z'));
    component.activeVisits.set([
      visit({ id: 'a', createdAt: '2026-07-29T16:30:00Z', careStage: 'ATTENTE_CONSTANTES' }),
      visit({
        id: 'b',
        createdAt: '2026-07-29T16:45:00Z',
        careStage: 'PRET_MEDECIN',
        vitals: { spo2: 88, alerts: [{ code: 'SPO2_LOW', severity: 'CRITICAL' }] },
      }),
      visit({ id: 'c', createdAt: '2026-07-29T16:50:00Z', careStage: 'EN_CONSULTATION' }),
    ]);

    expect(component.queueSummaryLabel()).toBe(
      'Actives : 3 · Attente constantes : 1 · Prêts : 1 · En consultation : 1'
      + ' · Alertes critiques : 1 · Attente max : 30 min',
    );
  });

  it('should take charge on the server before opening the consultation', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    component.startConsultation(visit({ id: 'visit-9' }));

    expect(mockVisitApi['takeCharge']).toHaveBeenCalledWith('visit-9', false);
    expect(navigate).toHaveBeenCalledWith(['/clinic/consultation', 'visit-9']);
  });

  it('should explain why a patient held by a colleague cannot be opened', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate');
    mockVisitApi['takeCharge'].mockReturnValueOnce(
      throwError(() => ({ error: { detail: 'Patient déjà en consultation chez Dr Alpha.' } })),
    );

    component.startConsultation(visit({ id: 'visit-9' }));

    expect(component.actionError()).toBe('Patient déjà en consultation chez Dr Alpha.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('should reload the queue after saving vitals', () => {
    component.openVitalsModal(visit({ id: 'visit-3' }));
    expect(component.selectedVisitForVitals()?.id).toBe('visit-3');

    component.onVitalsSaved();

    expect(component.selectedVisitForVitals()).toBeNull();
    expect(mockVisitApi['getActiveVisits']).toHaveBeenCalledTimes(2);
  });

  function visit(overrides: Partial<Visit>): Visit {
    return {
      id: 'visit-default',
      visitNumber: 'VIS-001',
      patientId: 'patient-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      reason: 'Motif',
      orientation: 'CONSULTATION',
      status: 'EN_COURS',
      createdAt: '2026-07-29T16:00:00Z',
      ...overrides,
    };
  }
});
