import { HttpClient, HttpHeaders } from '@angular/common/http';
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

  readonly state$ = this.stateSubject.asObservable();
  readonly transcript$ = this.transcriptSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();

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
      throw new Error('AI_REALTIME_UNSUPPORTED');
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
      channel.onclose = () => this.patchState({
        connected: false,
        connecting: false,
        userSpeaking: false,
        assistantSpeaking: false,
      });

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
      await this.waitForDataChannel(channel);
      this.patchState({ connected: true, connecting: false, muted: false });
    } catch (error) {
      this.disconnect();
      throw error;
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
    this.patchState({ muted });
  }

  speakApproved(message: string): boolean {
    const text = message.trim();
    if (!text || !this.isChannelOpen()) return false;

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
    if (!this.isChannelOpen() || !this.activeResponseId) return;
    this.sendEvent({ type: 'response.cancel', response_id: this.activeResponseId });
    this.sendEvent({ type: 'output_audio_buffer.clear' });
    this.activeResponseId = null;
    this.patchState({ assistantSpeaking: false });
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
        this.patchState({ assistantSpeaking: true });
        break;
      case 'response.done':
      case 'output_audio_buffer.cleared':
        this.activeResponseId = null;
        this.patchState({ assistantSpeaking: false });
        break;
      case 'error':
        this.errorSubject.next(
          event.error?.message
          ?? event.message
          ?? this.i18n.t('consultation.ai.realtimeError', 'Erreur audio temps réel.'),
        );
        break;
    }
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
      }, 2000);
    });
  }

  private waitForDataChannel(channel: RTCDataChannel): Promise<void> {
    if (channel.readyState === 'open') return Promise.resolve();
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        cleanup();
        reject(new Error('AI_REALTIME_CHANNEL_TIMEOUT'));
      }, 5000);
      const onOpen = () => {
        cleanup();
        resolve();
      };
      const onClose = () => {
        cleanup();
        reject(new Error('AI_REALTIME_CHANNEL_CLOSED'));
      };
      const cleanup = () => {
        clearTimeout(timeout);
        channel.removeEventListener('open', onOpen);
        channel.removeEventListener('close', onClose);
      };
      channel.addEventListener('open', onOpen);
      channel.addEventListener('close', onClose);
    });
  }
}
