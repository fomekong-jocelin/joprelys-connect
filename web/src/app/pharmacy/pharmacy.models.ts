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
  prescriptionId: string;
  prescriptionNumber: string;
  status: string;
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

