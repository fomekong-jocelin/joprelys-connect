export type OrganizationalUnitType = 'POLE' | 'DEPARTMENT' | 'SERVICE' | 'CARE_UNIT';

export interface HospitalServiceCatalogEntry {
  code: string;
  nameFr: string;
  nameEn: string;
}

export interface MedicalSpecialtyCatalogEntry {
  code: string;
  nameFr: string;
  nameEn: string;
}

export interface OrganizationalUnit {
  id: string;
  parentId: string | null;
  code: string;
  name: string | null;
  unitType: OrganizationalUnitType;
  serviceCatalogCode: string | null;
  active: boolean;
}

export interface SaveOrganizationalUnitPayload {
  code: string;
  unitType: OrganizationalUnitType;
  parentId: string | null;
  name: string | null;
  serviceCatalogCode: string | null;
}
