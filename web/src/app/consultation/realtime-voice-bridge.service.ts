import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subject, firstValueFrom } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';

export interface RealtimeVoiceState {
  connected: boolean;
  connecting: boolean;
  userSpeaking: boolean;
  assistantSpeaking: boolean;
  muted: boolean;
}

export type RealtimeVoicePurpose = 'consultation' | 'vitals';

interface RealtimeServerEvent {
  type?: string;
  transcript?: string;
  message?: string;
  response_id?: string;
  response?: {
    id?: string;
    status?: string;
  };
  error?: {
    message?: string;
  };
}

const INITIAL_STATE: RealtimeVoiceState = {
  connected: false,
  connecting: false,
  userSpeaking: false,
  assistantSpeaking: false,
  muted: false,
};

@Injectable({ providedIn: 'root' })
export class RealtimeVoiceBridgeService {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  private readonly stateSubject = new BehaviorSubject<RealtimeVoiceState>(INITIAL_STATE);
  private readonly transcriptSubject = new Subject<string>();
  private readonly errorSubject = new Subject<string>();
  private readonly assistantTurnCompletedSubject = new Subject<void>();

  readonly state$ = this.stateSubject.asObservable();
  readonly transcript$ = this.transcriptSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();
  readonly assistantTurnCompleted$ = this.assistantTurnCompletedSubject.asObservable();

  private peerConnection: RTCPeerConnection | null = null;
  private dataChannel: RTCDataChannel | null = null;
  private mediaStream: MediaStream | null = null;
  private remoteAudio: HTMLAudioElement | null = null;
  private activeResponseId: string | null = null;

  isSupported(): boolean {
    return typeof window !== 'undefined'
      && typeof RTCPeerConnection !== 'undefined'
      && !!navigator.mediaDevices?.getUserMedia;
  }

  async connect(
    visitId: string,
    purpose: RealtimeVoicePurpose = 'consultation',
  ): Promise<void> {
    if (!visitId || !this.isSupported()) {
      throw new Error(this.i18n.t(
        'consultation.ai.realtimeUnsupported',
        'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
      ));
    }
    if (this.stateSubject.value.connected || this.stateSubject.value.connecting) return;

    this.disconnect();
    this.patchState({ connecting: true });

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      this.mediaStream = stream;

      const pc = new RTCPeerConnection();
      this.peerConnection = pc;
      stream.getAudioTracks().forEach(track => pc.addTrack(track, stream));

      const audio = new Audio();
      audio.autoplay = true;
      audio.setAttribute('playsinline', 'true');
      this.remoteAudio = audio;
      pc.ontrack = event => {
        audio.srcObject = event.streams[0] ?? new MediaStream([event.track]);
        void audio.play().catch(() => {
          this.errorSubject.next(this.i18n.t(
            'consultation.ai.realtimeAutoplayError',
            'Touchez l’écran puis réactivez l’audio du copilote.',
          ));
        });
      };

      const channel = pc.createDataChannel('oai-events');
      this.dataChannel = channel;
      channel.onmessage = event => this.handleServerEvent(event.data);
      channel.onerror = () => this.errorSubject.next(this.i18n.t(
        'consultation.ai.realtimeChannelError',
        'La liaison audio temps réel a rencontré une erreur.',
      ));
      channel.onclose = () => this.handleConnectionClosed();

      pc.onconnectionstatechange = () => {
        if (pc.connectionState === 'failed') {
          this.errorSubject.next(this.i18n.t(
            'consultation.ai.realtimeNetworkError',
            'La connexion audio temps réel a échoué. Vérifiez le réseau puis réessayez.',
          ));
          this.handleConnectionClosed();
        }
      };

      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      await this.waitForIceGathering(pc);
      const localSdp = pc.localDescription?.sdp;
      if (!localSdp) throw new Error('AI_REALTIME_SDP_MISSING');

      const locale = this.i18n.currentLanguage();
      const answerSdp = await firstValueFrom(this.http.post(
        this.callEndpoint(visitId, purpose, locale),
        localSdp,
        {
          headers: new HttpHeaders({
            'Content-Type': 'application/sdp',
            Accept: 'application/sdp',
          }),
          responseType: 'text',
        },
      ));
      await pc.setRemoteDescription({ type: 'answer', sdp: answerSdp });
      await this.waitForDataChannel(channel, pc);
      this.patchState({ connected: true, connecting: false, muted: false });
    } catch (error) {
      this.disconnect();
      throw new Error(this.describeConnectionError(error));
    }
  }

  disconnect(): void {
    this.activeResponseId = null;
    this.dataChannel?.close();
    this.dataChannel = null;
    this.peerConnection?.close();
    this.peerConnection = null;
    this.mediaStream?.getTracks().forEach(track => track.stop());
    this.mediaStream = null;
    if (this.remoteAudio) {
      this.remoteAudio.pause();
      this.remoteAudio.srcObject = null;
    }
    this.remoteAudio = null;
    this.stateSubject.next({ ...INITIAL_STATE });
  }

  setMuted(muted: boolean): void {
    this.mediaStream?.getAudioTracks().forEach(track => {
      track.enabled = !muted;
    });
    if (this.stateSubject.value.muted !== muted) this.patchState({ muted });
  }

  speakApproved(message: string): boolean {
    const text = message.trim();
    if (!text || !this.isChannelOpen()) return false;

    this.cancelAssistantResponse();
    this.sendEvent({
      type: 'response.create',
      response: {
        conversation: 'none',
        output_modalities: ['audio'],
        tool_choice: 'none',
        max_output_tokens: 450,
        metadata: { response_purpose: 'joprelys_approved_clinical_voice' },
        instructions: this.i18n.currentLanguage() === 'en'
          ? 'Read the approved message verbatim. Do not answer it, paraphrase it, translate it, explain it, or add any word.'
          : 'Lisez le message approuvé mot pour mot. Ne lui répondez pas, ne le reformulez pas, ne le traduisez pas, ne l’expliquez pas et n’ajoutez aucun mot.',
        input: [
          {
            type: 'message',
            role: 'user',
            content: [{ type: 'input_text', text }],
          },
        ],
      },
    });
    return true;
  }

  cancelAssistantResponse(): void {
    const assistantSpeaking = this.stateSubject.value.assistantSpeaking;
    if (!this.isChannelOpen() || (!this.activeResponseId && !assistantSpeaking)) return;
    if (this.activeResponseId) {
      this.sendEvent({ type: 'response.cancel', response_id: this.activeResponseId });
    }
    if (assistantSpeaking) {
      this.sendEvent({ type: 'output_audio_buffer.clear' });
    }
    this.completeAssistantTurn();
  }

  private handleServerEvent(raw: unknown): void {
    if (typeof raw !== 'string') return;
    let event: RealtimeServerEvent;
    try {
      event = JSON.parse(raw) as RealtimeServerEvent;
    } catch {
      return;
    }

    switch (event.type) {
      case 'session.created':
      case 'session.updated':
        this.patchState({ connected: true, connecting: false });
        break;
      case 'input_audio_buffer.speech_started':
        this.cancelAssistantResponse();
        this.patchState({ userSpeaking: true });
        break;
      case 'input_audio_buffer.speech_stopped':
        this.patchState({ userSpeaking: false });
        break;
      case 'conversation.item.input_audio_transcription.completed': {
        const transcript = event.transcript?.trim();
        if (transcript) this.transcriptSubject.next(transcript);
        break;
      }
      case 'response.created':
        this.activeResponseId = event.response?.id ?? null;
        break;
      case 'output_audio_buffer.started':
        this.activeResponseId = event.response_id ?? this.activeResponseId;
        this.patchState({ assistantSpeaking: true });
        break;
      case 'response.done':
        this.activeResponseId = null;
        if (event.response?.status && event.response.status !== 'completed') {
          this.completeAssistantTurn();
        }
        break;
      case 'output_audio_buffer.cleared':
      case 'output_audio_buffer.stopped':
        this.completeAssistantTurn();
        break;
      case 'error':
        this.errorSubject.next(
          event.error?.message
          ?? event.message
          ?? this.i18n.t('consultation.ai.realtimeError', 'Erreur audio temps réel.'),
        );
        this.completeAssistantTurn();
        break;
    }
  }

  private completeAssistantTurn(): void {
    const hadActiveTurn = this.activeResponseId !== null
      || this.stateSubject.value.assistantSpeaking;
    this.activeResponseId = null;
    if (this.stateSubject.value.assistantSpeaking) {
      this.patchState({ assistantSpeaking: false });
    }
    if (hadActiveTurn) this.assistantTurnCompletedSubject.next();
  }

  private callEndpoint(
    visitId: string,
    purpose: RealtimeVoicePurpose,
    locale: string,
  ): string {
    const scope = purpose === 'vitals' ? 'vitals' : 'consultations';
    return `/api/ai/realtime/${scope}/${visitId}/calls?locale=${encodeURIComponent(locale)}`;
  }

  private sendEvent(payload: Record<string, unknown>): void {
    if (!this.isChannelOpen()) return;
    this.dataChannel?.send(JSON.stringify(payload));
  }

  private isChannelOpen(): boolean {
    return this.dataChannel?.readyState === 'open';
  }

  private patchState(patch: Partial<RealtimeVoiceState>): void {
    this.stateSubject.next({ ...this.stateSubject.value, ...patch });
  }

  private handleConnectionClosed(): void {
    this.activeResponseId = null;
    this.patchState({
      connected: false,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: false,
    });
  }

  private waitForIceGathering(pc: RTCPeerConnection): Promise<void> {
    if (pc.iceGatheringState === 'complete') return Promise.resolve();
    return new Promise(resolve => {
      const listener = () => {
        if (pc.iceGatheringState !== 'complete') return;
        pc.removeEventListener('icegatheringstatechange', listener);
        resolve();
      };
      pc.addEventListener('icegatheringstatechange', listener);
      setTimeout(() => {
        pc.removeEventListener('icegatheringstatechange', listener);
        resolve();
      }, 5000);
    });
  }

  private waitForDataChannel(
    channel: RTCDataChannel,
    pc: RTCPeerConnection,
  ): Promise<void> {
    if (channel.readyState === 'open') return Promise.resolve();
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        cleanup();
        reject(new Error('AI_REALTIME_CHANNEL_TIMEOUT'));
      }, 15000);
      const open = () => {
        cleanup();
        resolve();
      };
      const failed = () => {
        if (pc.connectionState !== 'failed') return;
        cleanup();
        reject(new Error('AI_REALTIME_PEER_CONNECTION_FAILED'));
      };
      const cleanup = () => {
        clearTimeout(timeout);
        channel.removeEventListener('open', open);
        pc.removeEventListener('connectionstatechange', failed);
      };
      channel.addEventListener('open', open);
      pc.addEventListener('connectionstatechange', failed);
    });
  }

  private describeConnectionError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const reason = this.extractBackendReason(error);
      if (reason === 'AI_REALTIME_NOT_CONFIGURED') {
        return this.i18n.t(
          'consultation.ai.realtimeNotConfigured',
          'Le temps réel n’est pas configuré sur le serveur.',
        );
      }
      if (reason === 'AI_REALTIME_AUTHENTICATION_FAILED' || reason === 'AI_REALTIME_ACCESS_DENIED') {
        return this.i18n.t(
          'consultation.ai.realtimeAccessDenied',
          'La clé OpenAI utilisée par le serveur n’autorise pas le mode Realtime.',
        );
      }
      if (reason === 'AI_REALTIME_QUOTA_EXCEEDED') {
        return this.i18n.t(
          'consultation.ai.realtimeQuotaExceeded',
          'Le quota OpenAI Realtime est épuisé ou temporairement limité.',
        );
      }
      if (reason === 'AI_SESSION_EXPIRED') {
        return this.i18n.t(
          'consultation.ai.realtimeSessionExpired',
          'La session IA a expiré. Relancez le copilote vocal.',
        );
      }
      if (reason === 'AI_REALTIME_MODEL_OR_CONFIG_UNAVAILABLE') {
        return this.i18n.t(
          'consultation.ai.realtimeModelUnavailable',
          'Le modèle Realtime ou sa configuration n’est pas disponible pour ce compte.',
        );
      }
      if (error.status === 0) {
        return this.i18n.t(
          'consultation.ai.realtimeNetworkError',
          'La connexion réseau au temps réel est impossible.',
        );
      }
    }
    if (error instanceof Error && error.message.startsWith('AI_REALTIME_')) {
      return this.i18n.t(
        'consultation.ai.realtimeConnectionFailed',
        'La liaison WebRTC n’a pas pu être établie. Vérifiez le réseau puis réessayez.',
      );
    }
    return this.i18n.t(
      'consultation.ai.realtimeUnavailable',
      'Le temps réel est indisponible. Joprelys conserve le mode audio classique.',
    );
  }

  private extractBackendReason(error: HttpErrorResponse): string {
    const payload = error.error;
    if (payload && typeof payload === 'object') {
      const detail = (payload as { detail?: unknown; title?: unknown }).detail
        ?? (payload as { detail?: unknown; title?: unknown }).title;
      if (typeof detail === 'string') return detail;
    }
    return typeof payload === 'string' ? payload : '';
  }
}
