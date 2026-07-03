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

export interface CreateLabOrderRequest {
  patientId: string;
  visitId?: string;
  targetOrganizationId?: string;
  examType: string;
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
  validatorName: string;
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
  patientId: string;
  validatorName: string;
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
