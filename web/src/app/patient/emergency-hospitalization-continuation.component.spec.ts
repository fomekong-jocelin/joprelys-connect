import { readFileSync } from 'node:fs';
const dictionary: Record<string, string> = JSON.parse(readFileSync('src/assets/i18n/features/hospital-continuity/fr.json', 'utf8'));
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError, forkJoin, Observable } from 'rxjs';
import { HospitalOrganizationApiService } from '../clinic/hospital-organization/hospital-organization-api.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyDocumentApiService } from '../emergency/document/emergency-document-api.service';
import { EmergencyHospitalizationContinuationComponent } from './emergency-hospitalization-continuation.component';
import { HospitalizationLocationApiService } from './hospitalization-location-api.service';
import { SpatialApiService } from './spatial-api.service';

describe('EmergencyHospitalizationContinuationComponent', () => {
  let fixture: ComponentFixture<EmergencyHospitalizationContinuationComponent>;
  let hospitalizationApi: { admit: ReturnType<typeof vi.fn<(...args: unknown[]) => unknown>>; placementOptions?: ReturnType<typeof vi.fn>; placementBeds?: ReturnType<typeof vi.fn> };
  let documentApi: { generateBundle: ReturnType<typeof vi.fn> };
  let rbacApi: { hasPermission: ReturnType<typeof vi.fn> };
  let spatialApi: Record<string, ReturnType<typeof vi.fn>>;
  let hospitalOrganizationApi: Record<string, ReturnType<typeof vi.fn>>;
  let staffApi: { list: ReturnType<typeof vi.fn> };

  const configureInputs = (): void => {
    fixture.componentRef.setInput('patientId', 'patient-1');
    fixture.componentRef.setInput('emergencyId', 'emergency-1');
    fixture.componentRef.setInput('identityStatus', 'PROVISIONAL_URGENCY');
    fixture.componentRef.setInput('temporaryPatientNumber', 'URG-TEMP-20260721-000001');
    fixture.detectChanges();
  };

  beforeEach(async () => {
    hospitalizationApi = {
      admit: vi.fn().mockReturnValue(of({ id: 'stay-1' })),
    };
    documentApi = {
      generateBundle: vi.fn().mockReturnValue(of([{ id: 'doc-1' }])),
    };
    rbacApi = {
      hasPermission: vi.fn().mockImplementation((permission: string) => permission === 'HOSPITALIZATION_ADMIT'),
    };
    hospitalOrganizationApi = {
      listUnits: vi.fn().mockReturnValue(of([
        {
          id: 'unit-admin',
          organizationId: 'org-1',
          parentId: null,
          code: 'CAISSE',
          name: 'Caisse',
          unitType: 'DEPARTMENT',
          serviceCatalogCode: null,
          active: true,
        },
        {
          id: 'unit-med',
          organizationId: 'org-1',
          parentId: null,
          code: 'MED',
          name: 'Médecine',
          unitType: 'CARE_UNIT',
          serviceCatalogCode: 'GENERAL_MEDICINE',
          active: true,
        },
      ])),
      listServiceCatalog: vi.fn().mockReturnValue(of([{
        code: 'GENERAL_MEDICINE',
        nameFr: 'Médecine générale',
        nameEn: 'General medicine',
        active: true,
      }])),
    };
    spatialApi = {
      listSpaces: vi.fn().mockReturnValue(of([{
        id: 'space-101',
        locationNodeId: null,
        code: 'ROOM_101',
        name: 'Chambre 101',
        spaceTypeCode: 'HOSPITAL_ROOM',
        inpatientProfile: true,
        active: true,
      }])),
      listUnitSpaceAssignments: vi.fn().mockReturnValue(of([{
        id: 'assignment-1',
        organizationalUnitId: 'unit-med',
        spaceId: 'space-101',
        validFrom: '2026-07-20T00:00:00Z',
        validTo: null,
      }])),
      listBeds: vi.fn().mockReturnValue(of([{
        id: 'bed-1',
        spaceId: 'space-101',
        bedNumber: '101-A',
        status: 'FREE',
        capacityStatus: 'OPEN',
        readinessStatus: 'READY',
        usageStatus: 'UNASSIGNED',
        available: true,
        version: 0,
      }])),
    };
    staffApi = {
      list: vi.fn().mockReturnValue(of([{
        id: 'doctor-1',
        email: 'doctor@joprelys.com',
        displayName: 'Dr Test',
        role: 'MEDECIN',
        enabled: true,
        createdAt: '2026-07-21T00:00:00Z',
        activeOrganizationalUnits: [{
          id: 'unit-med',
          code: 'MED',
          nameFr: 'Médecine',
          nameEn: 'Medicine',
          primary: true,
        }],
      }])),
    };

    hospitalizationApi['placementOptions'] = vi.fn(() => forkJoin({
      units: (hospitalOrganizationApi['listUnits'] as () => Observable<unknown>)(), serviceCatalog: (hospitalOrganizationApi['listServiceCatalog'] as () => Observable<unknown>)(),
      spaces: (spatialApi['listSpaces'] as () => Observable<unknown>)(), assignments: (spatialApi['listUnitSpaceAssignments'] as () => Observable<unknown>)(), staff: (staffApi.list as () => Observable<unknown>)(),
    }));
    hospitalizationApi['placementBeds'] = vi.fn(() => (spatialApi['listBeds'] as () => Observable<unknown>)());

    await TestBed.configureTestingModule({
      imports: [EmergencyHospitalizationContinuationComponent],
      providers: [
        { provide: HospitalizationLocationApiService, useValue: hospitalizationApi },
        { provide: EmergencyDocumentApiService, useValue: documentApi },
        { provide: RbacApiService, useValue: rbacApi },
        { provide: SpatialApiService, useValue: spatialApi },
        { provide: HospitalOrganizationApiService, useValue: hospitalOrganizationApi },
        { provide: StaffApiService, useValue: staffApi },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => dictionary[key] ?? fallback ?? key,
            currentLanguage: () => 'fr',
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EmergencyHospitalizationContinuationComponent);
    configureInputs();
  });

  it('only proposes organizational units that can host a patient', () => {
    expect(fixture.componentInstance.eligibleUnits().map((unit) => unit.name)).toEqual(['Médecine']);
    expect(fixture.nativeElement.textContent).not.toContain('Caisse');
    expect(fixture.nativeElement.textContent).toContain('URG-TEMP-20260721-000001');
  });

  it('filters inpatient spaces through the dated unit-space assignment', () => {
    fixture.componentInstance.selectUnit('unit-med');

    expect(fixture.componentInstance.eligibleSpaces().map((space) => space.name)).toEqual(['Chambre 101']);
  });

  it('only proposes physicians assigned to the selected unit', () => {
    fixture.componentInstance.selectUnit('unit-med');

    expect(fixture.componentInstance.eligiblePractitioners().map((practitioner) => practitioner.displayName))
      .toEqual(['Dr Test']);
  });

  it('admits from emergency with structured unit, space and bed UUIDs and secures documents', () => {
    const admitted = vi.fn();
    fixture.componentInstance.admitted.subscribe(admitted);
    fixture.componentInstance.selectUnit('unit-med');
    fixture.componentInstance.selectSpace('space-101');
    fixture.componentInstance.selectedBedId.set('bed-1');
    fixture.componentInstance.responsiblePractitionerId = 'doctor-1';
    fixture.componentInstance.admissionReason = 'Surveillance après stabilisation';

    fixture.componentInstance.submit(new Event('submit'));

    expect(hospitalizationApi.admit).toHaveBeenCalledWith({
      patientId: 'patient-1',
      serviceUnitId: 'unit-med',
      spaceId: 'space-101',
      bedId: 'bed-1',
      admissionReason: 'Surveillance après stabilisation',
      emergencyId: 'emergency-1',
      responsiblePractitionerId: 'doctor-1',
    });
    expect(documentApi.generateBundle).toHaveBeenCalledWith('emergency-1');
    expect(admitted).toHaveBeenCalledOnce();
  });

  it('keeps the created admission recoverable when document generation fails', () => {
    const admitted = vi.fn();
    documentApi.generateBundle.mockReturnValueOnce(throwError(() => new Error('document service unavailable')));
    fixture.componentInstance.admitted.subscribe(admitted);
    fixture.componentInstance.selectUnit('unit-med');
    fixture.componentInstance.selectSpace('space-101');
    fixture.componentInstance.selectedBedId.set('bed-1');
    fixture.componentInstance.responsiblePractitionerId = 'doctor-1';
    fixture.componentInstance.admissionReason = 'Surveillance après stabilisation';

    fixture.componentInstance.submit(new Event('submit'));
    fixture.detectChanges();

    expect(hospitalizationApi.admit).toHaveBeenCalledOnce();
    expect(admitted).not.toHaveBeenCalled();
    expect(fixture.componentInstance.admissionCreated()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('doit être régénéré');

    documentApi.generateBundle.mockReturnValueOnce(of([{ id: 'doc-2' }]));
    fixture.componentInstance.retryDocuments();

    expect(documentApi.generateBundle).toHaveBeenCalledTimes(2);
    expect(admitted).toHaveBeenCalledOnce();
  });

  it('does not load admission data or execute admission without the dedicated permission', () => {
    fixture.destroy();
    rbacApi.hasPermission.mockReturnValue(false);
    hospitalizationApi['placementOptions']!.mockClear();
    spatialApi['listSpaces'].mockClear();
    hospitalOrganizationApi['listUnits'].mockClear();
    staffApi.list.mockClear();
    hospitalizationApi.admit.mockClear();
    documentApi.generateBundle.mockClear();

    fixture = TestBed.createComponent(EmergencyHospitalizationContinuationComponent);
    configureInputs();

    fixture.componentInstance.selectedUnitId.set('unit-med');
    fixture.componentInstance.selectedSpaceId.set('space-101');
    fixture.componentInstance.selectedBedId.set('bed-1');
    fixture.componentInstance.responsiblePractitionerId = 'doctor-1';
    fixture.componentInstance.admissionReason = 'Surveillance après stabilisation';
    fixture.componentInstance.submit(new Event('submit'));

    expect(fixture.nativeElement.textContent).toContain(
      'Votre profil ne peut pas créer un séjour hospitalier.',
    );
    expect(hospitalizationApi['placementOptions']).not.toHaveBeenCalled();
    expect(spatialApi['listSpaces']).not.toHaveBeenCalled();
    expect(hospitalOrganizationApi['listUnits']).not.toHaveBeenCalled();
    expect(staffApi.list).not.toHaveBeenCalled();
    expect(hospitalizationApi.admit).not.toHaveBeenCalled();
    expect(documentApi.generateBundle).not.toHaveBeenCalled();
  });
});
