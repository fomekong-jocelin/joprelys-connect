export enum ExamType {
  LABORATOIRE = 'LABORATOIRE',
  IMAGERIE = 'IMAGERIE',
  CARDIOLOGIE = 'CARDIOLOGIE',
  ORL = 'ORL',
  OPHTALMOLOGIE = 'OPHTALMOLOGIE',
  AUTRE = 'AUTRE'
}

export enum LabOrderStatus {
  REQUESTED = 'REQUESTED',
  AWAITING_PAYMENT = 'AWAITING_PAYMENT',
  PAID = 'PAID',
  SAMPLE_COLLECTED = 'SAMPLE_COLLECTED',
  IN_PROGRESS = 'IN_PROGRESS',
  RESULT_AVAILABLE = 'RESULT_AVAILABLE',
  VALIDATED = 'VALIDATED',
  CANCELLED = 'CANCELLED'
}

export interface LabOrderItem {
  id: string;
  examName: string;
  status: LabOrderStatus;
  sampleCollectedAt?: string;
  resultAt?: string;
  validatedAt?: string;
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
  examType: ExamType;
  exams: string[];
  items?: LabOrderItem[];
  reason?: string;
  priority: string;
  status: LabOrderStatus;
  createdAt: string;
}

export interface CreateLabOrderRequest {
  patientId: string;
  visitId?: string;
  targetOrganizationId?: string;
  examType: ExamType;
  exams: string[];
  reason?: string;
  priority?: string;
}

export interface LabResultItemRequest {
  analyteName: string;
  value: string;
  unit?: string;
  referenceRange?: string;
  interpretation?: string;
  comment?: string;
}

export interface LabResultUploadRequest {
  examRequestNumber: string;
  labOrderItemId?: string;
  validatorName: string;
  validatorUserId?: string;
  status?: string;
  sampleCollectedAt?: string;
  resultAt?: string;
  validatedAt?: string;
  conclusion?: string;
  results: LabResultItemRequest[];
  pdfBase64?: string;
}

export interface LabResult {
  id: string;
  resultNumber: string;
  examRequestNumber: string;
  labOrderItemId?: string;
  examName?: string;
  patientId: string;
  validatorName: string;
  status?: string;
  validatorUserId?: string;
  conclusion?: string;
  documentId?: string;
  version: number;
  parentResultId?: string;
  analyteName: string;
  value: string;
  unit?: string;
  referenceRange?: string;
  interpretation?: string;
  comment?: string;
  pdfFilePath?: string;
  sampleCollectedAt?: string;
  resultAt?: string;
  validatedAt?: string;
  createdAt: string;
}
