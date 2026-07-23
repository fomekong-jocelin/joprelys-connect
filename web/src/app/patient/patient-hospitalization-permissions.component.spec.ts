import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { HospitalOrganizationApiService } from '../clinic/hospital-organization/hospital-organization-api.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { HospitalizationLocationApiService } from './hospitalization-location-api.service';
import { StructuredHospitalization } from './hospitalization-location.models';
import { PatientHospitalizationComponent } from './patient-hospitalization.component';
import { PatientApiService } from './patient-api.service';
import { SpatialApiService } from './spatial-api.service';

describe('PatientHospitalizationComponent permissions', () => {
  let component: PatientHospitalizationComponent;
  let grantedPermissions: Set<string>;
  let hasPermission: ReturnType<typeof vi.fn>;

  const activeStay: StructuredHospitalization = {
    id: 'stay-1',
    patientId: 'patient-1',
    organizationId: 'organization-1',
    version: 0,
    currentServiceUnitId: 'unit-1',
    currentSpaceId: 'space-201',
    currentBedId: 'bed-a',
    serviceName: 'Médecine',
    spaceName: 'Chambre 201',
    bedNumber: 'A',
    admissionReason: 'Surveillance',
    status: 'EN_COURS',
    admittedAt: '2026-07-22T08:00:00Z',
    hospitalizationNumber: 'HOS-001',
    visitId: 'visit-1',
    responsiblePractitionerId: 'doctor-1',
  };

  beforeEach(async () => {
    grantedPermissions = new Set<string>();
    hasPermission = vi.fn((permission: string) => grantedPermissions.has(permission));

    await TestBed.configureTestingModule({
      imports: [PatientHospitalizationComponent],
      providers: [
        { provide: RbacApiService, useValue: { hasPermission } },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback || key,
            currentLanguage: () => 'fr',
          },
        },
        {
          provide: PatientApiService,
          useValue: {
            getPatientVisits: () => of([]),
            getConsents: () => of([]),
            getOperatingReports: () => of([]),
          },
        },
        {
          provide: HospitalizationLocationApiService,
          useValue: { listForPatient: () => of([]) },
        },
        {
          provide: SpatialApiService,
          useValue: {
            listSpaces: () => of([]),
            listUnitSpaceAssignments: () => of([]),
            listBeds: () => of([]),
          },
        },
        {
          provide: HospitalOrganizationApiService,
          useValue: {
            listUnits: () => of([]),
            listServiceCatalog: () => of([]),
          },
        },
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
    ['cares', 'HOSPITALIZATION_CARE_WRITE'],
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

  it('fails closed when a stale token only contains the removed legacy permission', () => {
    component.list.set([activeStay]);
    component.activeTab = 'cares';
    grantedPermissions.add('HOSPITALIZATION_MANAGE');

    expect(component.canModify()).toBe(false);
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_CARE_WRITE');
    expect(hasPermission).not.toHaveBeenCalledWith('HOSPITALIZATION_MANAGE');
  });

  it('does not expose medication administration to a note-only profile', () => {
    component.list.set([activeStay]);
    component.activeTab = 'meds';
    grantedPermissions.add('HOSPITALIZATION_NOTE_WRITE');

    expect(component.canModify()).toBe(false);
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_MEDICATION_ADMINISTER');
  });

  it('does not expose admission to a nurse-style profile without HOSPITALIZATION_ADMIT', () => {
    grantedPermissions.add('HOSPITALIZATION_NOTE_WRITE');
    grantedPermissions.add('HOSPITALIZATION_CARE_WRITE');
    grantedPermissions.add('HOSPITALIZATION_MEDICATION_ADMINISTER');
    grantedPermissions.add('HOSPITALIZATION_CONSUMABLE_RECORD');

    expect(component.canModify()).toBe(false);
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_ADMIT');
  });
});
