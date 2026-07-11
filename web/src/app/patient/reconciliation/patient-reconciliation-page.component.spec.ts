import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientReconciliationApiService } from './patient-reconciliation-api.service';
import { PatientReconciliationPageComponent } from './patient-reconciliation-page.component';
import {
  PatientReconciliationCandidate,
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

  beforeEach(async () => {
    api = {
      getQueue: vi.fn().mockReturnValue(of([queueItem])),
      getCandidates: vi.fn().mockReturnValue(of([candidate])),
      decide: vi.fn().mockReturnValue(of({
        eventId: 'event-1',
        decision: 'LINK_EXISTING_DPU',
        sourcePatientId: queueItem.patientId,
        sourceIdentityStatus: 'MERGED',
        canonicalPatientId: candidate.patientId,
        temporaryPatientNumber: queueItem.temporaryPatientNumber,
        contributingPatientIds: [queueItem.patientId, candidate.patientId],
        decidedAt: '2026-07-11T11:00:00Z',
        replayed: false,
      })),
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
    expect(component.queue()).toEqual([queueItem]);
  });

  it('loads candidates only after selecting a provisional record', () => {
    component.selectPatient(queueItem);

    expect(api.getCandidates).toHaveBeenCalledWith(queueItem.patientId);
    expect(component.selectedPatient()).toEqual(queueItem);
    expect(component.candidates()).toEqual([candidate]);
  });

  it('records an explicit decision and refreshes the queue', () => {
    component.selectPatient(queueItem);
    component.submitDecision({
      decision: 'LINK_EXISTING_DPU',
      candidatePatientId: candidate.patientId,
      evidenceSourceType: 'DOCUMENT',
      evidenceReference: 'CNI-001',
      justification: 'Identity verified with the original document.',
    });

    expect(api.decide).toHaveBeenCalledWith(
      queueItem.patientId,
      expect.objectContaining({ candidatePatientId: candidate.patientId }),
      expect.any(String),
    );
    expect(api.getQueue).toHaveBeenCalledTimes(2);
    expect(component.selectedPatient()).toBeNull();
  });
});
