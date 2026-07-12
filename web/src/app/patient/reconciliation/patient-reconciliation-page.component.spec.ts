import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientReconciliationApiService } from './patient-reconciliation-api.service';
import { PatientReconciliationPageComponent } from './patient-reconciliation-page.component';
import {
  PatientReconciliationCandidate,
  PatientReconciliationDecisionDto,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';

describe('PatientReconciliationPageComponent', () => {
  let fixture: ComponentFixture<PatientReconciliationPageComponent>;
  let component: PatientReconciliationPageComponent;
  let api: {
    getQueue: ReturnType<typeof vi.fn>;
    getCandidates: ReturnType<typeof vi.fn>;
    decide: ReturnType<typeof vi.fn>;
  };

  const queueItem: PatientReconciliationQueueItem = {
    patientId: 'source-1',
    temporaryPatientNumber: 'URG-TEMP-20260711-000001',
    displayName: 'Nadège Maffock',
    identityStatus: 'VERIFIED',
    confidenceLevel: 'VERIFIED',
    apparentGender: 'FEMININ',
    estimatedAgeRange: '25-35',
    foundAt: '2026-07-11T10:00:00Z',
    foundLocation: 'Akwa',
    createdAt: '2026-07-11T10:00:00Z',
    canonicalPatientId: null,
    decisionEventId: null,
    decision: null,
    terminal: false,
  };

  const secondQueueItem: PatientReconciliationQueueItem = {
    ...queueItem,
    patientId: 'source-2',
    temporaryPatientNumber: 'URG-TEMP-20260711-000002',
    displayName: 'Patient B',
  };

  const candidate: PatientReconciliationCandidate = {
    patientId: 'canonical-1',
    globalPatientNumber: 'DPU-001',
    localPatientNumber: 'PAT-001',
    displayName: 'Nadège Maffock',
    gender: 'FEMININ',
    birthDate: '1994-05-10',
    phone: '+237699000111',
    city: 'Douala',
    score: 100,
    reasons: ['NAME_STRONG_MATCH', 'BIRTH_DATE_EXACT_MATCH'],
  };

  const secondCandidate: PatientReconciliationCandidate = {
    ...candidate,
    patientId: 'canonical-2',
    globalPatientNumber: 'DPU-002',
    localPatientNumber: 'PAT-002',
    displayName: 'Patient B',
  };

  const decisionDto: PatientReconciliationDecisionDto = {
    decision: 'LINK_EXISTING_DPU',
    candidatePatientId: candidate.patientId,
    evidenceSourceType: 'DOCUMENT',
    evidenceReference: 'CNI-001',
    justification: 'Identity verified with the original document.',
  };

  const decisionResult = {
    eventId: 'event-1',
    decision: 'LINK_EXISTING_DPU' as const,
    sourcePatientId: queueItem.patientId,
    sourceIdentityStatus: 'MERGED' as const,
    canonicalPatientId: candidate.patientId,
    temporaryPatientNumber: queueItem.temporaryPatientNumber,
    contributingPatientIds: [queueItem.patientId, candidate.patientId],
    decidedAt: '2026-07-11T11:00:00Z',
    replayed: false,
  };

  beforeEach(async () => {
    api = {
      getQueue: vi.fn().mockReturnValue(of([queueItem, secondQueueItem])),
      getCandidates: vi.fn().mockReturnValue(of([candidate])),
      decide: vi.fn().mockReturnValue(of(decisionResult)),
    };

    await TestBed.configureTestingModule({
      imports: [PatientReconciliationPageComponent],
      providers: [
        { provide: PatientReconciliationApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            locale: vi.fn().mockReturnValue('fr'),
            t: vi.fn((key: string, fallback?: string) => fallback ?? key),
          },
        },
        {
          provide: ApiErrorI18nService,
          useValue: { message: vi.fn().mockReturnValue('Erreur') },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientReconciliationPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the URG-TEMP reconciliation queue', () => {
    expect(api.getQueue).toHaveBeenCalledOnce();
    expect(component.queue()).toEqual([queueItem, secondQueueItem]);
  });

  it('loads candidates only after selecting a provisional record', () => {
    component.selectPatient(queueItem);

    expect(api.getCandidates).toHaveBeenCalledWith(queueItem.patientId);
    expect(component.selectedPatient()).toEqual(queueItem);
    expect(component.candidates()).toEqual([candidate]);
  });

  it('ignores an obsolete candidate response after another patient is selected', () => {
    const firstResponse = new Subject<PatientReconciliationCandidate[]>();
    const secondResponse = new Subject<PatientReconciliationCandidate[]>();
    api.getCandidates.mockImplementation((patientId: string) =>
      patientId === queueItem.patientId ? firstResponse : secondResponse);

    component.selectPatient(queueItem);
    component.selectPatient(secondQueueItem);

    secondResponse.next([secondCandidate]);
    firstResponse.next([candidate]);

    expect(component.selectedPatient()).toEqual(secondQueueItem);
    expect(component.candidates()).toEqual([secondCandidate]);
  });

  it('records an explicit decision and refreshes the queue', () => {
    component.selectPatient(queueItem);
    component.submitDecision(decisionDto);

    expect(api.decide).toHaveBeenCalledWith(
      queueItem.patientId,
      expect.objectContaining({ candidatePatientId: candidate.patientId }),
      expect.any(String),
    );
    expect(api.getQueue).toHaveBeenCalledTimes(2);
    expect(component.selectedPatient()).toBeNull();
  });

  it('reuses the idempotency key when the same decision is retried after a network error', () => {
    api.decide
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(decisionResult));

    component.selectPatient(queueItem);
    component.submitDecision(decisionDto);
    component.submitDecision(decisionDto);

    const firstKey = api.decide.mock.calls[0][2];
    const secondKey = api.decide.mock.calls[1][2];
    expect(firstKey).toBe(secondKey);
  });

  it('creates a new idempotency key after the decision payload changes', () => {
    api.decide.mockReturnValue(throwError(() => new Error('network')));

    component.selectPatient(queueItem);
    component.submitDecision(decisionDto);
    component.submitDecision({
      ...decisionDto,
      justification: 'Updated justification after reviewing the original document.',
    });

    const firstKey = api.decide.mock.calls[0][2];
    const secondKey = api.decide.mock.calls[1][2];
    expect(firstKey).not.toBe(secondKey);
  });
});
