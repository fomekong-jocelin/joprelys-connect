import { Bed } from '../../patient/patient.models';

export interface RoomConfiguration {
  id: string;
  wardId: string;
  roomNumber: string;
  capacity: number;
  comfortLevel: string;
  beds: Bed[];
}

export interface WardConfiguration {
  id: string;
  name: string;
  rooms: RoomConfiguration[];
}

export interface SpatialConfiguration {
  wards: WardConfiguration[];
}

export interface SaveWardPayload {
  name: string;
}

export interface SaveRoomPayload {
  wardId: string;
  roomNumber: string;
  capacity: number;
  comfortLevel: string;
}

export interface SaveBedPayload {
  roomId: string;
  bedNumber: string;
}
