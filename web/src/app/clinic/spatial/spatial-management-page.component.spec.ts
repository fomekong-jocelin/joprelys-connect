import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { DialogService } from '../../shared/ui/dialog.service';
import { bedFixture, roomFixture } from '../../../testing/bed-fixtures';
import { RbacApiService } from '../rbac/rbac-api.service';
import { SpatialManagementPageComponent } from './spatial-management-page.component';
import { SpaceOccupancyView } from './spatial-configuration.models';

describe('SpatialManagementPageComponent', () => {
  let component: SpatialManagementPageComponent;
  let hasPermission: ReturnType<typeof vi.fn>;
  let listInpatientOccupancy: ReturnType<typeof vi.fn>;
  let updateBedCapacityStatus: ReturnType<typeof vi.fn>;
  let updateBedCleaningStatus: ReturnType<typeof vi.fn>;
  let updateBedMaintenanceStatus: ReturnType<typeof vi.fn>;
  let confirm: ReturnType<typeof vi.fn>;

  const rooms: SpaceOccupancyView[] = [
    roomFixture({
      spaceId: 'room-201',
      spaceName: 'Chambre 201',
      installedBeds: 3,
      openBeds: 3,
      occupiedBeds: 1,
      availableBeds: 1,
      beds: [
        bedFixture({ id: 'bed-1', bedNumber: 'Lit 201-A' }),
        bedFixture({ id: 'bed-2', bedNumber: 'Lit 201-B', usageStatus: 'OCCUPIED', available: false }),
        bedFixture({ id: 'bed-3', bedNumber: 'Lit 201-C', readinessStatus: 'CLEANING', available: false }),
      ],
    }),
    roomFixture({
      spaceId: 'room-202',
      spaceName: 'Chambre 202',
      installedBeds: 2,
      openBeds: 1,
      occupiedBeds: 1,
      availableBeds: 0,
      beds: [
        bedFixture({ id: 'bed-4', spaceId: 'room-202', usageStatus: 'OCCUPIED', available: false }),
        bedFixture({ id: 'bed-5', spaceId: 'room-202', capacityStatus: 'CLOSED', available: false }),
      ],
    }),
  ];

  beforeEach(() => {
    hasPermission = vi.fn(() => false);
    listInpatientOccupancy = vi.fn(() => of(rooms));
    updateBedCapacityStatus = vi.fn(() => of({}));
    updateBedCleaningStatus = vi.fn(() => of({}));
    updateBedMaintenanceStatus = vi.fn(() => of({}));
    confirm = vi.fn().mockResolvedValue(true);

    TestBed.configureTestingModule({
      imports: [SpatialManagementPageComponent],
      providers: [
        {
          provide: SpatialApiService,
          useValue: { listInpatientOccupancy, updateBedCleaningStatus, updateBedMaintenanceStatus, updateBedCapacityStatus },
        },
        { provide: I18nService, useValue: { t: (key: string) => key, currentLanguage: () => 'fr' } },
        { provide: RbacApiService, useValue: { hasPermission } },
        { provide: DialogService, useValue: { confirm } },
      ],
    });
    TestBed.overrideComponent(SpatialManagementPageComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpatialManagementPageComponent).componentInstance;
  });

  it('loads every room in a single request', () => {
    component.ngOnInit();

    expect(listInpatientOccupancy).toHaveBeenCalledTimes(1);
    expect(component.rooms()).toHaveLength(2);
  });

  it('sums the backend availability instead of deducing it from installed beds', () => {
    component.ngOnInit();

    expect(component.totals()).toEqual({ free: 1, open: 4, occupied: 2, installed: 5 });
    expect(component.occupancyRate()).toBe(50);
  });

  it('counts each bed once, in its readable state', () => {
    component.ngOnInit();

    expect(component.stateCounts()).toMatchObject({ ALL: 5, FREE: 1, OCCUPIED: 2, CLEANING: 1, MAINTENANCE: 0, CLOSED: 1 });
  });

  it('only shows the rooms that have a bed in the chosen state, and toggles back to all', () => {
    component.ngOnInit();

    component.setFilter('CLOSED');
    expect(component.visibleRooms().map((view) => view.room.spaceName)).toEqual(['Chambre 202']);
    expect(component.visibleRooms()[0].beds.map((bed) => bed.id)).toEqual(['bed-5']);

    component.setFilter('CLOSED');
    expect(component.stateFilter()).toBe('ALL');
    expect(component.visibleRooms()).toHaveLength(2);
  });

  it('checks each operational permission independently', () => {
    hasPermission.mockImplementation((permission: string) => permission === 'BED_CLEANING_MANAGE');

    expect(component.permissions()).toEqual({ capacity: false, cleaning: true, maintenance: false });
  });

  it('maps every bed action to its dedicated endpoint', async () => {
    component.ngOnInit();
    const bed = rooms[0].beds[0];

    component.onBedAction(bed, 'START_CLEANING');
    component.onBedAction(bed, 'END_CLEANING');
    component.onBedAction(bed, 'START_MAINTENANCE');
    component.onBedAction(bed, 'END_MAINTENANCE');
    await component.changeBedCapacityStatus(bed.id, 'OPEN');

    expect(updateBedCleaningStatus.mock.calls).toEqual([['bed-1', 'CLEANING'], ['bed-1', 'READY']]);
    expect(updateBedMaintenanceStatus.mock.calls).toEqual([['bed-1', 'MAINTENANCE'], ['bed-1', 'READY']]);
    expect(updateBedCapacityStatus).toHaveBeenCalledWith('bed-1', 'OPEN');
  });

  it('confirms a closure with the room and the free beds left', async () => {
    component.ngOnInit();

    await component.changeBedCapacityStatus('bed-1', 'CLOSED');

    expect(confirm).toHaveBeenCalledWith(expect.objectContaining({
      title: 'spatial.closeBed.confirmTitle',
      confirmLabel: 'spatial.closeBed.confirmAction',
      tone: 'danger',
    }));
    expect(updateBedCapacityStatus).toHaveBeenCalledWith('bed-1', 'CLOSED');
  });

  it('keeps the bed open when the closure is not confirmed', async () => {
    confirm.mockResolvedValue(false);
    component.ngOnInit();

    await component.changeBedCapacityStatus('bed-1', 'CLOSED');

    expect(updateBedCapacityStatus).not.toHaveBeenCalled();
  });

  it('reopens a bed without confirmation', async () => {
    component.ngOnInit();

    await component.changeBedCapacityStatus('bed-5', 'OPEN');

    expect(confirm).not.toHaveBeenCalled();
    expect(updateBedCapacityStatus).toHaveBeenCalledWith('bed-5', 'OPEN');
  });

  it('ignores a second click while a bed action is pending, then refreshes and announces it', () => {
    const pending = new Subject<unknown>();
    updateBedCleaningStatus.mockReturnValue(pending);
    component.ngOnInit();

    component.changeBedCleaningStatus('bed-1', 'CLEANING');
    component.changeBedCleaningStatus('bed-1', 'CLEANING');

    expect(updateBedCleaningStatus).toHaveBeenCalledTimes(1);
    expect(component.pendingBedId()).toBe('bed-1');
    pending.next({});
    pending.complete();
    expect(component.pendingBedId()).toBeNull();
    expect(listInpatientOccupancy).toHaveBeenCalledTimes(2);
    expect(component.announcement()).toBe('spatial.tracking.updated');
  });

  it('keeps the rooms on screen while refreshing', () => {
    const refresh = new Subject<SpaceOccupancyView[]>();
    component.ngOnInit();
    listInpatientOccupancy.mockReturnValue(refresh);

    component.load();

    expect(component.loading()).toBe(false);
    expect(component.refreshing()).toBe(true);
    expect(component.rooms()).toHaveLength(2);
    refresh.next(rooms);
    refresh.complete();
    expect(component.refreshing()).toBe(false);
  });

  it('shows a translated bed action error instead of failing silently', () => {
    updateBedMaintenanceStatus.mockReturnValue(throwError(() => ({ error: {} })));
    component.ngOnInit();

    component.changeBedMaintenanceStatus('bed-1', 'MAINTENANCE');

    expect(component.bedActionError()).toBe('spatial.bedAction.error');
    expect(component.pendingBedId()).toBeNull();
  });

  it('refreshes the list when the bed changed in the meantime', () => {
    updateBedCleaningStatus.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.ngOnInit();

    component.changeBedCleaningStatus('bed-1', 'CLEANING');

    expect(component.bedActionError()).toBe('spatial.bedAction.conflict');
    expect(listInpatientOccupancy).toHaveBeenCalledTimes(2);
  });

  it('distinguishes a loading failure from an empty room list', () => {
    listInpatientOccupancy.mockReturnValueOnce(throwError(() => new Error('503')));

    component.ngOnInit();

    expect(component.loadError()).toBe('spatial.loadError.occupancy');
    component.load();
    expect(component.loadError()).toBeNull();
    expect(component.rooms()).toHaveLength(2);
  });

  it('agrees the labels with the count', () => {
    expect(component.plural('spatial.tracking.freeLabel', 1)).toBe('spatial.tracking.freeLabel.one');
    expect(component.plural('spatial.tracking.freeLabel', 0)).toBe('spatial.tracking.freeLabel.one');
    expect(component.plural('spatial.tracking.freeLabel', 3)).toBe('spatial.tracking.freeLabel.other');
  });
});
