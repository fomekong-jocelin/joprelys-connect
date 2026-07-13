export type EmergencyTriageAssessmentType = 'INITIAL' | 'REASSESSMENT';
export type EmergencyAirwayStatus = 'NOT_ASSESSED' | 'PATENT' | 'AT_RISK' | 'OBSTRUCTED';
export type EmergencyBreathingStatus = 'NOT_ASSESSED' | 'ADEQUATE' | 'DISTRESS' | 'FAILURE';
export type EmergencyCirculationStatus = 'NOT_ASSESSED' | 'STABLE' | 'COMPROMISED' | 'SHOCK';
export type EmergencyDisabilityStatus =
  | 'NOT_ASSESSED'
  | 'ALERT'
  | 'RESPONDS_TO_VOICE'
  | 'RESPONDS_TO_PAIN'
  | 'UNRESPONSIVE';
export type EmergencyExposureStatus =
  | 'NOT_ASSESSED'
  | 'NO_CRITICAL_FINDING'
  | 'TRAUMA'
  | 'HYPOTHERMIA'
  | 'HYPERTHERMIA'
  | 'OTHER';
export type EmergencyRecommendedOrientation =
  | 'RESUSCITATION'
  | 'OPERATING_ROOM'
  | 'HOSPITALIZATION'
  | 'CONSULTATION'
  | 'TRANSFER'
  | 'DISCHARGE'
  | 'DEATH';

export interface EmergencyAbcdeAssessmentRequest {
  readonly airwayStatus: EmergencyAirwayStatus;
  readonly breathingStatus: EmergencyBreathingStatus;
  readonly circulationStatus: EmergencyCirculationStatus;
  readonly disabilityStatus: EmergencyDisabilityStatus;
  readonly exposureStatus: EmergencyExposureStatus;
  readonly respiratoryRate?: number;
  readonly oxygenSaturation?: number;
  readonly gcsScore?: number;
  readonly painScore?: number;
  readonly recommendedOrientation?: EmergencyRecommendedOrientation;
  readonly clinicalNotes?: string;
  readonly assessedAt?: string;
}

export interface CreateEmergencyTriageAssessmentRequest {
  readonly triageLevel: 'RED' | 'ORANGE' | 'YELLOW' | 'GREEN';
  readonly hemodynamicStatus: 'SHOCK' | 'UNSTABLE' | 'STABLE';
  readonly bpSystolic?: number;
  readonly bpDiastolic?: number;
  readonly heartRate?: number;
  readonly temperature?: number;
  readonly abcdeAssessment: EmergencyAbcdeAssessmentRequest;
}

export interface EmergencyTriageAssessment {
  readonly id: string;
  readonly emergencyId: string;
  readonly assessmentType: EmergencyTriageAssessmentType;
  readonly sequenceNumber: number;
  readonly triageLevel: string;
  readonly hemodynamicStatus: string;
  readonly airwayStatus: EmergencyAirwayStatus;
  readonly breathingStatus: EmergencyBreathingStatus;
  readonly circulationStatus: EmergencyCirculationStatus;
  readonly disabilityStatus: EmergencyDisabilityStatus;
  readonly exposureStatus: EmergencyExposureStatus;
  readonly bpSystolic?: number;
  readonly bpDiastolic?: number;
  readonly heartRate?: number;
  readonly respiratoryRate?: number;
  readonly oxygenSaturation?: number;
  readonly temperature?: number;
  readonly gcsScore?: number;
  readonly painScore?: number;
  readonly recommendedOrientation?: EmergencyRecommendedOrientation;
  readonly clinicalNotes?: string;
  readonly assessedAt: string;
  readonly assessedByUserId?: string;
  readonly createdAt: string;
}