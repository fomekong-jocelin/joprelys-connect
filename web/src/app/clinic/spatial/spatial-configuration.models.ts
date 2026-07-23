export type FacilityLocationNodeType = 'SITE' | 'BUILDING' | 'FLOOR' | 'ZONE';

export interface FacilityLocationNode {
  readonly id: string;
  readonly parentId?: string | null;
  readonly code: string;
  readonly name: string;
  readonly nodeType: FacilityLocationNodeType;
  readonly active: boolean;
}

export interface LocationTypeEntry {
  readonly code: FacilityLocationNodeType;
  readonly rank: number;
}

export interface SpaceTypeEntry {
  readonly code: string;
  readonly nameFr: string;
  readonly nameEn: string;
  readonly inpatientCompatible: boolean;
}

export interface FacilitySpace {
  readonly id: string;
  readonly locationNodeId?: string | null;
  readonly code: string;
  readonly name: string;
  readonly spaceTypeCode: string;
  readonly inpatientProfile: boolean;
  readonly active: boolean;
}

export interface InpatientSpaceProfile {
  readonly spaceId: string;
  readonly spaceTypeCode: string;
  readonly comfortLevel: string;
}

export interface BedConfiguration {
  readonly id: string;
  readonly spaceId: string;
  readonly bedNumber: string;
  readonly status: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE';
  readonly capacityStatus: 'OPEN' | 'CLOSED';
  readonly readinessStatus: 'READY' | 'CLEANING' | 'MAINTENANCE';
  readonly usageStatus: 'OCCUPIED' | 'UNASSIGNED';
  readonly available: boolean;
  readonly version: number;
}

export interface UnitSpaceAssignment {
  readonly id: string;
  readonly organizationalUnitId: string;
  readonly spaceId: string;
  readonly validFrom: string;
  readonly validTo?: string | null;
}

export interface SaveLocationPayload {
  readonly parentId?: string | null;
  readonly code: string;
  readonly name: string;
  readonly nodeType: FacilityLocationNodeType;
}

export interface SaveSpacePayload {
  readonly locationNodeId?: string | null;
  readonly code: string;
  readonly name: string;
  readonly spaceTypeCode: string;
  readonly enableInpatientProfile: boolean;
}

export interface SaveBedPayload {
  readonly spaceId: string;
  readonly bedNumber: string;
}

export interface SaveInpatientProfilePayload {
  readonly comfortLevel: string;
}

export interface SaveUnitSpaceAssignmentPayload {
  readonly organizationalUnitId: string;
  readonly spaceId: string;
  readonly validFrom: string;
  readonly validTo?: string | null;
}

export interface SpaceOccupancyView {
  readonly spaceId: string;
  readonly spaceCode: string;
  readonly spaceName: string;
  readonly installedBeds: number;
  readonly occupiedBeds: number;
  readonly availableBeds: number;
  readonly openBeds: number;
  readonly readyBeds: number;
  readonly beds: readonly BedConfiguration[];
}

export interface OrganizationalUnitOccupancyView {
  readonly organizationalUnitId: string;
  readonly unitCode: string;
  readonly unitName: string;
  readonly spaceIds: readonly string[];
  readonly installedBeds: number;
  readonly occupiedBeds: number;
  readonly availableBeds: number;
  readonly openBeds: number;
  readonly readyBeds: number;
}

export interface TransferPayload {
  readonly hospitalizationId: string;
  readonly targetServiceUnitId: string;
  readonly targetSpaceId: string;
  readonly targetBedId: string;
}
