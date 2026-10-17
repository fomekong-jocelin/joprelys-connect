export interface PharmacyVerifyRequest {
  prescriptionNumber: string;
  pinCode: string;
}

export interface PharmacyVerifyItem {
  itemId: string;
  drugName: string;
  dosage: string;
  form?: string;
  quantity: string;
  quantityAlreadyDispensed: number;
  substitutionAllowed: boolean;
  instructions?: string;
}

export interface PharmacyVerifyResponse {
  pharmaceuticalValidated?: boolean;
  prescriptionId: string;
  prescriptionNumber: string;
  status: string;
  dispensationStatus?: 'NONE' | 'PARTIAL' | 'FULL';
  patientName: string;
  doctorName: string;
  issuedAt: string;
  expiresAt: string;
  items: PharmacyVerifyItem[];
}

export interface PharmacyDispensedItemRequest {
  prescriptionItemId: string;
  quantityDispensed: number;
  substitutedWith?: string;
}

export interface PharmacyDispenseRequest {
  prescriptionNumber: string;
  pinCode: string;
  pharmacyName: string;
  pharmacistLicense: string;
  dispensedItems: PharmacyDispensedItemRequest[];
}

export interface PharmacyDispensationHistoryItem {
  prescriptionItemId: string;
  drugName: string;
  quantityDispensed: number;
  substitutedWith?: string;
}

export interface PharmacyDispensationHistoryEntry {
  dispensationId: string;
  dispensedAt: string;
  pharmacyName: string;
  pharmacistLicense: string;
  items: PharmacyDispensationHistoryItem[];
}

export interface DrugStockResponse {
  id: string;
  drugName: string;
  genericName?: string;
  unit: string;
  quantityAvailable: number;
  minimumThreshold: number;
  belowThreshold: boolean;
  batchNumber?: string;
  expiryDate?: string;
  supplier?: string;
  updatedAt: string;
}

export interface CreateDrugStockRequest {
  drugName: string;
  genericName?: string;
  unit: string;
  quantityAvailable: number;
  minimumThreshold: number;
  batchNumber?: string;
  expiryDate?: string;
  supplier?: string;
}


/** Statut affiché : la délivrance est un état distinct du cycle de vie de l'ordonnance. */
export function pharmacyDisplayStatus(prescription: Pick<PharmacyVerifyResponse, 'status' | 'dispensationStatus'>): string {
  if (prescription.status !== 'ACTIVE') return prescription.status;
  if (prescription.dispensationStatus === 'FULL') return 'FULLY_DISPENSED';
  if (prescription.dispensationStatus === 'PARTIAL') return 'PARTIALLY_DISPENSED';
  return 'ACTIVE';
}

/** Une ordonnance active et non entièrement délivrée peut encore être délivrée. */
export function isPrescriptionDispensable(prescription: Pick<PharmacyVerifyResponse, 'status' | 'dispensationStatus'> | null | undefined): boolean {
  return prescription?.status === 'ACTIVE' && prescription.dispensationStatus !== 'FULL';
}
