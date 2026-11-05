import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { bedFixture } from '../../../testing/bed-fixtures';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { SpatialConfigurationPageComponent } from './spatial-configuration-page.component';

describe('SpatialConfigurationPageComponent', () => {
  let component: SpatialConfigurationPageComponent;
  let api: Record<string, ReturnType<typeof vi.fn>>;
  const locale = signal<'fr' | 'en'>('fr');

  const site = { id: 'site-1', parentId: null, code: 'SITE_DLA', name: 'Site Douala', nodeType: 'SITE' as const, active: true };
  const room = {
    id: 'space-1', locationNodeId: 'site-1', code: 'CARDIO_201', name: 'Chambre 201',
    spaceTypeCode: 'HOSPITAL_ROOM', inpatientProfile: true, active: true,
  };

  beforeEach(() => {
    locale.set('fr');
    api = {
      listLocations: vi.fn(() => of([site])),
      listSpaces: vi.fn(() => of([room])),
      listSpaceTypes: vi.fn(() => of([{ code: 'HOSPITAL_ROOM', nameFr: 'Chambre d’hospitalisation', nameEn: 'Hospital room', inpatientCompatible: true }])),
      listUnitSpaceAssignments: vi.fn(() => of([
        { id: 'assignment-1', organizationalUnitId: 'unit-1', spaceId: 'space-1', validFrom: '2026-07-23T00:00:00Z', validTo: null },
        { id: 'assignment-old', organizationalUnitId: 'unit-1', spaceId: 'space-1', validFrom: '2025-01-01T00:00:00Z', validTo: '2025-06-01T00:00:00Z' },
      ])),
      listBeds: vi.fn(() => of([bedFixture({ id: 'bed-1', spaceId: 'space-1', bedNumber: '201-A' })])),
      createLocation: vi.fn(() => of(site)),
      updateLocation: vi.fn(() => of(site)),
      setLocationActive: vi.fn(() => of(site)),
      createSpace: vi.fn(() => of(room)),
      updateSpace: vi.fn(() => of(room)),
      setSpaceActive: vi.fn(() => of(room)),
      getInpatientProfile: vi.fn(() => of({ spaceId: 'space-1', spaceTypeCode: 'HOSPITAL_ROOM', comfortLevel: 'VIP' })),
      saveInpatientProfile: vi.fn(() => of({})),
      createBed: vi.fn(() => of({})),
      updateBed: vi.fn(() => of({})),
      deleteBed: vi.fn(() => of(undefined)),
      createUnitSpaceAssignment: vi.fn(() => of({})),
      updateUnitSpaceAssignment: vi.fn(() => of({})),
    };

    TestBed.configureTestingModule({
      imports: [SpatialConfigurationPageComponent],
      providers: [
        { provide: SpatialApiService, useValue: api },
        {
          provide: HospitalOrganizationApiService,
          useValue: {
            listUnits: vi.fn(() => of([
              { id: 'unit-1', organizationId: 'org-1', parentId: null, code: 'CARDIO', name: 'Cardiologie', unitType: 'SERVICE', serviceCatalogCode: 'CARDIOLOGY', active: true },
              { id: 'pole-1', organizationId: 'org-1', parentId: null, code: 'POLE_MED', name: 'Pôle médecine', unitType: 'POLE', serviceCatalogCode: null, active: true },
            ])),
            listServiceCatalog: vi.fn(() => of([{ code: 'CARDIOLOGY', nameFr: 'Cardiologie', nameEn: 'Cardiology', active: true }])),
          },
        },
        { provide: I18nService, useValue: { t: (key: string) => key, locale } },
        { provide: RbacApiService, useValue: { hasPermission: vi.fn(() => false) } },
        { provide: OrganizationApiService, useValue: { list: vi.fn(() => of([])) } },
      ],
    });
    TestBed.overrideComponent(SpatialConfigurationPageComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpatialConfigurationPageComponent).componentInstance;
    component.ngOnInit();
  });

  it('loads places, rooms and beds, and nests the room under its site', () => {
    expect(component.loading()).toBe(false);
    expect(component.tree().nodes[0].location.name).toBe('Site Douala');
    expect(component.tree().nodes[0].spaces[0].name).toBe('Chambre 201');
    expect(component.beds('space-1')[0].bedNumber).toBe('201-A');
  });

  it('lists the current departments of a room, not the ended links', () => {
    expect(component.roomServices('space-1')).toEqual([
      expect.objectContaining({ label: 'Cardiologie', assignment: expect.objectContaining({ id: 'assignment-1' }) }),
    ]);
  });

  it('only offers departments and care units as room users', () => {
    expect(component.unitOptions().map((unit) => unit.id)).toEqual(['unit-1']);
  });

  it('saves a room, then its comfort level, closes the dialog and reloads', () => {
    component.openEditor({ kind: 'space' });

    component.save({
      kind: 'space',
      id: null,
      payload: { locationNodeId: 'site-1', code: 'PED_301', name: 'Chambre 301', spaceTypeCode: 'HOSPITAL_ROOM', enableInpatientProfile: true },
      comfortLevel: 'VIP',
    });

    expect(api['createSpace']).toHaveBeenCalledWith(expect.objectContaining({ code: 'PED_301' }), undefined);
    expect(api['saveInpatientProfile']).toHaveBeenCalledWith('space-1', { comfortLevel: 'VIP' }, undefined);
    expect(component.editorRequest()).toBeNull();
    expect(component.feedback()).toBe('spatial.config.saved');
    expect(api['listSpaces']).toHaveBeenCalledTimes(2);
  });

  it('keeps the dialog open with a translated error and the server detail in French', () => {
    api['createLocation'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Code déjà utilisé.' } })));
    component.openEditor({ kind: 'location' });

    component.save({ kind: 'location', id: null, payload: { parentId: null, code: 'SITE_DLA', name: 'Site', nodeType: 'SITE' } });

    expect(component.editorRequest()).not.toBeNull();
    expect(component.editorError()).toEqual({ message: 'spatial.config.error.conflict', detail: 'Code déjà utilisé.' });
  });

  it('hides the French server detail when the screen is in English', () => {
    locale.set('en');
    api['createBed'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'Nom obligatoire.' } })));
    component.openEditor({ kind: 'bed', space: room });

    component.save({ kind: 'bed', id: null, payload: { spaceId: 'space-1', bedNumber: '201-B' } });

    expect(component.editorError()).toEqual({ message: 'spatial.config.error.invalid', detail: '' });
  });

  it('deletes the bed being edited', () => {
    const bed = component.beds('space-1')[0];
    component.openEditor({ kind: 'bed', space: room, bed });

    component.remove();

    expect(api['deleteBed']).toHaveBeenCalledWith('bed-1', undefined);
    expect(component.feedback()).toBe('spatial.config.bedRemoved');
  });

  it('deactivates a room instead of deleting it', () => {
    component.openEditor({ kind: 'space', space: room });

    component.remove();

    expect(api['setSpaceActive']).toHaveBeenCalledWith('space-1', false, undefined);
  });

  it('reads the comfort level before editing an inpatient room', () => {
    component.editSpace(room);

    expect(api['getInpatientProfile']).toHaveBeenCalledWith('space-1', undefined);
    expect(component.editorRequest()).toEqual({ kind: 'space', space: room, comfortLevel: 'VIP' });
  });

  it('shows a retryable error when loading fails', () => {
    api['listSpaces'].mockReturnValueOnce(throwError(() => new Error('503')));

    component.loadConfiguration();
    expect(component.loadFailed()).toBe(true);

    component.retry();
    expect(component.loadFailed()).toBe(false);
  });
});
