import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AmbientSpeakerReviewComponent } from './ambient-speaker-review.component';
import {
  AmbientSpeakerReviewService,
  AmbientTranscriptItem,
} from './ambient-speaker-review.service';

describe('AmbientSpeakerReviewComponent demo-safe ambiguity flow', () => {
  let fixture: ComponentFixture<AmbientSpeakerReviewComponent>;
  let service: {
    transcript: ReturnType<typeof vi.fn>;
    assignSpeaker: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    service = {
      transcript: vi.fn().mockReturnValue(of({ visitId: 'visit-1', items: [unknownItem()] })),
      assignSpeaker: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AmbientSpeakerReviewComponent],
      providers: [
        { provide: AmbientSpeakerReviewService, useValue: service },
        {
          provide: I18nService,
          useValue: { t: (_key: string, fallback?: string) => fallback ?? _key },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AmbientSpeakerReviewComponent);
    fixture.componentRef.setInput('visitId', 'visit-1');
    fixture.detectChanges();
  });

  afterEach(() => TestBed.resetTestingModule());

  it('should ask one plain-language question and never infer patient automatically', () => {
    expect(fixture.nativeElement.textContent).toContain('Une phrase doit être vérifiée');
    expect(fixture.nativeElement.textContent).toContain('Qui a dit cette phrase ?');
    expect(fixture.nativeElement.textContent).toContain('Texte clinique à attribuer');
    expect(fixture.nativeElement.textContent).toContain('Médecin');
    expect(fixture.nativeElement.textContent).toContain('Patient');
    expect(fixture.nativeElement.textContent).not.toContain('source A');
    expect(service.assignSpeaker).not.toHaveBeenCalled();
  });

  it('should disappear immediately after the ambiguity is resolved', () => {
    const original = unknownItem();
    const corrected = {
      ...original,
      id: 'corrected-1',
      speakerType: 'DOCTOR' as const,
      speakerLabel: 'human:DOCTOR',
      supersedesItemId: original.id,
    };
    service.assignSpeaker.mockReturnValue(of(corrected));

    fixture.componentInstance.assign(original, 'DOCTOR');
    fixture.detectChanges();

    expect(service.assignSpeaker).toHaveBeenCalledWith('visit-1', original, 'DOCTOR');
    expect(fixture.componentInstance.reviewItems()).toHaveLength(0);
    expect(fixture.componentInstance.shouldRender()).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('Qui a dit cette phrase ?');
  });

  it('should stay invisible when all speakers are already attributed', () => {
    service.transcript.mockReturnValue(of({ visitId: 'visit-1', items: [doctorItem()] }));
    fixture.componentInstance.load();
    fixture.detectChanges();

    expect(fixture.componentInstance.reviewItems()).toHaveLength(0);
    expect(fixture.componentInstance.shouldRender()).toBe(false);
    expect(fixture.nativeElement.textContent.trim()).toBe('');
  });

  it('should ignore pure filler utterances instead of asking the clinician to classify them', () => {
    service.transcript.mockReturnValue(of({
      visitId: 'visit-1',
      items: [{ ...unknownItem(), text: 'Oh.' }, { ...unknownItem(), id: 'hello', text: 'Hello.' }],
    }));
    fixture.componentInstance.load();
    fixture.detectChanges();

    expect(fixture.componentInstance.reviewItems()).toHaveLength(0);
    expect(fixture.componentInstance.shouldRender()).toBe(false);
  });

  it('should refresh the ledger instead of forcing a stale speaker decision', () => {
    const original = unknownItem();
    const current = {
      ...original,
      id: 'current-leaf',
      speakerType: 'PATIENT' as const,
      speakerLabel: 'human:PATIENT',
      supersedesItemId: original.id,
    };
    service.assignSpeaker.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { detail: 'AI_AMBIENT_TRANSCRIPT_ITEM_SUPERSEDED' },
    })));
    service.transcript.mockReturnValueOnce(of({ visitId: 'visit-1', items: [current] }));

    fixture.componentInstance.assign(original, 'DOCTOR');
    fixture.detectChanges();

    expect(service.transcript).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.items()).toEqual([current]);
    expect(fixture.nativeElement.textContent).toContain('Cette phrase a changé');
  });

  function unknownItem(): AmbientTranscriptItem {
    return {
      id: '11111111-1111-1111-1111-111111111111',
      sequence: 1,
      sourceEventId: 'chunk-1:seg-1',
      source: 'AMBIENT_DIARIZED',
      speakerType: 'UNSPECIFIED',
      speakerLabel: 'A',
      text: 'Texte clinique à attribuer',
      locale: 'fr',
      startOffsetMs: 1_000,
      endOffsetMs: 2_000,
      status: 'FINAL',
      supersedesItemId: null,
      createdAt: '2026-07-26T17:00:00Z',
    };
  }

  function doctorItem(): AmbientTranscriptItem {
    return {
      ...unknownItem(),
      id: '22222222-2222-2222-2222-222222222222',
      speakerType: 'DOCTOR',
      speakerLabel: 'doctor',
    };
  }
});
