import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PatientApiService } from '../patient/patient-api.service';
import { PatientAllergy } from '../patient/patient.models';
import { ConsultationPatientContextStore } from './consultation-patient-context.store';

describe('ConsultationPatientContextStore', () => {
  const allergy = { id: 'a-1', substance: 'Pénicilline', severity: 'CRITICAL', status: 'ACTIVE' } as PatientAllergy;
  let api: {
    getById: ReturnType<typeof vi.fn>;
    getAllergies: ReturnType<typeof vi.fn>;
    getMedicalHistory: ReturnType<typeof vi.fn>;
  };

  function setup(): ConsultationPatientContextStore {
    TestBed.configureTestingModule({
      providers: [ConsultationPatientContextStore, { provide: PatientApiService, useValue: api }],
    });
    return TestBed.inject(ConsultationPatientContextStore);
  }

  beforeEach(() => {
    api = {
      getById: vi.fn().mockReturnValue(of({ id: 'patient-1', fullName: 'Patient Test' })),
      getAllergies: vi.fn().mockReturnValue(of([allergy])),
      getMedicalHistory: vi.fn().mockReturnValue(of([])),
    };
  });

  it('marque les allergies comme vérifiées après un chargement réussi, même vide', () => {
    api.getAllergies.mockReturnValue(of([]));
    const store = setup();

    store.load('patient-1');

    expect(store.allergiesStatus()).toBe('loaded');
    expect(store.allergiesVerified()).toBe(true);
    expect(store.failedSections()).toEqual([]);
  });

  it("ne confond jamais un échec de chargement avec « aucune allergie »", () => {
    api.getAllergies.mockReturnValue(throwError(() => new Error('503')));
    api.getMedicalHistory.mockReturnValue(throwError(() => new Error('503')));
    const store = setup();

    store.load('patient-1');

    expect(store.allergiesStatus()).toBe('error');
    expect(store.allergiesVerified()).toBe(false);
    expect(store.failedSections().map((section) => section.messageKey)).toEqual([
      'clinicalSafety.allergies.unverified',
      'clinicalSafety.history.unverified',
    ]);
  });

  it('relance uniquement les sections en échec', () => {
    api.getAllergies.mockReturnValueOnce(throwError(() => new Error('timeout')));
    const store = setup();
    store.load('patient-1');

    store.retryFailed();

    expect(api.getAllergies).toHaveBeenCalledTimes(2);
    expect(api.getById).toHaveBeenCalledTimes(1);
    expect(api.getMedicalHistory).toHaveBeenCalledTimes(1);
    expect(store.allergies()).toEqual([allergy]);
    expect(store.allergiesVerified()).toBe(true);
  });
});
