import { Bed } from '../../patient/patient.models';

export const HOSPITAL_SERVICE_TYPES = [
  'HOSPITALIZATION',
  'EMERGENCY',
  'OUTPATIENT',
  'MEDICO_TECHNICAL',
  'PHARMACY',
  'ADMINISTRATIVE',
] as const;

export type HospitalServiceType = (typeof HOSPITAL_SERVICE_TYPES)[number];

export interface RoomConfiguration {
  readonly id: string;
  readonly wardId: string;
  readonly roomNumber: string;
  readonly capacity: number;
  readonly comfortLevel: string;
  readonly beds: readonly Bed[];
}

export interface WardConfiguration {
  readonly id: string;
  readonly name: string;
  readonly serviceType: HospitalServiceType;
  readonly allowsRooms: boolean;
  readonly rooms: readonly RoomConfiguration[];
}

export interface SpatialConfiguration {
  readonly wards: readonly WardConfiguration[];
}

export interface SaveWardPayload {
  readonly name: string;
  readonly serviceType: HospitalServiceType;
}

export interface SaveRoomPayload {
  readonly wardId: string;
  readonly roomNumber: string;
  readonly capacity: number;
  readonly comfortLevel: string;
}

export interface SaveBedPayload {
  readonly roomId: string;
  readonly bedNumber: string;
}
