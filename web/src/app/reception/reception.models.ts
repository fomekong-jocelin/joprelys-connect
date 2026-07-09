export interface CreateReceptionLogRequest {
  logType: string;
  firstName: string;
  lastName: string;
  idDocumentType?: string;
  idDocumentNumber?: string;
  targetPatientId?: string;
  targetStaffId?: string;
  reason?: string;
  arrivalAt?: string;
}

export interface ReceptionLog {
  id: string;
  organizationId: string;
  logType: string;
  firstName: string;
  lastName: string;
  idDocumentType?: string;
  idDocumentNumber?: string;
  targetPatientId?: string;
  targetStaffId?: string;
  reason?: string;
  arrivalAt: string;
  departureAt?: string;
  createdByUserId?: string;
  createdAt: string;
  updatedAt: string;
}
