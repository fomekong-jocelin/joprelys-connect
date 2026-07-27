import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  ClinicalNoteProjection,
  ClinicalNoteProjectionApiService,
  ClinicalNoteValidation,
} from './clinical-note-projection-api.service';
import { LinkedEvidenceNotePanelComponent } from './linked-evidence-note-panel.component';

const FR: Record<string, string> = {
  'consultation.linkedEvidence.title': 'Note clinique sourcée',
  'consultation.linkedEvidence.subtitle': 'Projection déterministe sourcée.',
  'consultation.linkedEvidence.validated': 'Validée',
  'consultation.linkedEvidence.reviewRequired': 'À relire',
  'consultation.linkedEvidence.refresh': 'Actualiser',
  'consultation.linkedEvidence.loading': 'Construction…',
  'consultation.linkedEvidence.showEvidence': 'Voir la preuve',
  'consultation.linkedEvidence.hideEvidence': 'Masquer la preuve',
  'consultation.linkedEvidence.primaryEvidence': 'Preuve principale',
  'consultation.linkedEvidence.audioRetentionNotice': 'Le segment audio brut n’est pas conservé après l’ACK.',
  'consultation.linkedEvidence.version': 'Version relue :',
  'consultation.linkedEvidence.validationSafety': 'Toute modification exige une nouvelle validation.',
  'consultation.linkedEvidence.validate': 'Valider cette version',
  'consultation.linkedEvidence.validating': 'Validation…',
  'consultation.linkedEvidence.validatedAt': 'Validée le',
  'consultation.linkedEvidence.validationSuccess': 'Cette version a été validée.',
  'consultation.linkedEvidence.validationFailed': 'Validation impossible.',
  'consultation.linkedEvidence.stale': 'La note a changé depuis votre dernière lecture.',
  'consultation.linkedEvidence.staleHelp': 'Relisez la version courante.',
  'consultation.linkedEvidence.section.hpi': 'Histoire de la maladie actuelle',
  'consultation.linkedEvidence.authority.patient': 'Rapporté par le patient',
  'consultation.linkedEvidence.polarity.positive': 'Présent',
  'consultation.linkedEvidence.laterality.right': 'Droite',
  'consultation.linkedEvidence.speaker.patient': 'Patient',
  'consultation.linkedEvidence.fact.symptom': 'Symptôme',
};

describe('LinkedEvidenceNotePanelComponent', () => {
  let fixture: ComponentFixture<LinkedEvidenceNotePanelComponent>;
  let api: {
    getProjection: ReturnType<typeof vi.fn>;
    getValidationHistory: ReturnType<typeof vi.fn>;
    validateProjection: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      getProjection: vi.fn().mockReturnValue(of(projection())),
      getValidationHistory: vi.fn().mockReturnValue(of({ visitId: 'visit-1', validations: [] })),
      validateProjection: vi.fn().mockReturnValue(of(validation())),
    };

    await TestBed.configureTestingModule({
      imports: [LinkedEvidenceNotePanelComponent],
      providers: [
        { provide: ClinicalNoteProjectionApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => FR[key] ?? fallback ?? key,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LinkedEvidenceNotePanelComponent);
    fixture.componentRef.setInput('visitId', 'visit-1');
    fixture.detectChanges();
  });

  it('renders a deterministic note while evidence stays collapsed by default', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Note clinique sourcée');
    expect(text).toContain('Histoire de la maladie actuelle');
    expect(text).toContain('Douleur abdominale');
    expect(text).toContain('Rapporté par le patient');
    expect(text).toContain('Voir la preuve');
    expect(text).not.toContain('depuis trois jours en bas à droite');
  });

  it('shows the exact FINAL transcript quote, speaker and timestamp on demand', () => {
    fixture.componentInstance.toggleEvidence('fact-1');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('depuis trois jours en bas à droite');
    expect(text).toContain('Patient');
    expect(text).toContain('00:04–00:07');
    expect(text).toContain('Preuve principale');
    expect(text).toContain('segment audio brut n’est pas conservé');
    expect(fixture.nativeElement.querySelector('audio')).toBeNull();
  });

  it('validates exactly the reviewed projection version and marks it validated', () => {
    fixture.componentInstance.validateCurrentProjection();
    fixture.detectChanges();

    expect(api.validateProjection).toHaveBeenCalledTimes(1);
    expect(api.validateProjection.mock.calls[0][0]).toBe('visit-1');
    expect(api.validateProjection.mock.calls[0][2]).toBe('clinical-note-projection-v1:abc123456789');
    expect(api.validateProjection.mock.calls[0][1]).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
    );
    expect(fixture.nativeElement.textContent).toContain('Validée');
    expect(fixture.nativeElement.textContent).toContain('Cette version a été validée.');
  });

  it('refuses a stale validation, reloads the projection and requires a new review', () => {
    api.validateProjection.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { detail: 'AI_CLINICAL_NOTE_PROJECTION_STALE' },
    })));
    api.getProjection.mockClear();

    fixture.componentInstance.validateCurrentProjection();
    fixture.detectChanges();

    expect(api.getProjection).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('La note a changé depuis votre dernière lecture.');
    expect(fixture.nativeElement.textContent).toContain('Valider cette version');
  });

  it('ignores a late projection response from a previous visit', () => {
    const delayedVisit2 = new Subject<ClinicalNoteProjection>();
    api.getProjection
      .mockReturnValueOnce(delayedVisit2)
      .mockReturnValueOnce(of(projection('visit-3', 'clinical-note-projection-v1:visit3')));
    api.getValidationHistory.mockReturnValue(of({ visitId: 'visit-3', validations: [] }));

    fixture.componentRef.setInput('visitId', 'visit-2');
    fixture.detectChanges();
    expect(fixture.componentInstance.loading()).toBe(true);

    fixture.componentRef.setInput('visitId', 'visit-3');
    fixture.detectChanges();
    expect(fixture.componentInstance.projection()?.visitId).toBe('visit-3');
    expect(fixture.componentInstance.loading()).toBe(false);

    delayedVisit2.next(projection('visit-2', 'clinical-note-projection-v1:late'));
    delayedVisit2.complete();
    fixture.detectChanges();

    expect(fixture.componentInstance.projection()?.visitId).toBe('visit-3');
    expect(fixture.componentInstance.projection()?.projectionVersion)
      .toBe('clinical-note-projection-v1:visit3');
  });

  function projection(
    visitId = 'visit-1',
    projectionVersion = 'clinical-note-projection-v1:abc123456789',
  ): ClinicalNoteProjection {
    return {
      visitId,
      projectionVersion,
      maxFactSequence: 7,
      sections: [{
        code: 'HISTORY_OF_PRESENT_ILLNESS',
        entries: [{
          factId: 'fact-1',
          factSequence: 7,
          factType: 'SYMPTOM',
          authority: 'PATIENT_REPORTED',
          conceptCode: null,
          conceptText: 'Douleur abdominale',
          polarity: 'POSITIVE',
          valuePrimary: null,
          valueSecondary: null,
          unitCode: null,
          temporalityText: 'depuis 3 jours',
          laterality: 'RIGHT',
          frequencyText: null,
          routeText: null,
          evidence: [{
            transcriptItemId: 'item-1',
            speakerType: 'PATIENT',
            speakerLabel: null,
            startOffsetMs: 4_200,
            endOffsetMs: 7_200,
            quoteStartChar: 12,
            quoteEndChar: 47,
            quoteText: 'depuis trois jours en bas à droite',
            primarySupport: true,
          }],
        }],
      }],
    };
  }

  function validation(): ClinicalNoteValidation {
    return {
      id: 'db-validation-1',
      validationId: '11111111-1111-4111-8111-111111111111',
      visitId: 'visit-1',
      projectionVersion: 'clinical-note-projection-v1:abc123456789',
      projectionSchemaVersion: 'clinical-note-projection-v1',
      maxFactSequence: 7,
      validatedByUserId: 'doctor-1',
      validatedAt: '2026-07-27T16:30:00Z',
      facts: [{
        factId: 'fact-1',
        factSequence: 7,
        sectionCode: 'HISTORY_OF_PRESENT_ILLNESS',
        position: 0,
      }],
    };
  }
});
