import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { FormBuilder } from '@angular/forms';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EMPTY, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { ConsultationApiService } from './consultation-api.service';
import { ConsultationFeedbackStore } from './consultation-feedback.store';
import { ConsultationPatientContextStore } from './consultation-patient-context.store';
import { ConsultationPrescriptionFacade } from './consultation-prescription.facade';
import { Prescription } from './consultation.models';

describe('ConsultationPrescriptionFacade', () => {
  const draftPrescription = {
    id: 'prescription-1',
    consultationId: 'consultation-1',
    status: 'DRAFT',
    items: [
      {
        drugName: 'Paracétamol',
        dosage: '1 g',
        substitutionAllowed: true,
      },
    ],
    createdAt: '2026-08-01T10:00:00Z',
    updatedAt: '2026-08-01T10:00:00Z',
  } satisfies Prescription;

  function setup(prescription: Prescription = draftPrescription, allergiesVerified = true) {
    const api = {
      getPrescription: vi.fn().mockReturnValue(of(prescription)),
      finalizePrescription: vi.fn().mockReturnValue(of(null)),
    };
    TestBed.configureTestingModule({
      providers: [
        FormBuilder,
        { provide: RbacApiService, useValue: { hasPermission: () => true } },
        ConsultationFeedbackStore,
        { provide: ConsultationPatientContextStore, useValue: { allergiesVerified: signal(allergiesVerified) } },
        ConsultationPrescriptionFacade,
        { provide: ConsultationApiService, useValue: api },
        { provide: I18nService, useValue: { t: (key: string) => key } },
      ],
    });
    return {
      api,
      facade: TestBed.inject(ConsultationPrescriptionFacade),
    };
  }

  afterEach(() => TestBed.resetTestingModule());

  it('charge la prescription et reconstruit ses lignes', () => {
    const { api, facade } = setup();

    facade.load('consultation-1');

    expect(api.getPrescription).toHaveBeenCalledWith('consultation-1');
    expect(facade.current()?.id).toBe('prescription-1');
    expect(facade.items.length).toBe(1);
    expect(facade.items.at(0).get('drugName')?.value).toBe('Paracétamol');
  });

  it('bloque les mutations locales après finalisation', () => {
    const active = { ...draftPrescription, status: 'ACTIVE' } satisfies Prescription;
    const { facade } = setup(active);
    facade.load('consultation-1');

    facade.addLine();
    facade.removeLine(0);

    expect(facade.items.length).toBe(1);
    expect(facade.items.at(0).disabled).toBe(true);
  });

  it('efface une prescription précédente lorsque le backend retourne 204', () => {
    const { api, facade } = setup();
    facade.load('consultation-1');
    api.getPrescription.mockReturnValue(EMPTY);

    facade.load('consultation-2');

    expect(facade.current()).toBeNull();
    expect(facade.items.length).toBe(0);
  });

  it("n'autorise ni ajout de ligne ni signature tant que les allergies ne sont pas vérifiées", () => {
    const { api, facade } = setup(draftPrescription, false);
    facade.load('consultation-1');

    facade.addLine();
    facade.finalize();

    expect(facade.allergiesVerified()).toBe(false);
    expect(facade.items.length).toBe(1);
    expect(api.finalizePrescription).not.toHaveBeenCalled();
  });

  it('autorise ajout et signature une fois les allergies vérifiées', () => {
    const { api, facade } = setup();
    facade.load('consultation-1');

    facade.addLine();
    expect(facade.items.length).toBe(2);

    facade.finalize();
    expect(api.finalizePrescription).toHaveBeenCalledWith('prescription-1');
  });
});
