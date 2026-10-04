import { HospitalServiceCatalogEntry, OrganizationalUnit } from '../clinic/hospital-organization/hospital-organization.models';
import { FacilitySpace, UnitSpaceAssignment } from '../clinic/spatial/spatial-configuration.models';
export interface HospitalPractitioner {
  readonly id: string; readonly displayName: string; readonly role: string; readonly enabled: boolean;
  readonly email?: string; readonly createdAt?: string;
  readonly activeOrganizationalUnits?: readonly { id: string; code?: string; nameFr?: string; nameEn?: string; primary?: boolean }[];
}
export interface HospitalPlacementOptions {
  readonly units: OrganizationalUnit[]; readonly serviceCatalog: HospitalServiceCatalogEntry[];
  readonly spaces: FacilitySpace[]; readonly assignments: UnitSpaceAssignment[]; readonly staff: HospitalPractitioner[];
}
export interface AdmissionVisit { readonly id: string; readonly visitNumber: string; readonly reason: string; readonly createdAt: string; readonly status: string; }
export interface HospitalConsent { id: string; consentType: string; patientSignaturePresent: boolean; witnessName?: string; documentId?: string; }
export interface SurgicalImplant { id?: string; implantName: string; lotNumber: string; quantity: number; unitPrice: number; manufacturer: string; }
export interface HospitalOperatingReport {
  id: string; procedureName: string; procedureDescription: string; preOperativeDiagnosis: string; postOperativeDiagnosis: string;
  anesthesiaType: string; anesthesiaDescription: string; operationDate: string; validated: boolean; validatedBy: string;
  kSurgeonValue: number; kAnesthesistValue: number; kBlocValue: number; implants: SurgicalImplant[];
}
export interface EligibleMedication { prescriptionItemId: string; medicationName: string; dosage: string; posology?: string; route?: string; prescriptionNumber?: string; }
