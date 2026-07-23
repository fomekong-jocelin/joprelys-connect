import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { SpatialConfigurationPageComponent } from './spatial-configuration-page.component';

describe('SpatialConfigurationPageComponent', () => {
  let component: SpatialConfigurationPageComponent;
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let organizationApi: Record<string, ReturnType<typeof vi.fn>>;

  const location = {
    id: 'site-1',
    parentId: null,
    code: 'SITE_DLA',
    name: 'Site Douala',
    nodeType: 'SITE' as const,
    active: true,
  };

  const space = {
    id: 'space-1',
    locationNodeId: 'site-1',
    code: 'CARDIO_201',
    name: 'Chambre 201',
    spaceTypeCode: 'HOSPITAL_ROOM',
    inpatientProfile: true,
    active: true,
  };

  beforeEach(() => {
    api = {
      listLocations: vi.fn(() => of([location])),
      listSpaces: vi.fn(() => of([space])),
      listSpaceTypes: vi.fn(() => of([{
        code: 'HOSPITAL_ROOM',
        nameFr: 'Chambre d’hospitalisation',
        nameEn: 'Hospital room',
        inpatientCompatible: true,
      }])),
      listUnitSpaceAssignments: vi.fn(() => of([{
        id: 'assignment-1',
        organizationalUnitId: 'unit-1',
        spaceId: 'space-1',
        validFrom: '2026-07-23T00:00:00Z',
        validTo: null,
      }])),
      listBeds: vi.fn(() => of([{
        id: 'bed-1',
        spaceId: 'space-1',
        bedNumber: '201-A',
        status: 'FREE',
        capacityStatus: 'OPEN',
        readinessStatus: 'READY',
        usageStatus: 'UNASSIGNED',
        available: true,
        version: 0,
      }])),
      createLocation: vi.fn(() => of(location)),
      updateLocation: vi.fn(() => of(location)),
      setLocationActive: vi.fn(() => of(location)),
      createSpace: vi.fn(() => of(space)),
      updateSpace: vi.fn(() => of(space)),
      setSpaceActive: vi.fn(() => of(space)),
      getInpatientProfile: vi.fn(() => of({
        spaceId: 'space-1',
        spaceTypeCode: 'HOSPITAL_ROOM',
        comfortLevel: 'STANDARD',
      })),
      saveInpatientProfile: vi.fn(() => of({
        spaceId: 'space-1',
        spaceTypeCode: 'HOSPITAL_ROOM',
        comfortLevel: 'STANDARD',
      })),
      createBed: vi.fn(() => of({})),
      updateBed: vi.fn(() => of({})),
      deleteBed: vi.fn(() => of(undefined)),
      createUnitSpaceAssignment: vi.fn(() => of({})),
      updateUnitSpaceAssignment: vi.fn(() => of({})),
    };

    organizationApi = { list: vi.fn(() => of([])) };

    TestBed.configureTestingModule({
      imports: [SpatialConfigurationPageComponent],
      providers: [
        { provide: SpatialApiService, useValue: api },
        {
          provide: HospitalOrganizationApiService,
          useValue: {
            listUnits: vi.fn(() => of([{
              id: 'unit-1',
              organizationId: 'org-1',
              parentId: null,
              code: 'CARDIO',
              name: 'Cardiologie',
              unitType: 'CARE_UNIT',
              serviceCatalogCode: 'CARDIOLOGY',
              active: true,
            }])),
            listServiceCatalog: vi.fn(() => of([{
              code: 'CARDIOLOGY',
              nameFr: 'Cardiologie',
              nameEn: 'Cardiology',
              active: true,
            }])),
          },
        },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
            currentLanguage: () => 'fr',
          },
        },
        { provide: RbacApiService, useValue: { hasPermission: vi.fn(() => false) } },
        { provide: OrganizationApiService, useValue: organizationApi },
      ],
    });
    TestBed.overrideComponent(SpatialConfigurationPageComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpatialConfigurationPageComponent).componentInstance;
  });

  it('loads locations, spaces, organizational units and beds on initialization', () => {
    component.ngOnInit();

    expect(component.locations()[0].name).toBe('Site Douala');
    expect(component.spaces()[0].name).toBe('Chambre 201');
    expect(component.units()[0].name).toBe('Cardiologie');
    expect(component.beds('space-1')[0].bedNumber).toBe('201-A');
    expect(component.loading()).toBe(false);
  });

  it('does not submit an incomplete location', () => {
    component.openLocationEditor();
    component.editor()!.code = 'SITE_YDE';
    component.editor()!.name = '';

    component.submitEditor();

    expect(api['createLocation']).not.toHaveBeenCalled();
    expect(component.editor()).not.toBeNull();
  });

  it('creates a typed inpatient space and persists its profile', () => {
    component.ngOnInit();
    component.openSpaceEditor();
    component.editor()!.code = 'PED_301';
    component.editor()!.name = 'Chambre 301';
    component.editor()!.locationNodeId = 'site-1';
    component.editor()!.spaceTypeCode = 'HOSPITAL_ROOM';
    component.editor()!.enableInpatientProfile = true;
    component.editor()!.comfortLevel = 'VIP';

    component.submitEditor();

    expect(api['createSpace']).toHaveBeenCalledWith({
      locationNodeId: 'site-1',
      code: 'PED_301',
      name: 'Chambre 301',
      spaceTypeCode: 'HOSPITAL_ROOM',
      enableInpatientProfile: true,
    }, undefined);
    expect(api['saveInpatientProfile']).toHaveBeenCalledWith(
      'space-1',
      { comfortLevel: 'VIP' },
      undefined,
    );
    expect(component.successMessage()).toBe('Configuration enregistrée.');
  });

  it('opens bed creation against a spaceId rather than a roomId', () => {
    component.openBedEditor(space);

    expect(component.editor()?.kind).toBe('bed');
    expect(component.editor()?.spaceId).toBe('space-1');
    expect(component.editor()?.bedNumber).toBe('');
  });
});
