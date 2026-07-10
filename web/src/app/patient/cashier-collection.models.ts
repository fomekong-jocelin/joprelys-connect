export type CashierCollectionStatus = 'PATIENT_DUE' | 'PATIENT_PARTIALLY_PAID';

export interface CashierCollectionQueueItem {
  invoiceId: string;
  invoiceNumber: string;
  patientId: string;
  patientName: string;
  globalPatientNumber: string;
  phone?: string;
  totalAmount: number;
  patientRemainingAmount: number;
  collectionStatus: CashierCollectionStatus;
  createdAt: string;
  validatedAt?: string;
}
