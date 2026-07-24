import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { EmergencyApiService } from './emergency-api.service';
import { EmergencyDashboardComponent } from './emergency-dashboard.component';
import { EmergencyRecord } from './emergency.models';

@Component({ selector: 'app-shell', standalone: true, template: '<ng-content />' })
class AppShellStubComponent {}

describe('EmergencyDashboardComponent', () => {
  let fixture: ComponentFixture<EmergencyDashboardComponent>;
  let component: EmergencyDashboardComponent;
  let router: Router;
  let permissions: ReturnType<typeof signal<Set<string>>>;
  let emergencyApi: {
    getActive: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
    addResuscitationLog: ReturnType<typeof vi.fn>;
    stabilize: ReturnType<typeof vi.fn>;
  };

  const record: EmergencyRecord = {
    id: 'emergency-1',
    organizationId: 'organization-1',
    patientId: 'source-1',
    patientName: 'Patient provisoire',
    globalPatientNumber: 'DPU-TEMP-1',
    localPatientNumber: 'PAT-TEMP-1',
    temporaryPatientNumber: 'URG-TEMP-20260721-000001',
    identityStatus: 'PROVISIONAL_URGENCY',
    identityConfidenceLevel: 'NONE',
    apparentGender: 'MASCULIN',
    estimatedAgeRange: '35-45',
    physicalDescription: 'Cicatrice frontale',
    foundAt: '2026-07-21T10:00:00Z',
    foundLocation: 'Akwa',
    arrivalMode: 'AMBULANCE',
    triageLevel: 'RED',
    hemodynamicStatus: 'SHOCK',
    chiefComplaint: 'Altération de la conscience',
    initialBpSystolic: 90,
    initialBpDiastolic: 60,
    initialHr: 120,
    initialTemp: 38,
    thirdPartyRecorded: false,
    resuscitationLogs: [],
    createdAt: '2026-07-21T10:00:00Z',
    updatedAt: '2026-07-21T10:00:00Z',
  };

  beforeEach(async () => {
    emergencyApi = {
      getActive: vi.fn().mockReturnValue(of([record])),
      getById: vi.fn().mockReturnValue(of(record)),
      addResuscitationLog: vi.fn().mockReturnValue(of({})),
      stabilize: vi.fn().mockReturnValue(of({ ...record, stabilizedAt: '2026-07-21T10:30:00Z' })),
    };
    permissions = signal(new Set(['EMERGENCY_READ', 'EMERGENCY_WRITE', 'PATIENT_WRITE']));

    await TestBed.configureTestingModule({
      imports: [EmergencyDashboardComponent],
      providers: [
        provideRouter([]),
        { provide: EmergencyApiService, useValue: emergencyApi },
        {
          provide: RbacApiService,
          useValue: {
            hasPermission: (permission: string) => permissions().has(permission),
          },
        },
        { provide: I18nService, useValue: { t: (_key: string, fallback?: string) => fallback ?? _key } },
      ],
    })
      .overrideComponent(EmergencyDashboardComponent, {
        remove: { imports: [AppShellComponent] },
        add: { imports: [AppShellStubComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(EmergencyDashboardComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('renders the five-section URG-TEMP workspace', () => {
    component.openDrawer(record);
    fixture.detectChanges();

    const workspaceNavigation = fixture.nativeElement.querySelector('aside nav');
    const buttons = workspaceNavigation?.querySelectorAll('button') ?? [];
    expect(buttons.length).toBe(5);
    expect(workspaceNavigation?.textContent).toContain('Documents');
  });

  it('does not expose or open emergency admission without EMERGENCY_WRITE', () => {
    permissions.set(new Set(['EMERGENCY_READ']));
    fixture.detectChanges();

    expect(component.canCreateEmergency()).toBe(false);
    component.openAdmissionModal();
    expect(component.isAdmissionModalOpen()).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('emergency.admissionBtn');
  });

  it('allows emergency admission when EMERGENCY_WRITE is granted', () => {
    expect(component.canCreateEmergency()).toBe(true);
    component.openAdmissionModal();
    fixture.detectChanges();
    expect(component.isAdmissionModalOpen()).toBe(true);
  });

  it('opens the canonical continuation route with the source emergency', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.continueToHospitalization(record);

    expect(navigate).toHaveBeenCalledWith(
      ['/patients', 'source-1', 'hospitalizations'],
      { queryParams: { emergencyId: 'emergency-1' } },
    );
  });

  it('opens the reconciliation workspace preselected with the emergency context', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.openReconciliation(record);

    expect(navigate).toHaveBeenCalledWith(
      ['/clinic/patient-reconciliation'],
      { queryParams: { patientId: 'source-1', emergencyId: 'emergency-1' } },
    );
  });

  it('continues directly to hospitalization after an admission orientation', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.selectedEmergency.set(record);
    component.stabilizeOrientation.set('ADMISSION');

    component.onSubmitStabilize();

    expect(emergencyApi.stabilize).toHaveBeenCalledWith('emergency-1', 'ADMISSION');
    expect(navigate).toHaveBeenCalledWith(
      ['/patients', 'source-1', 'hospitalizations'],
      { queryParams: { emergencyId: 'emergency-1' } },
    );
  });
});
