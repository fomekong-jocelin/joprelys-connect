import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyApiService } from '../../emergency/emergency-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PatientReconciliationApiService } from './patient-reconciliation-api.service';
import { PatientReconciliationPageComponent } from './patient-reconciliation-page.component';
import {
  PatientReconciliationCandidate,
  PatientReconciliationCorrectionDto,
  PatientReconciliationDecisionDto,
  PatientReconciliationEvent,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';

@Component({ selector: 'app-shell', standalone: true, template: '<ng-content />' })
class AppShellStubComponent {}

describe('PatientReconciliationPageComponent', () => {
  let fixture: ComponentFixture<PatientReconciliationPageComponent>;
  let component: PatientReconciliationPageComponent;
  let router: Router;
  let api: {
    getQueue: ReturnType<typeof vi.fn>;
    getCandidates: ReturnType<typeof vi.fn>;
    getHistory: ReturnType<typeof vi.fn>;
    decide: ReturnType<typeof vi.fn>;
    correct: ReturnType<typeof vi.fn>;
  };
  let emergencyApi: {
    getPatientEmergencies: ReturnType<typeof vi.fn>;
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

  const linkedQueueItem: PatientReconciliationQueueItem = {
    ...queueItem,
    identityStatus: 'MERGED',
    canonicalPatientId: 'canonical-1',
    decisionEventId: 'event-1',
    decision: 'LINK_EXISTING_DPU',
    terminal: true,
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

  const historyEvent: PatientReconciliationEvent = {
    eventId: 'event-1',
    decision: 'LINK_EXISTING_DPU',
    sourcePatientId: queueItem.patientId,
    candidatePatientId: candidate.patientId,
    previousIdentityStatus: 'VERIFIED',
    resultingIdentityStatus: 'MERGED',
    similarityScore: 100,
    matchReasons: ['NAME_STRONG_MATCH'],
    evidenceSourceType: 'DOCUMENT',
    evidenceReference: 'CNI-001',
    justification: 'Identity verified with the original document.',
    correctedEventId: null,
    createdByUserId: 'user-1',
    createdAt: '2026-07-11T11:00:00Z',
  };

  const decisionDto: PatientReconciliationDecisionDto = {
    decision: 'LINK_EXISTING_DPU',
    candidatePatientId: candidate.patientId,
    evidenceSourceType: 'DOCUMENT',
    evidenceReference: 'CNI-001',
    justification: 'Identity verified with the original document.',
  };

  const correctionDto: PatientReconciliationCorrectionDto = {
    correctedEventId: 'event-1',
    replacementCanonicalPatientId: null,
    evidenceSourceType: 'DOCUMENT',
    evidenceReference: 'REVUE-001',
    justification: 'The source document belongs to a namesake.',
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

  const correctionResult = {
    ...decisionResult,
    eventId: 'event-2',
    decision: 'CORRECT_LINK' as const,
    sourceIdentityStatus: 'VERIFIED' as const,
    canonicalPatientId: queueItem.patientId,
    contributingPatientIds: [queueItem.patientId],
  };

  beforeEach(async () => {
    api = {
      getQueue: vi.fn().mockReturnValue(of([queueItem, secondQueueItem, linkedQueueItem])),
      getCandidates: vi.fn().mockReturnValue(of([candidate, secondCandidate])),
      getHistory: vi.fn().mockReturnValue(of([historyEvent])),
      decide: vi.fn().mockReturnValue(of(decisionResult)),
      correct: vi.fn().mockReturnValue(of(correctionResult)),
    };
    emergencyApi = {
      getPatientEmergencies: vi.fn().mockReturnValue(of([{
        id: 'emergency-1',
        patientId: queueItem.patientId,
        createdAt: '2026-07-11T10:00:00Z',
      }])),
    };

    await TestBed.configureTestingModule({
      imports: [PatientReconciliationPageComponent],
      providers: [
        provideRouter([]),
        { provide: PatientReconciliationApiService, useValue: api },
        { provide: EmergencyApiService, useValue: emergencyApi },
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
    })
      .overrideComponent(PatientReconciliationPageComponent, {
        remove: { imports: [AppShellComponent] },
        add: { imports: [AppShellStubComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PatientReconciliationPageComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('loads the URG-TEMP reconciliation queue', () => {
    expect(api.getQueue).toHaveBeenCalledOnce();
    expect(component.queue()).toEqual([queueItem, secondQueueItem, linkedQueueItem]);
  });

  it('renders inside the shared application shell', () => {
    expect(fixture.nativeElement.querySelector('app-shell')).not.toBeNull();
  });

  it('loads candidates and history after selecting a provisional record', () => {
    component.selectPatient(queueItem);

    expect(api.getCandidates).toHaveBeenCalledWith(queueItem.patientId);
    expect(api.getHistory).toHaveBeenCalledWith(queueItem.patientId);
    expect(component.selectedPatient()).toEqual(queueItem);
    expect(component.candidates()).toEqual([candidate, secondCandidate]);
    expect(component.history()).toEqual([historyEvent]);
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

  it('records an explicit decision, preserves the journey and refreshes the queue', () => {
    component.selectPatient(queueItem);
    component.submitDecision(decisionDto);

    expect(api.decide).toHaveBeenCalledWith(
      queueItem.patientId,
      expect.objectContaining({ candidatePatientId: candidate.patientId }),
      expect.any(String),
    );
    expect(api.getQueue).toHaveBeenCalledTimes(2);
    expect(emergencyApi.getPatientEmergencies).toHaveBeenCalledWith(queueItem.patientId);
    expect(component.completedJourney()).toEqual(expect.objectContaining({
      result: decisionResult,
      emergencyId: 'emergency-1',
      loadingEmergency: false,
    }));
    expect(component.selectedPatient()).toBeNull();
  });

  it('continues to hospitalization on the canonical DPU with the source emergency', () => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.selectPatient(queueItem);
    component.submitDecision(decisionDto);

    component.continueToHospitalization();

    expect(navigate).toHaveBeenCalledWith(
      ['/patients', candidate.patientId, 'hospitalizations'],
      { queryParams: { emergencyId: 'emergency-1' } },
    );
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

  it('loads candidates for a terminal link and submits a secured correction', () => {
    component.selectPatient(linkedQueueItem);
    component.submitCorrection(correctionDto);

    expect(api.getHistory).toHaveBeenCalledWith(linkedQueueItem.patientId);
    expect(api.getCandidates).toHaveBeenCalledWith(linkedQueueItem.patientId);
    expect(api.correct).toHaveBeenCalledWith(
      linkedQueueItem.patientId,
      correctionDto,
      expect.any(String),
    );
  });

  it('reuses the correction idempotency key after a network error', () => {
    api.correct
      .mockReturnValueOnce(throwError(() => new Error('network')))
      .mockReturnValueOnce(of(correctionResult));

    component.selectPatient(linkedQueueItem);
    component.submitCorrection(correctionDto);
    component.submitCorrection(correctionDto);

    expect(api.correct.mock.calls[0][2]).toBe(api.correct.mock.calls[1][2]);
  });
});
