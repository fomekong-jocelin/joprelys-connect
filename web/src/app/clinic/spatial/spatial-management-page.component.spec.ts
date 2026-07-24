import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { SpatialManagementPageComponent } from './spatial-management-page.component';

describe('SpatialManagementPageComponent', () => {
  let component: SpatialManagementPageComponent;
  let hasPermission: ReturnType<typeof vi.fn>;
  let updateBedCapacityStatus: ReturnType<typeof vi.fn>;
  let updateBedCleaningStatus: ReturnType<typeof vi.fn>;
  let updateBedMaintenanceStatus: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    hasPermission = vi.fn(() => false);
    updateBedCapacityStatus = vi.fn(() => of({}));
    updateBedCleaningStatus = vi.fn(() => of({}));
    updateBedMaintenanceStatus = vi.fn(() => of({}));

    TestBed.configureTestingModule({
      imports: [SpatialManagementPageComponent],
      providers: [
        {
          provide: SpatialApiService,
          useValue: {
            listInpatientSpaces: vi.fn(() => of([{
              id: 'space-1',
              locationNodeId: null,
              code: 'MED_201',
              name: 'Chambre 201',
              spaceTypeCode: 'HOSPITAL_ROOM',
              inpatientProfile: true,
              active: true,
            }])),
            listSpaces: vi.fn(() => of([{
              id: 'space-1',
              locationNodeId: null,
              code: 'MED_201',
              name: 'Chambre 201',
              spaceTypeCode: 'HOSPITAL_ROOM',
              inpatientProfile: true,
              active: true,
            }])),
            getSpaceOccupancy: vi.fn(() => of({
              spaceId: 'space-1',
              spaceCode: 'MED_201',
              spaceName: 'Chambre 201',
              installedBeds: 6,
              openBeds: 4,
              readyBeds: 3,
              occupiedBeds: 1,
              availableBeds: 2,
              beds: [],
            })),
            updateBedCleaningStatus,
            updateBedMaintenanceStatus,
            updateBedCapacityStatus,
          },
        },
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: RbacApiService, useValue: { hasPermission } },
      ],
    });
    TestBed.overrideComponent(SpatialManagementPageComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpatialManagementPageComponent).componentInstance;
  });

  it('uses the backend availability count instead of installed minus occupied', () => {
    component.ngOnInit();

    expect(component.occupancy()!.availableBeds).toBe(2);
    expect(component.occupancy()!.installedBeds - component.occupancy()!.occupiedBeds).toBe(5);
  });

  it('calculates the occupancy rate against open beds', () => {
    component.ngOnInit();

    expect(component.occupancyRate()).toBe(25);
  });

  it('checks each operational permission independently', () => {
    hasPermission.mockImplementation((permission: string) => permission === 'BED_CLEANING_MANAGE');

    expect(component.canManageCleaning()).toBe(true);
    expect(component.canManageMaintenance()).toBe(false);
    expect(component.canManageCapacity()).toBe(false);
    expect(component.canOperateBed()).toBe(true);
  });

  it('calls the dedicated capacity endpoint', () => {
    component.ngOnInit();

    component.changeBedCapacityStatus('bed-1', 'CLOSED');

    expect(updateBedCapacityStatus).toHaveBeenCalledWith('bed-1', 'CLOSED');
  });

  it('calls the dedicated cleaning endpoint', () => {
    component.ngOnInit();

    component.changeBedCleaningStatus('bed-1', 'CLEANING');

    expect(updateBedCleaningStatus).toHaveBeenCalledWith('bed-1', 'CLEANING');
  });

  it('calls the dedicated maintenance endpoint', () => {
    component.ngOnInit();

    component.changeBedMaintenanceStatus('bed-1', 'MAINTENANCE');

    expect(updateBedMaintenanceStatus).toHaveBeenCalledWith('bed-1', 'MAINTENANCE');
  });
});
