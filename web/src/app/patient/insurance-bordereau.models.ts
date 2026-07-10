export type BordereauStatus =
  | 'DRAFT'
  | 'SENT'
  | 'RECEIVED'
  | 'ACCEPTED'
  | 'PARTIALLY_PAID'
  | 'SETTLED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'PAID';

export interface InsuranceBordereau {
  id: string;
  bordereauNumber: string;
  insuranceConventionId: string;
  insuranceConventionName: string;
  startDate: string;
  endDate: string;
  totalAmount: number;
  acceptedAmount?: number | null;
  paidAmount: number;
  remainingAmount: number;
  disputedAmount: number;
  insurerReference?: string | null;
  paymentReference?: string | null;
  rejectionReason?: string | null;
  status: BordereauStatus;
  sentAt?: string | null;
  receivedAt?: string | null;
  acceptedAt?: string | null;
  rejectedAt?: string | null;
  settledAt?: string | null;
  createdAt: string;
}

export interface InsuranceBordereauInvoice {
  id: string;
  invoiceNumber: string;
  totalAmount: number;
  insuranceShare: number;
  patientShare: number;
  status: string;
}

export interface InsuranceBordereauDetails extends InsuranceBordereau {
  invoices: InsuranceBordereauInvoice[];
}

export interface GenerateInsuranceBordereauRequest {
  insuranceConventionId: string;
  startDate: string;
  endDate: string;
}
