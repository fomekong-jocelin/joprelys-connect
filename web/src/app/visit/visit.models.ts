export interface Visit {
  id: string;
  visitNumber: string;
  patientId: string;
  patientName: string;
  patientDpu: string;
  reason: string;
  orientation: string;
  status: string;
  createdAt: string;
  closedAt?: string;
}

export interface CreateVisitRequest {
  patientId: string;
  reason: string;
  orientation: string;
}
