import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { SpatialManagementPageComponent } from './spatial-management-page.component';

describe('SpatialManagementPageComponent', () => {
  let component: SpatialManagementPageComponent;
  let hasPermission: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    hasPermission = vi.fn(() => false);

    TestBed.configureTestingModule({
      imports: [SpatialManagementPageComponent],
      providers: [
        {
          provide: SpatialApiService,
          useValue: {
            listWards: vi.fn(() => of([{ id: 'ward-1', name: 'Médecine' }])),
            getWardOccupancy: vi.fn(() => of({
              id: 'ward-1',
              name: 'Médecine',
              rooms: [],
              totalBedsCount: 4,
              occupiedBedsCount: 1,
              availableBedsCount: 1,
            })),
          },
        },
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: RbacApiService, useValue: { hasPermission } },
      ],
    });
    TestBed.overrideComponent(SpatialManagementPageComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpatialManagementPageComponent).componentInstance;
  });

  it('uses the backend availability count instead of total minus occupied', () => {
    component.ngOnInit();

    expect(component.availableBedsCount()).toBe(1);
    expect(component.occupancy()!.totalBedsCount - component.occupancy()!.occupiedBedsCount).toBe(3);
  });

  it('uses the dedicated operational bed status permission', () => {
    hasPermission.mockReturnValue(true);

    expect(component.canModify()).toBe(true);
    expect(hasPermission).toHaveBeenCalledWith('BED_OPERATIONAL_STATUS_MANAGE');
  });
});
