import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { PatientDetailComponent } from './patient-detail.component';
import { PatientApiService } from './patient-api.service';
import { Patient } from './patient.models';

describe('PatientDetailComponent mobile action hierarchy', () => {
  let fixture: ComponentFixture<PatientDetailComponent>;
  let effectivePermissions: ReturnType<typeof signal<Set<string>>>;

  const patient: Patient = {
    id: 'patient-1',
    organizationId: 'org-1',
    globalPatientNumber: 'DPU-001',
    localPatientNumber: 'PAT-001',
    fullName: 'Patient Test',
    gender: 'MASCULIN',
    birthDate: '1985-05-15',
    phone: '+237699999999',
    city: 'Douala',
    status: 'ACTIVE',
    createdAt: '2026-07-05T12:00:00Z',
    updatedAt: '2026-07-05T12:00:00Z',
  };

  beforeEach(async () => {
    effectivePermissions = signal(new Set([
      'VISIT_READ',
      'VISIT_CREATE',
      'CLINICAL_READ',
    ]));

    await TestBed.configureTestingModule({
      imports: [PatientDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthTokenStorageService,
          useValue: {
            session: signal({
              name: 'Dr. Test',
              email: 'doctor@joprelys.local',
              role: 'MEDECIN',
              org: 'org-1',
            }),
            registerSessionBoundaryCleanup: vi.fn(),
          },
        },
        {
          provide: VisitApiService,
          useValue: {
            getActiveVisits: vi.fn().mockReturnValue(of([])),
            create: vi.fn(),
          },
        },
        {
          provide: PatientApiService,
          useValue: {
            getById: vi.fn().mockReturnValue(of(patient)),
            getAllergies: vi.fn().mockReturnValue(of([])),
            downloadSummaryPdf: vi.fn(),
            triggerEmergencyAccess: vi.fn().mockReturnValue(of(undefined)),
          },
        },
        {
          provide: RbacApiService,
          useValue: {
            access: signal(null),
            ensureMyAccess: vi.fn().mockReturnValue(of(null)),
            hasPermission: vi.fn((permission: string) => effectivePermissions().has(permission)),
          },
        },
        {
          provide: I18nService,
          useValue: {
            t: vi.fn((key: string) => key),
            locale: signal('fr'),
            currentLanguage: vi.fn(() => 'fr'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientDetailComponent);
    fixture.componentRef.setInput('patient', patient);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('should put the primary visit action on a full mobile row before secondary actions', () => {
    const actions = fixture.nativeElement.querySelector('[data-testid="patient-header-actions"]');
    const primary = fixture.nativeElement.querySelector('[data-testid="patient-primary-action"]');
    const summary = fixture.nativeElement.querySelector('[data-testid="patient-summary-action"]');
    const back = fixture.nativeElement.querySelector('[data-testid="patient-back-action"]');

    expect(actions?.classList.contains('grid-cols-2')).toBe(true);
    expect(primary?.classList.contains('order-1')).toBe(true);
    expect(primary?.classList.contains('col-span-2')).toBe(true);
    expect(primary?.textContent).toContain('patient.detail.openVisit');
    expect(summary?.classList.contains('order-2')).toBe(true);
    expect(back?.classList.contains('order-3')).toBe(true);
  });

  it('should keep summary permission-gated while retaining the back action', () => {
    effectivePermissions.set(new Set(['VISIT_READ', 'VISIT_CREATE']));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="patient-summary-action"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="patient-back-action"]')).not.toBeNull();
  });
});
