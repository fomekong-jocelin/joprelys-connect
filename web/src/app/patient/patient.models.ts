export type PatientIdentityStatus = 'PROVISIONAL_URGENCY' | 'DECLARED' | 'VERIFIED' | 'MERGED';

export interface Patient {
  id: string;
  organizationId: string;
  globalPatientNumber: string;
  localPatientNumber: string;
  fullName: string;
  gender: string;
  birthDate: string;
  phone?: string;
  city: string;
  district?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  allergies?: string;
  medicalHistory?: string;
  bloodGroup?: string;
  email?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  emergencyAccessActive?: boolean;
  identityStatus?: PatientIdentityStatus;
  temporaryPatientNumber?: string;
  displayName?: string;
  identityConfidenceLevel?: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERIFIED';
  apparentGender?: string;
  estimatedAgeRange?: string;
  physicalDescription?: string;
  foundAt?: string;
  foundLocation?: string;
}

export interface CreatePatientDto {
  fullName: string;
  gender: string;
  birthDate: string;
  phone?: string;
  city: string;
  district?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  allergies?: string;
  medicalHistory?: string;
  bloodGroup?: string;
  email?: string;
}

export interface LabOrder {
  id: string;
  examRequestNumber: string;
  patientId: string;
  patientName: string;
  visitId?: string;
  requesterPractitionerId: string;
  requesterPractitionerName: string;
  sourceOrganizationId: string;
  targetOrganizationId?: string;
  examType: string;
  exams: string[];
  reason?: string;
  priority: string;
  status: string;
  createdAt: string;
}

export interface LabResult {
  id: string;
  resultNumber: string;
  examRequestNumber: string;
  patientId: string;
  validatorName: string;
  analyteName: string;
  value: string;
  unit?: string;
  referenceRange?: string;
  interpretation: string;
  comment?: string;
  pdfFilePath?: string;
  sampleCollectedAt?: string;
  resultAt?: string;
  validatedAt?: string;
  createdAt: string;
}

export interface PatientAllergy {
  id?: string;
  patientId: string;
  substance: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reaction?: string;
  status: 'ACTIVE' | 'INACTIVE';
  discoveredAt?: string;
  comment?: string;
  updatedAt?: string;
}

export interface CreatePatientAllergyRequest {
  substance: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reaction?: string;
  status?: 'ACTIVE' | 'INACTIVE';
  discoveredAt?: string;
  comment?: string;
}

export interface PatientMedicalHistory {
  id?: string;
  patientId: string;
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER' | 'ALLERGIC' | 'SOCIAL';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
  important?: boolean;
  updatedAt?: string;
}

export interface CreatePatientMedicalHistoryRequest {
  category: 'MEDICAL' | 'SURGICAL' | 'FAMILY' | 'OBSTETRICAL' | 'OTHER' | 'ALLERGIC' | 'SOCIAL';
  description: string;
  onsetDate?: string;
  isOngoing: boolean;
  comment?: string;
  important?: boolean;
}

export interface Hospitalization {
  id: string;
  patientId: string;
  organizationId: string;
  version: number;
  serviceName: string;
  roomNumber: string;
  bedNumber: string;
  admissionReason: string;
  status: 'EN_COURS' | 'SORTI' | 'SORTI_CONTRE_AVIS';
  admittedAt: string;
  dischargedAt?: string;
  dischargeDiagnosis?: string;
  dischargeInstructions?: string;
  dischargeDecidedAt?: string;
  dischargeDecidedBy?: string;
  dischargeAgainstMedicalAdvice?: boolean;
  physicalDepartureAt?: string;
  physicalDepartureBy?: string;
  physicalDepartureNote?: string;
  pdfFilePath?: string;
  hospitalizationNumber: string;
  visitId: string;
  responsiblePractitionerId: string;
  documentId?: string;
}

export interface CreateHospitalizationRequest {
  patientId: string;
  serviceName: string;
  roomNumber: string;
  bedNumber: string;
  admissionReason: string;
  visitId: string;
  responsiblePractitionerId: string;
}

export interface HospitalizationNote {
  id: string;
  hospitalizationId: string;
  organizationId: string;
  authorName: string;
  noteContent: string;
  createdAt: string;
}

export interface CreateHospitalizationNoteRequest {
  noteContent: string;
}

export interface DischargeHospitalizationRequest {
  dischargeDiagnosis: string;
  dischargeInstructions: string;
  againstMedicalAdvice?: boolean;
}

export interface ConfirmPhysicalDepartureRequest {
  confirmed: true;
  note?: string;
}

export interface PatientDuplicateCandidate {
  id: string;
  sourcePatient: Patient;
  targetPatient: Patient;
  similarityScore: number;
  status: 'PENDING' | 'RESOLVED' | 'IGNORED';
  createdAt: string;
}

export interface PatientVaccination {
  id?: string;
  patientId: string;
  vaccineName: string;
  batchNumber?: string;
  administeredAt: string;
  administeredBy?: string;
  notes?: string;
  nextDoseAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePatientVaccinationRequest {
  vaccineName: string;
  batchNumber?: string;
  administeredAt: string;
  administeredBy?: string;
  notes?: string;
  nextDoseAt?: string;
}

export interface MedicalCaptchaResponse {
  captchaId: string;
  question: string;
}

export interface PatientPreRegistrationRequest {
  organizationId: string;
  firstName: string;
  lastName: string;
  gender: string;
  birthDate: string;
  bloodGroup?: string;
  phone?: string;
  email?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  captchaId: string;
  captchaAnswer: string;
}

export interface PatientPreRegistrationResponse {
  id: string;
  organizationId: string;
  firstName: string;
  lastName: string;
  gender: string;
  birthDate: string;
  bloodGroup?: string;
  phone?: string;
  email?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  status: 'AWAITING_VALIDATION' | 'VALIDATED' | 'REJECTED';
  createdAt: string;
  validatedAt?: string;
  validatedBy?: string;
  similarityScore?: number;
  similarPatientId?: string;
  similarPatientName?: string;
}

export interface PreRegistrationValidationRequest {
  firstName: string;
  lastName: string;
  gender: string;
  birthDate: string;
  bloodGroup?: string;
  phone?: string;
  email?: string;
  address?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelation?: string;
  reconcileWithPatientId?: string;
}

export interface PreRegistrationPage {
  content: PatientPreRegistrationResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface Ward {
  id: string;
  name: string;
}

export interface Bed {
  id: string;
  roomId: string;
  bedNumber: string;
  status: 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE';
  version: number;
}

export interface RoomOccupancy {
  id: string;
  roomNumber: string;
  capacity: number;
  comfortLevel: string;
  beds: Bed[];
}

export interface WardOccupancy {
  id: string;
  name: string;
  rooms: RoomOccupancy[];
  totalBedsCount: number;
  occupiedBedsCount: number;
  availableBedsCount: number;
}

export interface BedAssignment {
  id: string;
  hospitalizationId: string;
  bedId: string;
  assignedAt: string;
  releasedAt?: string;
}

export interface InsuranceConvention {
  id: string;
  name: string;
  coveragePercentage: number;
}

export interface TariffGrid {
  id: string;
  keyLetter: string;
  unitValue: number;
}

export interface InvoiceItem {
  id?: string;
  label: string;
  itemType: 'CONSULTATION' | 'K_SURGEON' | 'K_ANESTHESIST' | 'K_BLOC' | 'AMI_CARE' | 'STAY_FEE' | 'MEDICATION';
  unitPrice: number;
  quantity: number;
  coefficient?: number;
  totalItemAmount?: number;
}

export interface Invoice {
  id: string;
  patientId: string;
  visitId?: string;
  invoiceNumber: string;
  insuranceConvention?: InsuranceConvention;
  totalAmount: number;
  patientShare: number;
  insuranceShare: number;
  status: 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'SETTLED' | 'PROFORMA' | 'VALIDATED' | 'CANCELLED';
  items: InvoiceItem[];
  createdAt: string;
  validatedAt?: string;
  validatedByUserId?: string;
  discountAmount?: number;
  discountReason?: string;
}

export interface EstimateItem {
  id?: string;
  label: string;
  itemType: string;
  unitPrice: number;
  quantity: number;
  totalAmount?: number;
}

export interface Estimate {
  id: string;
  patientId: string;
  visitId?: string;
  estimateNumber: string;
  totalAmount: number;
  patientShare: number;
  insuranceShare: number;
  status: 'DRAFT' | 'ACCEPTED' | 'REJECTED' | 'INVOICED';
  items: EstimateItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreditNote {
  id: string;
  invoiceId: string;
  creditNoteNumber: string;
  amount: number;
  reason: string;
  status: 'ACTIVE' | 'CANCELLED';
  createdAt: string;
}

export interface Receivable {
  id: string;
  invoiceId: string;
  debtorType: 'PATIENT' | 'INSURANCE';
  debtorId: string;
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  status: 'UNPAID' | 'PARTIALLY_PAID' | 'PAID';
  dueDate?: string;
  createdAt: string;
  agingSlice?: string;
}

export interface ReceivableReminder {
  id: string;
  receivableId: string;
  actionType: 'PHONE_CALL' | 'EMAIL' | 'LETTER' | 'VISIT';
  status: 'PENDING' | 'PROMISED_PAYMENT' | 'DISPUTE' | 'UNREACHABLE';
  notes?: string;
  actorId: string;
  createdAt: string;
}

export interface Payment {
  id: string;
  invoiceId: string;
  amount: number;
  paymentMethod: 'CASH' | 'CHECK' | 'BANK_TRANSFER';
  referenceNumber?: string;
  receivedByUserId: string;
  createdAt: string;
}

export interface CashRegister {
  id: string;
  code: string;
  name: string;
}

export interface CashSession {
  id: string;
  cashRegisterId: string;
  cashRegisterName: string;
  openedByUserId: string;
  openedAt: string;
  openingBalance: number;
  closedByUserId?: string;
  closedAt?: string;
  closingBalance?: number;
  declaredBalance?: number;
  discrepancyAmount?: number;
  discrepancyReason?: string;
  status: 'OPEN' | 'CLOSED';
  discrepancyResolved: boolean;
  resolutionNotes?: string;
  resolvedByUserId?: string;
  resolvedAt?: string;
}

export interface CashMovement {
  id: string;
  cashRegisterSessionId: string;
  movementType: 'IN' | 'OUT' | 'TRANSFER_TO_BANK';
  amount: number;
  description: string;
  paymentMethod: 'CASH' | 'CHECK' | 'BANK_TRANSFER';
  referenceNumber?: string;
  createdByUserId: string;
  createdAt: string;
}

export interface PaymentReceipt {
  id: string;
  paymentId: string;
  receiptNumber: string;
  amount: number;
  paymentMethod: string;
  createdAt: string;
}

export type BordereauStatus = 'DRAFT' | 'SENT' | 'PAID' | 'CANCELLED';

export interface Bordereau {
  id: string;
  bordereauNumber: string;
  insuranceConventionId: string;
  insuranceConventionName: string;
  startDate: string;
  endDate: string;
  totalAmount: number;
  status: BordereauStatus;
  createdAt: string;
}

export interface BordereauInvoiceDto {
  id: string;
  invoiceNumber: string;
  totalAmount: number;
  insuranceShare: number;
  patientShare: number;
  status: string;
}

export interface BordereauDetails extends Bordereau {
  invoices: BordereauInvoiceDto[];
}

export interface SettlementParty {
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  status: 'NOT_DUE' | 'UNPAID' | 'PARTIALLY_PAID' | 'PAID';
}

export interface InvoiceSettlementSummary {
  invoiceId: string;
  collectionStatus: 'NOT_YET_DUE' | 'PATIENT_DUE' | 'PATIENT_PARTIALLY_PAID' | 'INSURANCE_DUE' | 'SETTLED' | 'CANCELLED';
  patient: SettlementParty;
  insurance?: SettlementParty;
}

export interface CashSessionSummary {
  sessionId: string;
  openingCash: number;
  cashReceipts: number;
  chequeReceipts: number;
  transferReceipts: number;
  cashExpenses: number;
  bankDeposits: number;
  expectedCash: number;
}
