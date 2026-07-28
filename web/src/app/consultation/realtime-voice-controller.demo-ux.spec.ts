import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, NEVER, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService, AiSessionResponse } from './ai-consultation-api.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

const FR: Record<string, string> = {
  'consultation.ai.focusSecureListening': 'Écoute sécurisée',
  'consultation.ai.focusSafetyActive': 'Sauvegarde audio active',
  'consultation.ai.simpleListening': 'Je vous écoute',
  'consultation.ai.simpleListeningHelp': 'Parlez naturellement. La transcription apparaît dès qu’une phrase est finalisée.',
  'consultation.ai.focusTranscriptTitle': 'Transcription en direct',
  'consultation.ai.focusTranscriptWaiting': 'Parlez normalement. La dernière phrase réellement reconnue apparaîtra ici.',
  'consultation.ai.pauseListeningShort': 'Pause',
  'consultation.ai.finishListening': 'Terminer',
  'consultation.ai.switchToDictation': 'Passer en dictée',
  'consultation.ai.focusAudioUnavailable': 'Audio non confirmé',
  'consultation.ai.reconnectingSimple': 'Reconnexion audio…',
  'consultation.ai.reconnectingProtectedHelp': 'Votre consultation reste protégée pendant la reconnexion.',
  'consultation.ai.focusTranscriptNoChannel': 'La transcription commencera dès que le canal audio sera confirmé.',
  'consultation.ai.realtimeProtectedReconnectSimple': 'Temps réel interrompu. La consultation reste enregistrée localement et sera synchronisée automatiquement.',
};

describe('RealtimeVoiceControllerComponent demo UX', () => {
  let fixture: ComponentFixture<RealtimeVoiceControllerComponent>;
  let component: RealtimeVoiceControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let transcripts: Subject<any>;
  let synthesizeSpeech: ReturnType<typeof vi.fn>;
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
    synthesizeSpeech = vi.fn().mockReturnValue(NEVER);
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
        {
          provide: RealtimeClinicalIntakeApiService,
          useValue: { ingest: vi.fn().mockReturnValue(of({})) },
        },
        {
          provide: AiConsultationApiService,
          useValue: {
            sendRealtimeTranscript: vi.fn().mockReturnValue(of(messageResponse())),
            answerRealtimeClarification: vi.fn().mockReturnValue(of(messageResponse())),
            synthesizeSpeech,
          },
        },
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
    component.session = sessionWithGreeting();
    (component as any).connectedVisitId = 'visit-1';
    state.next({ ...state.value, connected: true });
    fixture.detectChanges();
  });

  afterEach(() => {
    component.ngOnDestroy();
    TestBed.resetTestingModule();
  });

  it('should never vocalize the generic greeting when realtime connects', () => {
    expect((component as any).speakPendingClarification).toBeUndefined();
    expect((bridge as any).speakApproved).toBeUndefined();
    expect(synthesizeSpeech).not.toHaveBeenCalled();
  });

  it('should vocalize a pending clinical clarification once through the dedicated TTS channel', () => {
    component.session = {
      ...sessionWithGreeting(),
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
    state.next({ ...state.value, connected: false });
    state.next({ ...state.value, connected: true });

    expect(component.session.clarifications[0].question)
      .toBe('Depuis combien de jours la douleur évolue-t-elle ?');
    expect((component as any).speakApproved).toBeUndefined();
    expect(synthesizeSpeech).toHaveBeenCalledTimes(1);
    expect(synthesizeSpeech)
      .toHaveBeenCalledWith('Depuis combien de jours la douleur évolue-t-elle ?');
  });

  it('should stop TTS on barge-in without muting realtime capture', () => {
    const playbackStop = vi.spyOn((component as any).voicePlayback, 'stop');
    bridge.setMuted.mockClear();

    state.next({ ...state.value, userSpeaking: true });

    expect(playbackStop).toHaveBeenCalled();
    expect(bridge.setMuted).not.toHaveBeenCalledWith(true);
  });

  it('should show only the focus physician actions when healthy', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('IA en cours...');
    expect(text).toContain('Écoute en cours... Parlez naturellement');
    expect(text).toContain('Arrêter');
    expect(text).toContain('Conseil');
    expect(fixture.nativeElement.querySelector('app-voice-listening-surface')).not.toBeNull();
    expect(text).not.toContain('Capture de sécurité active');
    expect(text).not.toContain('Preuve clinique ambient');
    expect(text).not.toContain('Copilote Realtime sécurisé');
  });

  it('should display the last transcript really received from realtime', () => {
    transcripts.next({
      transcript: 'Patient sans fièvre depuis trois jours',
      confidence: 0.91,
      eventId: 'event-1',
      itemId: 'item-1',
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Patient sans fièvre depuis trois jours');
  });

  it('should expose safety information only when realtime is actually degraded', () => {
    state.next({ ...state.value, connected: false });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Temps réel interrompu');
    expect(fixture.nativeElement.textContent).toContain('reste enregistrée localement');
  });

  function messageResponse() {
    return {
      sessionId: 'session-1',
      transcript: 'Patient sans fièvre depuis trois jours',
      draft: {},
      changedFields: [],
      assistantMessage: '',
      needsClarification: false,
      conversation: [],
      clarifications: [],
      revisions: [],
      expiresAt: '2026-07-26T22:00:00Z',
    };
  }

  function sessionWithGreeting(): AiSessionResponse {
    return {
      sessionId: 'session-1',
      visitId: 'visit-1',
      status: 'ACTIVE',
      expiresAt: '2026-07-26T22:00:00Z',
      draft: {},
      transcript: null,
      pendingTranscript: null,
      transcriptStatus: 'NONE',
      conversation: [],
      clarifications: [],
      revisions: [],
      assistantMessage: 'Bonjour docteur. Je vous écoute.',
      needsClarification: false,
    };
  }
});
