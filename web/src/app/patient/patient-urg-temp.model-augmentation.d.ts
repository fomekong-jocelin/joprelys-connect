import './patient.models';

declare module './patient.models' {
  interface Patient {
    identityStatus?: 'PROVISIONAL_URGENCY' | 'DECLARED' | 'VERIFIED' | 'MERGED';
    temporaryPatientNumber?: string;
    displayName?: string;
    identityConfidenceLevel?: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'VERIFIED';
    apparentGender?: string;
    estimatedAgeRange?: string;
    physicalDescription?: string;
    foundAt?: string;
    foundLocation?: string;
  }
}

export {};
