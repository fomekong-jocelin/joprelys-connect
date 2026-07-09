export interface CreateEmergencyRequest {
  patientId: string;
  arrivalMode: string;
  triageLevel: string;
  hemodynamicStatus: string;
  chiefComplaint: string;
  initialBpSystolic?: number;
  initialBpDiastolic?: number;
  initialHr?: number;
  initialTemp?: number;
}

export interface AddResuscitationLogRequest {
  actionType: string;
  description: string;
  quantity?: number;
  unit?: string;
  administeredAt?: string;
}

export interface ResuscitationLog {
  id: string;
  emergencyId: string;
  actionType: string;
  description: string;
  quantity?: number;
  unit?: string;
  administeredAt: string;
  administeredByUserId?: string;
}

export interface EmergencyRecord {
  id: string;
  organizationId: string;
  patientId: string;
  patientName: string;
  visitId?: string;
  arrivalMode: string;
  triageLevel: string;
  hemodynamicStatus: string;
  chiefComplaint: string;
  initialBpSystolic?: number;
  initialBpDiastolic?: number;
  initialHr?: number;
  initialTemp?: number;
  stabilizedAt?: string;
  orientation?: string;
  createdByUserId?: string;
  resuscitationLogs: ResuscitationLog[];
  createdAt: string;
  updatedAt: string;
}
