import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { SpatialConfigurationPageComponent } from './spatial-configuration-page.component';

describe('SpatialConfigurationPageComponent', () => {
  let component: SpatialConfigurationPageComponent;
  let api: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = {
      getConfiguration: vi.fn(() => of({
        wards: [{
          id: 'ward-1',
          name: 'Cardiologie',
          serviceType: 'HOSPITALIZATION',
          allowsRooms: true,
          rooms: [],
        }],
      })),
      createWard: vi.fn(() => of({
        id: 'ward-2',
        name: 'Pédiatrie',
        serviceType: 'HOSPITALIZATION',
        allowsRooms: true,
      })),
      updateWard: vi.fn(() => of({})),
      deleteWard: vi.fn(() => of(undefined)),
      createRoom: vi.fn(() => of({})),
      updateRoom: vi.fn(() => of({})),
      deleteRoom: vi.fn(() => of(undefined)),
      createBed: vi.fn(() => of({})),
      updateBed: vi.fn(() => of({})),
      deleteBed: vi.fn(() => of(undefined)),
    };

    TestBed.configureTestingModule({
      imports: [SpatialConfigurationPageComponent],
      providers: [
        { provide: SpatialApiService, useValue: api },
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: RbacApiService, useValue: { hasPermission: vi.fn(() => false) } },
        { provide: OrganizationApiService, useValue: { list: vi.fn(() => of([])) } },
      ],
    });
    TestBed.overrideComponent(SpatialConfigurationPageComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpatialConfigurationPageComponent).componentInstance;
  });

  it('loads the hospital structure on initialization', () => {
    component.ngOnInit();

    expect(component.configuration().wards[0].name).toBe('Cardiologie');
    expect(component.loading()).toBe(false);
  });

  it('creates a typed department and refreshes the structure', () => {
    component.openWardEditor();
    component.editor()!.name = 'Pédiatrie';
    component.editor()!.serviceType = 'HOSPITALIZATION';

    component.submitEditor();

    expect(api['createWard']).toHaveBeenCalledWith({
      name: 'Pédiatrie',
      serviceType: 'HOSPITALIZATION',
    }, undefined);
    expect(api['getConfiguration']).toHaveBeenCalled();
    expect(component.successMessage()).toBe('spatial.config.saveSuccess');
  });
});
