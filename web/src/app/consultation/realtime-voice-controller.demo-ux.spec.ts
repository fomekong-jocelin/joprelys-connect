import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { AiSessionResponse } from './ai-consultation-api.service';
import { AmbientAudioCaptureService, AmbientCaptureState } from './ambient-audio-capture.service';
import { RealtimeClinicalIntakeApiService } from './realtime-clinical-intake-api.service';
import {
  RealtimeVoiceBridgeService,
  RealtimeVoiceState,
} from './realtime-voice-bridge.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

describe('RealtimeVoiceControllerComponent demo UX', () => {
  let fixture: ComponentFixture<RealtimeVoiceControllerComponent>;
  let component: RealtimeVoiceControllerComponent;
  let state: BehaviorSubject<RealtimeVoiceState>;
  let ambientState: BehaviorSubject<AmbientCaptureState>;
  let bridge: {
    state$: BehaviorSubject<RealtimeVoiceState>;
    transcript$: Subject<any>;
    error$: Subject<string>;
    assistantTurnCompleted$: Subject<void>;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    isSupported: ReturnType<typeof vi.fn>;
    setMuted: ReturnType<typeof vi.fn>;
    speakApproved: ReturnType<typeof vi.fn>;
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
    bridge = {
      state$ : state,
      transcript$: new Subject(),
      error$: new Subject(),
      assistantTurnCompleted$: new Subject(),
      connect: vi.fn().mockResolvedValue(undefined),
      disconnect: vi.fn(),
      isSupported: vi.fn().mockReturnValue(true),
      setMuted: vi.fn(),
      speakApproved: vi.fn().mockReturnValue(true),
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
          provide: I18nService,
          useValue: {
            currentLanguage: () => 'fr',
            t: (_key: string, fallback?: string) => fallback ?? _key,
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
    expect(bridge.speakApproved).not.toHaveBeenCalledWith('Bonjour docteur. Je vous écoute.');
    expect(bridge.speakApproved).not.toHaveBeenCalled();
  });

  it('should vocalize a real pending clarification only once', () => {
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

    (component as any).speakPendingClarification();
    (component as any).speakPendingClarification();

    expect(bridge.speakApproved).toHaveBeenCalledTimes(1);
    expect(bridge.speakApproved).toHaveBeenCalledWith('Depuis combien de jours la douleur évolue-t-elle ?');
  });

  it('should show only the simple physician actions when healthy', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Je vous écoute');
    expect(text).toContain('Mettre en pause');
    expect(text).toContain('Passer en dictée');
    expect(text).toContain('Terminer');
    expect(text).not.toContain('Capture de sécurité active');
    expect(text).not.toContain('Preuve clinique ambient');
    expect(text).not.toContain('Copilote Realtime sécurisé');
  });

  it('should expose safety information only when realtime is actually degraded', () => {
    state.next({ ...state.value, connected: false });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Temps réel interrompu');
    expect(fixture.nativeElement.textContent).toContain('reste enregistrée localement');
  });

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
