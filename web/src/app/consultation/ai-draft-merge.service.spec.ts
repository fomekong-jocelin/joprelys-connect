import { TestBed } from '@angular/core/testing';
import { AiDraftMergeService } from './ai-draft-merge.service';

describe('AiDraftMergeService clinical safety', () => {
  let service: AiDraftMergeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AiDraftMergeService);
  });

  it('preserves a physician edit made after the AI base snapshot', () => {
    const plan = service.plan(
      {
        baseDraft: { symptoms: 'Douleur abdominale depuis trois jours' },
        draft: { symptoms: 'Douleur abdominale depuis trois jours avec nausées' },
      },
      {
        symptoms: 'Douleur abdominale depuis trois jours, irradiant en fosse iliaque droite',
      },
    );

    expect(plan.textPatch['symptoms']).toBeUndefined();
    expect(plan.conflicts).toContain('symptoms');
  });

  it('applies a proposed text when the physician value still matches the AI base', () => {
    const plan = service.plan(
      {
        baseDraft: { symptoms: 'Douleur abdominale' },
        draft: { symptoms: 'Douleur abdominale depuis trois jours' },
      },
      { symptoms: 'Douleur abdominale' },
    );

    expect(plan.textPatch['symptoms']).toBe('Douleur abdominale depuis trois jours');
    expect(plan.conflicts).not.toContain('symptoms');
  });

  it('never removes existing prescription lines and only adds a new drug', () => {
    const plan = service.plan(
      {
        baseDraft: {},
        draft: {
          prescription: JSON.stringify([
            { drugName: 'Paracétamol', dosage: '1 g' },
            { drugName: 'Spasfon', dosage: '80 mg' },
          ]),
        },
      },
      {
        prescription: [
          { drugName: 'Paracétamol', dosage: '1 g' },
          { drugName: 'Amoxicilline', dosage: '500 mg' },
        ],
      },
    );

    expect(plan.prescriptionAdds).toEqual([
      expect.objectContaining({ drugName: 'Spasfon', dosage: '80 mg', substitutionAllowed: false }),
    ]);
    expect(plan.conflicts).not.toContain('prescription');
  });

  it('forces AI-created prescription lines to non-substitutable until clinician changes it', () => {
    const plan = service.plan(
      {
        baseDraft: {},
        draft: {
          prescription: JSON.stringify([
            { drugName: 'Paracétamol', dosage: '1000 mg', substitutionAllowed: true },
          ]),
        },
      },
      { prescription: [] },
    );

    expect(plan.prescriptionAdds).toEqual([
      expect.objectContaining({
        drugName: 'Paracétamol',
        dosage: '1000 mg',
        substitutionAllowed: false,
      }),
    ]);
  });

  it('preserves an existing drug when AI proposes a conflicting dosage', () => {
    const plan = service.plan(
      {
        baseDraft: {},
        draft: {
          prescription: JSON.stringify([{ drugName: 'Paracétamol', dosage: '500 mg' }]),
        },
      },
      {
        prescription: [{ drugName: 'Paracétamol', dosage: '1 g' }],
      },
    );

    expect(plan.prescriptionAdds).toEqual([]);
    expect(plan.conflicts).toContain('prescription');
  });

  it('merges lab orders without deleting existing exams or creating duplicates', () => {
    const plan = service.plan(
      {
        baseDraft: {},
        draft: { labOrders: JSON.stringify(['NFS', 'CRP']) },
      },
      { exams: ['NFS', 'Créatinine'] },
    );

    expect(plan.labAdds).toEqual(['CRP']);
    expect(plan.conflicts).not.toContain('labOrders');
  });

  it('returns vitals as a separate proposal instead of a direct form mutation', () => {
    const plan = service.plan(
      {
        baseDraft: {},
        draft: { vitals: JSON.stringify({ temperature: 38.4, spo2: 96 }) },
      },
      {},
    );

    expect(plan.vitalsProposal).toEqual({ temperature: 38.4, spo2: 96 });
    expect(plan.conflicts).not.toContain('vitals');
  });
});
