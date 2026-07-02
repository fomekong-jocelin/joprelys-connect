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
