import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuditApiService } from '../../audit/audit-api.service';
import { AuditLog } from '../../audit/audit.models';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientAuditTrailTabComponent } from './patient-audit-trail-tab.component';

describe('PatientAuditTrailTabComponent mobile disclosure', () => {
  let fixture: ComponentFixture<PatientAuditTrailTabComponent>;

  const logs: AuditLog[] = [
    {
      id: 'audit-1',
      actorUserId: 'user-1',
      actorName: 'NOUPOUE Charmande',
      actorOrganizationId: 'org-1',
      patientId: 'patient-1',
      resourceType: 'PATIENT',
      resourceId: 'patient-1',
      action: 'READ_AUDIT',
      reason: "Consultation de l'historique d'audit du patient",
      ipAddress: '143.105.152.106',
      userAgent: 'Chrome Mobile',
      status: 'SUCCESS',
      createdAt: '2026-07-29T14:18:00Z',
    },
    {
      id: 'audit-2',
      actorUserId: 'user-2',
      actorName: 'Second User',
      actorOrganizationId: 'org-1',
      patientId: 'patient-1',
      resourceType: 'PATIENT',
      resourceId: 'patient-1',
      action: 'READ_PATIENT',
      status: 'SUCCESS',
      createdAt: '2026-07-29T13:37:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientAuditTrailTabComponent],
      providers: [
        {
          provide: PatientDetailComponent,
          useValue: { patient: signal({ id: 'patient-1' }) },
        },
        {
          provide: AuditApiService,
          useValue: { getPatientLogs: vi.fn().mockReturnValue(of(logs)) },
        },
        {
          provide: RbacApiService,
          useValue: { hasPermission: vi.fn().mockReturnValue(true) },
        },
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: signal<'fr' | 'en'>('fr'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientAuditTrailTabComponent);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('renders audit events as independent cards without the legacy timeline rail', () => {
    expect(fixture.nativeElement.querySelectorAll('[data-audit-id]').length).toBe(2);
    expect(fixture.nativeElement.querySelector('.flow-root')).toBeNull();
    expect(fixture.nativeElement.querySelector('.h-full.w-0\\.5')).toBeNull();
  });

  it('keeps every audit row collapsed by default', () => {
    expect(fixture.componentInstance.isExpanded('audit-1')).toBe(false);
    expect(fixture.componentInstance.isExpanded('audit-2')).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="patient-audit-details"]')).toBeNull();
  });

  it('opens and closes one audit row without changing the others', () => {
    fixture.componentInstance.toggleLog('audit-1');
    fixture.detectChanges();

    expect(fixture.componentInstance.isExpanded('audit-1')).toBe(true);
    expect(fixture.componentInstance.isExpanded('audit-2')).toBe(false);
    expect(fixture.nativeElement.querySelectorAll('[data-testid="patient-audit-details"]').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('143.105.152.106');
    expect(fixture.nativeElement.textContent).toContain('Chrome Mobile');

    fixture.componentInstance.toggleLog('audit-1');
    fixture.detectChanges();

    expect(fixture.componentInstance.isExpanded('audit-1')).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-testid="patient-audit-details"]')).toBeNull();
  });

  it('formats dates according to the active product locale instead of the browser default', () => {
    const formatted = fixture.componentInstance.formatDateTime('2026-07-29T14:18:00Z');

    expect(formatted).toContain('29/07/2026');
  });
});
