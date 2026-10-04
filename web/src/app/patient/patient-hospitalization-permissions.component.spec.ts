import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { BedConfiguration } from '../clinic/spatial/spatial-configuration.models';
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
    roomNumber: 'Chambre 201',
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
          useValue: { listForPatient: () => of([]), practitioners: () => of([]), admissionVisits: () => of([]), placementBeds: () => of([]), placementOptions: () => of({ units: [], serviceCatalog: [], spaces: [], assignments: [], staff: [] }), admit: vi.fn().mockReturnValue(of({})) },
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

  it('only exposes active physicians assigned to the selected hospitalization service', () => {
    component.staffList.set([
      {
        id: 'doctor-med',
        email: 'med@example.com',
        displayName: 'Dr Médecine',
        role: 'MEDECIN',
        enabled: true,
        createdAt: '2026-08-09T00:00:00Z',
        activeOrganizationalUnits: [{
          id: 'unit-med',
          code: 'MED',
          nameFr: 'Médecine',
          nameEn: 'Medicine',
          primary: true,
        }],
      },
      {
        id: 'doctor-surgery',
        email: 'surgery@example.com',
        displayName: 'Dr Chirurgie',
        role: 'MEDECIN',
        enabled: true,
        createdAt: '2026-08-09T00:00:00Z',
        activeOrganizationalUnits: [{
          id: 'unit-surgery',
          code: 'SURG',
          nameFr: 'Chirurgie',
          nameEn: 'Surgery',
          primary: true,
        }],
      },
      {
        id: 'admin-med',
        email: 'admin@example.com',
        displayName: 'Administration',
        role: 'ADMIN_CLINIQUE',
        enabled: true,
        createdAt: '2026-08-09T00:00:00Z',
        activeOrganizationalUnits: [{
          id: 'unit-med',
          code: 'MED',
          nameFr: 'Médecine',
          nameEn: 'Medicine',
          primary: true,
        }],
      },
    ]);
    component.selectedUnitId.set('unit-med');

    expect(component.eligiblePractitioners().map((practitioner) => practitioner.id)).toEqual(['doctor-med']);
  });

  it('does not expose admission to a nurse-style profile without HOSPITALIZATION_ADMIT', () => {
    grantedPermissions.add('HOSPITALIZATION_NOTE_WRITE');
    grantedPermissions.add('HOSPITALIZATION_CARE_WRITE');
    grantedPermissions.add('HOSPITALIZATION_MEDICATION_ADMINISTER');
    grantedPermissions.add('HOSPITALIZATION_CONSUMABLE_RECORD');

    expect(component.canModify()).toBe(false);
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_ADMIT');
  });
  it('keeps a loading failure distinct from an empty history and allows retry', () => {
    grantedPermissions.add('HOSPITALIZATION_ADMIT');
    const api = TestBed.inject(HospitalizationLocationApiService);
    const list = vi.spyOn(api, 'listForPatient').mockReturnValueOnce(throwError(() => new Error('offline')));
    component.loadHospitalizations();
    expect(component.loadError()).toBeTruthy();
    component.openAdmitModal();
    expect(component.showAdmitModal()).toBe(false);
    component.loadHospitalizations();
    expect(component.loadError()).toBeNull();
    expect(list).toHaveBeenCalledTimes(2);
    component.openAdmitModal();
    expect(component.showAdmitModal()).toBe(true);
  });

  it('sends only one admission while a request is pending and releases the form after failure', () => {
    grantedPermissions.add('HOSPITALIZATION_ADMIT');
    const pending = new Subject<StructuredHospitalization>();
    const admit = vi.spyOn(TestBed.inject(HospitalizationLocationApiService), 'admit').mockReturnValue(pending);
    component.selectedUnitId.set('unit-1'); component.selectedBedId.set('bed-1');
    component.freeBeds.set([{ id: 'bed-1', spaceId: 'space-1', bedNumber: 'A', status: 'FREE',
      capacityStatus: 'OPEN', readinessStatus: 'READY', usageStatus: 'UNASSIGNED', available: true, version: 0, roomNumber: '101' }]);
    component.admissionReason = 'Surveillance'; component.visitId = 'visit-1'; component.responsiblePractitionerId = 'doctor-1';
    component.saveAdmission(new Event('submit')); component.saveAdmission(new Event('submit'));
    expect(admit).toHaveBeenCalledOnce(); expect(component.admissionSubmitting()).toBe(true);
    pending.error({ status: 500 });
    expect(component.admissionSubmitting()).toBe(false);
    expect(component.admitError()).toBeTruthy();
  });

  it('ignores an older bed response and waits for the latest request', () => {
    const first = new Subject<BedConfiguration[]>(), latest = new Subject<BedConfiguration[]>();
    vi.spyOn(TestBed.inject(HospitalizationLocationApiService), 'placementBeds')
      .mockReturnValueOnce(first).mockReturnValueOnce(latest);
    component.spaces.set([{ id: 'space-1', code: '101', name: '101', spaceTypeCode: 'HOSPITAL_ROOM', active: true, inpatientProfile: true }]);
    component.assignments.set([{ id: 'assignment-1', organizationalUnitId: 'unit-1', spaceId: 'space-1', validFrom: '2026-01-01T00:00:00Z' }]);
    component.selectedUnitId.set('unit-1');
    component.onAdmissionWardChange(); component.onAdmissionWardChange();
    first.next([]); first.complete();
    expect(component.bedsLoading()).toBe(true);
    latest.next([]); latest.complete();
    expect(component.bedsLoading()).toBe(false);
  });
});
