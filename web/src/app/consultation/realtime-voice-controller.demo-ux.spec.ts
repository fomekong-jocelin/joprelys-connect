import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

const FR: Record<string, string> = {
  'consultation.ai.focusTranscriptTitle': 'Transcription en direct',
  'consultation.ai.focusTranscriptWaiting': 'Parlez normalement. La dernière phrase réellement reconnue apparaîtra ici.',
  'consultation.ai.focusTranscriptNoChannel': 'La transcription commencera dès que le canal audio sera confirmé.',
  'consultation.ai.realtimeProtectedReconnectSimple': 'Temps réel interrompu. La consultation reste enregistrée localement et sera synchronisée automatiquement.',
  'consultation.ai.correctTranscript': 'Corriger',
};

describe('RealtimeVoiceControllerComponent focused capture UX', () => {
  let fixture: ComponentFixture<RealtimeVoiceControllerComponent>;
  let component: RealtimeVoiceControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let transcripts: Subject<any>;
  let bridge: {
    state$: BehaviorSubject<RealtimeVoiceState>;
    transcript$: Subject<any>;
    error$: Subject<string>;
    assistantTurnCompleted$: Subject<void>;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    isSupported: ReturnType<typeof vi.fn>;
    setMuted: ReturnType<typeof vi.fn>;
  };
  let intake: {
    ingest: ReturnType<typeof vi.fn>;
    list: ReturnType<typeof vi.fn>;
    correct: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    state = new BehaviorSubject<RealtimeVoiceState>({
      connected: false,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: false,
      muted: false,
    });
    ambientState = new BehaviorSubject<AmbientCaptureState>({
      supported: true,
      active: true,
      starting: false,
      recovering: false,
      pendingChunks: 0,
      pendingBytes: 0,
      uploading: false,
      online: true,
      storagePressure: false,
      lastError: null,
    });
    transcripts = new Subject();
    bridge = {
      state$: state,
      transcript$: transcripts,
      error$: new Subject(),
      assistantTurnCompleted$: new Subject(),
      connect: vi.fn().mockResolvedValue(undefined),
      disconnect: vi.fn(),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
    };
    intake = {
      ingest: vi.fn().mockImplementation(
        (visitId: string, transcript: string, confidence: number | null, eventId: string, itemId?: string) =>
          of(ack(visitId, transcript, confidence, eventId, itemId)),
      ),
      list: vi.fn().mockReturnValue(of([])),
      correct: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RealtimeVoiceControllerComponent],
      providers: [
        { provide: RealtimeVoiceBridgeService, useValue: bridge },
        {
          provide: AmbientAudioCaptureService,
          useValue: {
            state$: ambientState,
            start: vi.fn().mockResolvedValue(undefined),
            stop: vi.fn().mockResolvedValue(undefined),
            mediaStreamForVisit: vi.fn().mockReturnValue({} as MediaStream),
          },
        },
        { provide: RealtimeClinicalIntakeApiService, useValue: intake },
        {
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (key: string, fallback?: string) => FR[key] ?? fallback ?? key,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RealtimeVoiceControllerComponent);
    component = fixture.componentInstance;
    component.visitId = 'visit-1';
    component.enabled = true;
    component.session = activeSession();
    (component as any).connectedVisitId = 'visit-1';
    state.next({ ...state.value, connected: true });
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
    TestBed.resetTestingModule();
  });

  it('shows only secure recording controls and transcript while healthy', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Enregistrement sécurisé');
    expect(text).toContain('Écoute en cours... Parlez naturellement');
    expect(text).toContain('Arrêter');
    expect(text).toContain('Transcription en direct');
    expect(text).not.toContain('Voulez-vous appliquer');
    expect(text).not.toContain('Précision demandée');
    expect(text).not.toContain('Modifications à valider');
  });

  it('never vocalizes a pending clarification during continuous capture', () => {
    component.session = {
      ...activeSession(),
      clarifications: [{
        id: 'clar-1',
        field: 'symptoms',
        question: 'Depuis combien de jours la douleur évolue-t-elle ?',
        status: 'PENDING',
        options: [],
        createdAt: '2026-07-26T20:00:00Z',
        answer: null,
        resolvedAt: null,
      }],
    };

    state.next({ ...state.value, connected: false });
    state.next({ ...state.value, connected: true });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Depuis combien de jours');
    expect((component as any).voicePlayback).toBeUndefined();
  });

  it('displays the exact transcript after its durable ACK', () => {
    transcripts.next({
      transcript: 'Patient sans fièvre depuis trois jours',
      confidence: 0.91,
      eventId: 'event-1',
      itemId: 'item-1',
    });
    fixture.detectChanges();

    expect(intake.ingest).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('Patient sans fièvre depuis trois jours');
  });

  it('marks low-confidence transcript for review instead of hiding it', () => {
    transcripts.next({
      transcript: 'Le patient nie toute fièvre',
      confidence: 0.2,
      eventId: 'event-low',
      itemId: 'item-low',
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Le patient nie toute fièvre');
    expect(text).toContain('À vérifier');
  });

  it('shows safety information only when realtime is actually degraded', () => {
    state.next({ ...state.value, connected: false });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Temps réel interrompu');
    expect(fixture.nativeElement.textContent).toContain('reste enregistrée localement');
  });

  function ack(
    visitId: string,
    transcript: string,
    confidence: number | null,
    eventId: string,
    itemId?: string,
  ): RealtimeClinicalIntakeAck {
    const normalizedConfidence = confidence ?? 0;
    return {
      id: `ack-${eventId}`,
      visitId,
      source: 'CONSULTATION',
      sequence: 1,
      eventId,
      itemId: itemId ?? null,
      transcript,
      originalTranscript: null,
      confidence: normalizedConfidence,
      reviewRequired: confidence === null || normalizedConfidence < 0.35,
      correctionCount: 0,
      correctedAt: null,
      captureStatus: 'PENDING',
      receivedAt: '2026-07-26T22:00:00Z',
    };
  }

  function activeSession() {
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
      status: 'ACTIVE',
      expiresAt: '2026-07-26T22:00:00Z',
      draft: {},
      transcript: null,
      pendingTranscript: null,
      transcriptStatus: 'NONE' as const,
      conversation: [],
      clarifications: [],
      revisions: [],
      assistantMessage: 'Bonjour docteur. Je vous écoute.',
      needsClarification: false,
    };
  }
});
