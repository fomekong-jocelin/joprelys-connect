import { Bed, RoomOccupancy, WardOccupancy } from '../../patient/patient.models';

export type BedCapacityStatus = 'OPEN' | 'CLOSED';
export type BedReadinessStatus = 'READY' | 'CLEANING' | 'MAINTENANCE';
export type BedUsageStatus = 'UNASSIGNED' | 'OCCUPIED';

export type BedStateReasonCode =
  | 'CAPACITY_REOPENING'
  | 'CAPACITY_TEMPORARY_CLOSURE'
  | 'CAPACITY_STAFFING_SHORTAGE'
  | 'CAPACITY_SAFETY'
  | 'CAPACITY_OTHER'
  | 'CLEANING_AFTER_DEPARTURE'
  | 'CLEANING_AFTER_TRANSFER'
  | 'CLEANING_ROUTINE'
  | 'CLEANING_ISOLATION'
  | 'CLEANING_INCIDENT'
  | 'CLEANING_COMPLETED'
  | 'MAINTENANCE_PREVENTIVE'
  | 'MAINTENANCE_CORRECTIVE'
  | 'MAINTENANCE_SAFETY'
  | 'MAINTENANCE_COMPLETED'
  | 'LEGACY_SUPERVISION';

export interface BedStateChangePayload {
  reasonCode: BedStateReasonCode;
  note?: string;
}

export interface BedStateChangeHistoryItem {
  id: string;
  bedId: string;
  axis: 'CAPACITY' | 'READINESS';
  previousValue: string;
  newValue: string;
  reasonCode: BedStateReasonCode;
  reasonNote?: string;
  actorId?: string;
  actorDisplayName: string;
  source: 'MANUAL' | 'SYSTEM_TRANSFER' | 'SYSTEM_PHYSICAL_DEPARTURE' | 'LEGACY_SUPERVISION';
  occurredAt: string;
}

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
