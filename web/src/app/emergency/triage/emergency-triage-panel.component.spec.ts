import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyApiService } from '../emergency-api.service';
import { EmergencyTriagePanelComponent } from './emergency-triage-panel.component';
import { EmergencyTriageAssessment } from './emergency-triage.models';

describe('EmergencyTriagePanelComponent', () => {
  let fixture: ComponentFixture<EmergencyTriagePanelComponent>;
  let component: EmergencyTriagePanelComponent;
  let api: {
    getTriageAssessments: ReturnType<typeof vi.fn>;
    addTriageAssessment: ReturnType<typeof vi.fn>;
  };

  const initialAssessment: EmergencyTriageAssessment = {
    id: 'assessment-1',
    emergencyId: 'emergency-1',
    assessmentType: 'INITIAL',
    sequenceNumber: 1,
    triageLevel: 'RED',
    hemodynamicStatus: 'SHOCK',
    airwayStatus: 'NOT_ASSESSED',
    breathingStatus: 'NOT_ASSESSED',
    circulationStatus: 'NOT_ASSESSED',
    disabilityStatus: 'NOT_ASSESSED',
    exposureStatus: 'NOT_ASSESSED',
    assessedAt: '2026-07-13T15:00:00Z',
    createdAt: '2026-07-13T15:00:00Z',
  };

  const reassessment: EmergencyTriageAssessment = {
    ...initialAssessment,
    id: 'assessment-2',
    assessmentType: 'REASSESSMENT',
    sequenceNumber: 2,
    airwayStatus: 'PATENT',
    breathingStatus: 'DISTRESS',
    circulationStatus: 'COMPROMISED',
    disabilityStatus: 'ALERT',
    exposureStatus: 'TRAUMA',
    recommendedOrientation: 'RESUSCITATION',
    assessedAt: '2026-07-13T15:10:00Z',
    createdAt: '2026-07-13T15:10:01Z',
  };

  beforeEach(async () => {
    api = {
      getTriageAssessments: vi.fn().mockReturnValue(of([initialAssessment])),
      addTriageAssessment: vi.fn().mockReturnValue(of(reassessment)),
    };

    await TestBed.configureTestingModule({
      imports: [EmergencyTriagePanelComponent],
      providers: [
        { provide: EmergencyApiService, useValue: api },
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

    fixture = TestBed.createComponent(EmergencyTriagePanelComponent);
    fixture.componentRef.setInput('emergencyId', 'emergency-1');
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the immutable triage history', () => {
    expect(api.getTriageAssessments).toHaveBeenCalledWith('emergency-1');
    expect(component.assessments()).toEqual([initialAssessment]);
  });

  it('submits an ABCDE reassessment and appends the response', () => {
    component.form.patchValue({
      triageLevel: 'ORANGE',
      hemodynamicStatus: 'UNSTABLE',
      airwayStatus: 'PATENT',
      breathingStatus: 'DISTRESS',
      circulationStatus: 'COMPROMISED',
      disabilityStatus: 'ALERT',
      exposureStatus: 'TRAUMA',
      respiratoryRate: 26,
      oxygenSaturation: 96,
      gcsScore: 15,
      painScore: 6,
      recommendedOrientation: 'RESUSCITATION',
      clinicalNotes: 'Amélioration partielle.',
    });

    component.submit();

    expect(api.addTriageAssessment).toHaveBeenCalledWith(
      'emergency-1',
      expect.objectContaining({
        triageLevel: 'ORANGE',
        abcdeAssessment: expect.objectContaining({
          breathingStatus: 'DISTRESS',
          recommendedOrientation: 'RESUSCITATION',
        }),
      }),
    );
    expect(component.assessments()).toEqual([initialAssessment, reassessment]);
    expect(component.success()).not.toBeNull();
  });
});