import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
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

  it('should keep patient detail navigation out of the global sidebar', () => {
    effectiveAccess.set({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: [
        'PATIENT_READ',
        'CLINICAL_READ',
        'LAB_ORDER_READ',
        'HOSPITALIZATION_READ',
        'AUDIT_READ',
      ],
    });
    fixture.componentRef.setInput('session', {
      role: 'MEDECIN',
      name: 'Médecin Test',
    });
    fixture.detectChanges();

    const items = fixture.componentInstance.menuItems();
    const paths = items.map((item) => item.path);

    expect(paths.filter((path) => path === '/patients')).toHaveLength(1);
    expect(paths.some((path) => /^\/patients\/[^/]+\/(profile|consultations|lab-orders|hospitalizations|audit-trail)$/.test(path))).toBe(false);
    expect(items.some((item) => item.indent)).toBe(false);
  });

  it('should group facility and bed-capacity links with task-oriented labels', () => {
    effectiveAccess.set({
      userId: 'admin-1',
      roles: ['ADMIN_CLINIQUE'],
      permissions: ['ORGANIZATION_STRUCTURE_MANAGE', 'HOSPITALIZATION_READ', 'SPATIAL_CONFIGURATION_MANAGE'],
    });
    fixture.componentRef.setInput('session', {
      role: 'ADMIN_CLINIQUE',
      name: 'Administrateur clinique',
    });
    fixture.detectChanges();

    const items = fixture.componentInstance.menuItems();
    const visibleItems = items.filter((item) => !item.isHeader);

    expect(items.map((item) => item.label)).toEqual([
      'menu.dashboard',
      'menu.section.facility',
      'menu.hospitalOrganization',
      'menu.section.capacity',
      'menu.spatial',
      'menu.spatialConfig',
    ]);
    expect(visibleItems.map((item) => item.path)).toEqual([
      '/dashboard',
      '/clinic/hospital-organization',
      '/clinic/spatial',
      '/clinic/spatial/configuration',
    ]);
  });

  it('should not expose billing or the global lab queue to a doctor without their exact permissions', () => {
    effectiveAccess.set({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: ['PATIENT_READ', 'CLINICAL_READ', 'LAB_ORDER_READ'],
    });
    fixture.componentRef.setInput('session', {
      role: 'MEDECIN',
      name: 'Médecin Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).not.toContain('/clinic/billing');
    expect(paths).not.toContain('/clinic/lab-orders');
  });

  it('should expose the personal appointment calendar only with its exact permission', () => {
    effectiveAccess.set({
      userId: 'doctor-1',
      roles: ['MEDECIN'],
      permissions: ['APPOINTMENT_READ_OWN'],
    });
    fixture.componentRef.setInput('session', {
      role: 'MEDECIN',
      name: 'Médecin Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).toContain('/clinic/appointments');
  });

  it('should hide the personal appointment calendar from reception without the exact permission', () => {
    effectiveAccess.set({
      userId: 'reception-1',
      roles: ['AGENT_ACCUEIL'],
      permissions: ['APPOINTMENT_READ', 'APPOINTMENT_WRITE'],
    });
    fixture.componentRef.setInput('session', {
      role: 'AGENT_ACCUEIL',
      name: 'Accueil Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).not.toContain('/clinic/appointments');
  });

  it('should expose workspaces from dynamically granted exact permissions', () => {
    effectiveAccess.set({
      userId: 'custom-1',
      roles: ['ROLE_PERSONNALISE'],
      permissions: ['BILLING_INVOICE_READ', 'PATIENT_READ', 'LAB_QUEUE_READ'],
    });
    fixture.componentRef.setInput('session', {
      role: 'ROLE_PERSONNALISE',
      name: 'Profil dynamique',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).toContain('/clinic/billing');
    expect(paths).toContain('/clinic/lab-orders');
  });

  it('should show only the cashier workspace to a cashier-only profile', () => {
    effectiveAccess.set({
      userId: 'cashier-1',
      roles: ['CAISSIER'],
      permissions: [
        'BILLING_INVOICE_READ',
        'CASH_QUEUE_READ',
        'CASH_PAYMENT_COLLECT',
        'CASH_SESSION_OPEN',
        'CASH_SESSION_CLOSE',
        'CASH_MOVEMENT_WRITE',
        'CASH_HISTORY_READ',
      ],
    });
    fixture.componentRef.setInput('session', {
      role: 'CAISSIER',
      name: 'Caissier Test',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).toContain('/clinic/cashier');
    expect(paths).not.toContain('/clinic/billing');
  });

  it('should show both workspaces when cashier and billing management permissions are combined', () => {
    effectiveAccess.set({
      userId: 'cashier-manager-1',
      roles: ['CAISSIER', 'SECRETAIRE_COMPTABLE'],
      permissions: [
        'CASH_QUEUE_READ',
        'CASH_PAYMENT_COLLECT',
        'BILLING_INVOICE_READ',
        'BILLING_INVOICE_WRITE',
      ],
    });
    fixture.componentRef.setInput('session', {
      role: 'CAISSIER,SECRETAIRE_COMPTABLE',
      name: 'Caissier gestionnaire',
    });
    fixture.detectChanges();

    const paths = fixture.componentInstance.menuItems().map((item) => item.path);

    expect(paths).toContain('/clinic/cashier');
    expect(paths).toContain('/clinic/billing');
  });
});
