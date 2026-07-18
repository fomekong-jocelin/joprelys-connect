import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { ActivePatientService } from '../../patient/active-patient.service';
import { PatientApiService } from '../../patient/patient-api.service';
import { AppShellNavComponent } from './app-shell-nav.component';

describe('AppShellNavComponent patient isolation', () => {
  let fixture: ComponentFixture<AppShellNavComponent>;
  let effectiveAccess: ReturnType<typeof signal<{
    userId: string;
    roles: string[];
    permissions: string[];
  } | null>>;

  beforeEach(async () => {
    effectiveAccess = signal({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: ['AVAILABILITY_MANAGE', 'PATIENT_READ', 'PATIENT_WRITE'],
    });

    await TestBed.configureTestingModule({
      imports: [AppShellNavComponent],
      providers: [
        provideRouter([]),
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: ActivePatientService, useValue: { patient: signal(null) } },
        {
          provide: PatientApiService,
          useValue: { getPendingPreRegistrations: vi.fn().mockReturnValue(of({ totalElements: 0 })) },
        },
        {
          provide: RbacApiService,
          useValue: {
            access: effectiveAccess,
            ensureMyAccess: vi.fn().mockReturnValue(of(effectiveAccess())),
            hasPermission: vi.fn().mockReturnValue(true),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellNavComponent);
  });

  afterEach(() => fixture.destroy());

  it('should expose only patient routes even when professional permissions remain cached', () => {
    fixture.componentRef.setInput('session', {
      role: 'PATIENT',
      name: 'Patient Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).toContain('/patient/appointments');
    expect(paths).not.toContain('/clinic/availability');
    expect(paths.filter(Boolean).every((path) => path.startsWith('/patient/'))).toBe(true);
  });

  it('should keep patient mode exclusive for a mixed role value', () => {
    fixture.componentRef.setInput('session', {
      role: 'PATIENT,MEDECIN',
      name: 'Patient Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).not.toContain('/dashboard');
    expect(paths).not.toContain('/clinic/availability');
    expect(paths).toContain('/patient/dashboard');
  });

  it('should not derive professional menu access from the local JWT role alone', () => {
    effectiveAccess.set(null);
    fixture.componentRef.setInput('session', {
      role: 'MEDECIN',
      name: 'Médecin Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).toEqual(['/dashboard']);
    expect(paths).not.toContain('/patients');
    expect(paths).not.toContain('/clinic/availability');
    expect(paths).not.toContain('/clinic/emergencies');
  });
});
