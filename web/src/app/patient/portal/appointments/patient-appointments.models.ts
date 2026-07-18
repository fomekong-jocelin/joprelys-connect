export interface DoctorDirectoryEntry {
  doctorId: string;
  displayName: string;
  specialty: string | null;
  department: string | null;
}

export interface AppointmentSlot {
  doctorId: string;
  startAt: string;
  endAt: string;
}

export interface PatientAppointment {
  id: string;
  doctorId: string;
  doctorDisplayName: string;
  patientId: string;
  startAt: string;
  endAt: string;
  status: AppointmentStatus;
  reason: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  visitId: string | null;
  reminderSentAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export type AppointmentStatus =
  | 'CONFIRMED'
  | 'CANCELLED_BY_PATIENT'
  | 'CANCELLED_BY_CLINIC'
  | 'COMPLETED'
  | 'NO_SHOW';

export interface PatientBookAppointmentRequest {
  doctorId: string;
  startAt: string;
  reason?: string;
}

export interface ApiErrorEnvelope {
  error?: {
    code?: string;
    message?: string;
    trace_id?: string;
  };
}
