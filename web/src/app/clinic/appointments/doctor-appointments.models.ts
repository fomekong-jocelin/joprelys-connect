export type DoctorAppointmentStatus =
  | 'CONFIRMED'
  | 'CANCELLED_BY_PATIENT'
  | 'CANCELLED_BY_CLINIC'
  | 'COMPLETED'
  | 'NO_SHOW';

export interface DoctorAppointment {
  id: string;
  patientId: string;
  patientDisplayName: string;
  patientLocalNumber: string;
  startAt: string;
  endAt: string;
  status: DoctorAppointmentStatus;
  reason: string | null;
  visitId: string | null;
}

export interface DoctorAgendaDay {
  key: string;
  date: Date;
  appointments: readonly DoctorAppointment[];
}
