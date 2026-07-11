import './patient.models';

declare module './patient.models' {
  interface Patient {
    identityStatus?: 'PROVISIONAL_URGENCY' | 'DECLARED' | 'VERIFIED' | 'MERGED';
    temporaryPatientNumber?: string;
    displayName?: string;
  }
}

export {};
