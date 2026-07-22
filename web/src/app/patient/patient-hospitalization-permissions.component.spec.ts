import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientHospitalizationComponent } from './patient-hospitalization.component';
import { PatientApiService } from './patient-api.service';
import { Hospitalization } from './patient.models';
import { SpatialApiService } from './spatial-api.service';

describe('PatientHospitalizationComponent permissions', () => {
  let component: PatientHospitalizationComponent;
  let grantedPermissions: Set<string>;
  let hasPermission: ReturnType<typeof vi.fn>;

  const activeStay: Hospitalization = {
    id: 'stay-1',
    patientId: 'patient-1',
    organizationId: 'organization-1',
    visitId: 'visit-1',
    responsiblePractitionerId: 'doctor-1',
    serviceName: 'Médecine',
    roomNumber: '201',
    bedNumber: 'A',
    admissionReason: 'Surveillance',
    status: 'EN_COURS',
    admittedAt: '2026-07-22T08:00:00Z',
    hospitalizationNumber: 'HOS-001',
    version: 0,
  };

  beforeEach(async () => {
    grantedPermissions = new Set<string>();
    hasPermission = vi.fn((permission: string) => grantedPermissions.has(permission));

    await TestBed.configureTestingModule({
      imports: [PatientHospitalizationComponent],
      providers: [
        { provide: RbacApiService, useValue: { hasPermission } },
        { provide: I18nService, useValue: { t: (key: string, fallback?: string) => fallback || key } },
        { provide: PatientApiService, useValue: {} },
        { provide: SpatialApiService, useValue: {} },
        { provide: StaffApiService, useValue: { list: () => of([]) } },
      ],
    })
      .overrideComponent(PatientHospitalizationComponent, { set: { template: '' } })
      .compileComponents();

    component = TestBed.createComponent(PatientHospitalizationComponent).componentInstance;
    component.patientId = 'patient-1';
  });

  it('uses the dedicated admission permission when there is no active stay', () => {
    grantedPermissions.add('HOSPITALIZATION_ADMIT');

    expect(component.canModify()).toBe(true);
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_ADMIT');
    expect(hasPermission).not.toHaveBeenCalledWith('HOSPITALIZATION_MANAGE');
  });

  it.each([
    ['notes', 'HOSPITALIZATION_NOTE_WRITE'],
    ['consents', 'HOSPITALIZATION_CONSENT_RECORD'],
    ['cares', 'HOSPITALIZATION_CARE_RECORD'],
    ['meds', 'HOSPITALIZATION_MEDICATION_ADMINISTER'],
    ['consumptions', 'HOSPITALIZATION_CONSUMABLE_RECORD'],
    ['cro', 'CLINICAL_WRITE'],
  ])('maps tab %s to permission %s', (tab, permission) => {
    component.list.set([activeStay]);
    component.activeTab = tab;
    grantedPermissions.add(permission);
    hasPermission.mockClear();

    expect(component.canModify()).toBe(true);
    expect(hasPermission).toHaveBeenCalledWith(permission);
    expect(hasPermission).not.toHaveBeenCalledWith('HOSPITALIZATION_MANAGE');
  });

  it('does not expose a medication administration action to a note-only profile', () => {
    component.list.set([activeStay]);
    component.activeTab = 'meds';
    grantedPermissions.add('HOSPITALIZATION_NOTE_WRITE');

    expect(component.canModify()).toBe(false);
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_MEDICATION_ADMINISTER');
  });
});
