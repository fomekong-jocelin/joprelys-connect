export interface StructuredHospitalization {
  readonly id: string;
  readonly patientId: string;
  readonly organizationId: string;
  readonly version: number;
  readonly currentServiceUnitId: string;
  readonly currentSpaceId: string;
  readonly currentBedId: string;
  readonly serviceName: string;
  readonly spaceName: string;
  /** Alias d'affichage Angular transitoire. La source de vérité reste currentSpaceId/spaceName. */
  readonly roomNumber: string;
  readonly bedNumber: string;
  readonly admissionReason: string;
  readonly status: 'EN_COURS' | 'SORTI' | 'SORTI_CONTRE_AVIS';
  readonly admittedAt: string;
  readonly dischargedAt?: string;
  readonly dischargeDiagnosis?: string;
  readonly dischargeInstructions?: string;
  readonly dischargeDecidedAt?: string;
  readonly dischargeDecidedBy?: string;
  readonly dischargeAgainstMedicalAdvice?: boolean;
  readonly physicalDepartureAt?: string;
  readonly physicalDepartureBy?: string;
  readonly physicalDepartureNote?: string;
  readonly pdfFilePath?: string;
  readonly hospitalizationNumber: string;
  readonly visitId: string;
  readonly emergencyId?: string;
  readonly responsiblePractitionerId: string;
  readonly documentId?: string;
}

export interface StructuredAdmissionRequest {
  readonly patientId: string;
  readonly serviceUnitId: string;
  readonly spaceId: string;
  readonly bedId: string;
  readonly admissionReason: string;
  readonly visitId?: string;
  readonly emergencyId?: string;
  readonly responsiblePractitionerId: string;
}
