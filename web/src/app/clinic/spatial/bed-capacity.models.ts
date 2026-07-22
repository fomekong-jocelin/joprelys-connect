import { Bed, RoomOccupancy, WardOccupancy } from '../../patient/patient.models';

export type BedCapacityStatus = 'OPEN' | 'CLOSED';
export type BedReadinessStatus = 'READY' | 'CLEANING' | 'MAINTENANCE';
export type BedUsageStatus = 'UNASSIGNED' | 'OCCUPIED';

export type BedCapacityView = Bed & {
  capacityStatus: BedCapacityStatus;
  readinessStatus: BedReadinessStatus;
  usageStatus: BedUsageStatus;
  available: boolean;
};

export type RoomCapacityView = Omit<RoomOccupancy, 'beds'> & {
  beds: BedCapacityView[];
};

export type WardCapacityView = Omit<WardOccupancy, 'rooms'> & {
  rooms: RoomCapacityView[];
  openBedsCount: number;
  readyBedsCount: number;
};
