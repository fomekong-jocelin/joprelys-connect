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

export interface RealtimeTranscriptTurn {
  transcript: string;
  confidence: number | null;
  eventId?: string;
  itemId?: string;
}

export type RealtimeVoicePurpose = 'consultation' | 'vitals';
export type RealtimeMediaStreamProvider = () => MediaStream | null;

interface RealtimeServerEvent {
  type?: string;
  transcript?: string;
  message?: string;
  event_id?: string;
  item_id?: string;
  response_id?: string;
  logprobs?: Array<{
    logprob?: number;
    token?: string;
  }>;
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
  private readonly transcriptSubject = new Subject<RealtimeTranscriptTurn>();
  private readonly errorSubject = new Subject<string>();
  private readonly assistantTurnCompletedSubject = new Subject<void>();

  readonly state$ = this.stateSubject.asObservable();
  readonly transcript$ = this.transcriptSubject.asObservable();
  readonly error$ = this.errorSubject.asObservable();
  readonly assistantTurnCompleted$ = this.assistantTurnCompletedSubject.asObservable();

  private peerConnection: RTCPeerConnection | null = null;
  private dataChannel: RTCDataChannel | null = null;
  private mediaStream: MediaStream | null = null;
  private mediaStreamOwned = false;
  private audioSender: RTCRtpSender | null = null;
  private senderMuteGeneration = 0;
  private remoteAudio: HTMLAudioElement | null = null;
  private activeResponseId: string | null = null;

  private desiredVisitId = '';
  private desiredPurpose: RealtimeVoicePurpose = 'consultation';
  private desiredMediaStreamProvider: RealtimeMediaStreamProvider | null = null;
  private shouldStayConnected = false;
  private requestedMuted = false;
  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private disconnectedTimer: ReturnType<typeof setTimeout> | null = null;
  private trackMutedTimer: ReturnType<typeof setTimeout> | null = null;
  private tearingDown = false;

  isSupported(): boolean {
    return typeof window !== 'undefined'
      && typeof RTCPeerConnection !== 'undefined'
      && !!navigator.mediaDevices?.getUserMedia;
  }

  async connect(
    visitId: string,
    purpose: RealtimeVoicePurpose = 'consultation',
    mediaStreamProvider?: RealtimeMediaStreamProvider,
  ): Promise<void> {
    if (!visitId || !this.isSupported()) {
      throw new Error(this.i18n.t(
        'consultation.ai.realtimeUnsupported',
        'Ce navigateur ne prend pas en charge la connexion audio temps réel.',
      ));
    }

    const targetChanged = this.desiredVisitId !== visitId || this.desiredPurpose !== purpose;
    this.desiredVisitId = visitId;
    this.desiredPurpose = purpose;
    this.desiredMediaStreamProvider = mediaStreamProvider ?? null;
    this.shouldStayConnected = true;
    if (targetChanged) this.reconnectAttempts = 0;

    this.clearReconnectTimer();
    if (this.stateSubject.value.connected || this.stateSubject.value.connecting) return;

    try {
      await this.establishConnection();
    } catch (error) {
      if (this.shouldStayConnected) this.scheduleReconnect();
      throw new Error(this.describeConnectionError(error));
    }
  }

  disconnect(): void {
    this.shouldStayConnected = false;
    this.desiredVisitId = '';
    this.reconnectAttempts = 0;
    this.requestedMuted = false;
    this.clearReconnectTimer();
    this.clearDisconnectedTimer();
    this.clearTrackMutedTimer();
    this.teardownTransport();
    this.desiredMediaStreamProvider = null;
    this.stateSubject.next({ ...INITIAL_STATE });
  }

  setMuted(muted: boolean): void {
    this.requestedMuted = muted;
    if (this.stateSubject.value.muted !== muted) this.patchState({ muted });
    void this.syncOutboundTrack();
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

  private async establishConnection(): Promise<void> {
    if (!this.shouldStayConnected || !this.desiredVisitId || this.stateSubject.value.connecting) return;

    this.clearDisconnectedTimer();
    this.clearTrackMutedTimer();
    this.teardownTransport();
    this.patchState({
      connected: false,
      connecting: true,
      userSpeaking: false,
      assistantSpeaking: false,
      muted: this.requestedMuted,
    });

    let acquiredStream: MediaStream | null = null;
    let ownsAcquiredStream = false;
    try {
      const provider = this.desiredMediaStreamProvider;
      if (provider) {
        acquiredStream = provider();
        if (!this.usableAudioTrack(acquiredStream)) {
          throw new Error('AI_REALTIME_SHARED_MIC_UNAVAILABLE');
        }
      } else {
        acquiredStream = await navigator.mediaDevices.getUserMedia({
          audio: {
            channelCount: 1,
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true,
          },
        });
        ownsAcquiredStream = true;
      }

      if (!this.shouldStayConnected) {
        if (ownsAcquiredStream) acquiredStream?.getTracks().forEach(track => track.stop());
        return;
      }
      if (!this.usableAudioTrack(acquiredStream)) {
        if (ownsAcquiredStream) acquiredStream?.getTracks().forEach(track => track.stop());
        throw new Error('AI_REALTIME_MICROPHONE_TRACK_MISSING');
      }

      const stream = acquiredStream as MediaStream;
      const microphoneTrack = stream.getAudioTracks()[0];
      this.mediaStream = stream;
      this.mediaStreamOwned = ownsAcquiredStream;

      const pc = new RTCPeerConnection();
      this.peerConnection = pc;
      this.watchMicrophoneTrack(microphoneTrack);
      const sender = pc.addTrack(microphoneTrack, stream);
      this.audioSender = sender;
      if (this.requestedMuted) await sender.replaceTrack(null);

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
      channel.onerror = () => {
        if (this.tearingDown) return;
        this.errorSubject.next(this.i18n.t(
          'consultation.ai.realtimeChannelError',
          'La liaison audio temps réel a rencontré une erreur. Reconnexion automatique…',
        ));
        this.handleConnectionClosed();
      };
      channel.onclose = () => {
        if (!this.tearingDown) this.handleConnectionClosed();
      };

      pc.onconnectionstatechange = () => this.handlePeerConnectionState(pc);
      pc.oniceconnectionstatechange = () => this.handleIceConnectionState(pc);

      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      await this.waitForIceGathering(pc);
      const localSdp = pc.localDescription?.sdp;
      if (!localSdp) throw new Error('AI_REALTIME_SDP_MISSING');

      const locale = this.i18n.currentLanguage();
      const answerSdp = await firstValueFrom(this.http.post(
        this.callEndpoint(this.desiredVisitId, this.desiredPurpose, locale),
        localSdp,
        {
          headers: new HttpHeaders({
            'Content-Type': 'application/sdp',
            Accept: 'application/sdp',
          }),
          responseType: 'text',
        },
      ));
      if (!this.shouldStayConnected) return;
      await pc.setRemoteDescription({ type: 'answer', sdp: answerSdp });
      await this.waitForDataChannel(channel, pc);
      if (!this.shouldStayConnected) return;

      this.reconnectAttempts = 0;
      this.patchState({
        connected: true,
        connecting: false,
        muted: this.requestedMuted,
      });
      await this.syncOutboundTrack();
    } catch (error) {
      if (acquiredStream && ownsAcquiredStream && acquiredStream !== this.mediaStream) {
        acquiredStream.getTracks().forEach(track => track.stop());
      }
      this.teardownTransport();
      this.patchState({
        connected: false,
        connecting: false,
        userSpeaking: false,
        assistantSpeaking: false,
        muted: this.requestedMuted,
      });
      throw error;
    }
  }

  private async syncOutboundTrack(): Promise<void> {
    const sender = this.audioSender;
    if (!sender) return;
    const generation = ++this.senderMuteGeneration;
    const microphoneTrack = this.mediaStream?.getAudioTracks()[0] ?? null;
    const targetTrack = this.requestedMuted ? null : microphoneTrack;
    if (!this.requestedMuted && (!microphoneTrack || microphoneTrack.readyState === 'ended')) {
      if (generation === this.senderMuteGeneration && this.shouldStayConnected) {
        this.handleConnectionClosed();
      }
      return;
    }
    if (sender.track === targetTrack) return;
    try {
      await sender.replaceTrack(targetTrack);
    } catch {
      if (generation !== this.senderMuteGeneration || sender !== this.audioSender) return;
      this.errorSubject.next(this.i18n.t(
        'consultation.ai.realtimeSenderError',
        'Le canal micro temps réel n’a pas pu être sécurisé. Reconnexion automatique…',
      ));
      if (this.shouldStayConnected) this.handleConnectionClosed();
    }
  }

  private usableAudioTrack(stream: MediaStream | null): MediaStreamTrack | null {
    const track = stream?.getAudioTracks()[0] ?? null;
    return track && track.readyState !== 'ended' ? track : null;
  }

  private watchMicrophoneTrack(track: MediaStreamTrack): void {
    if (typeof track.addEventListener !== 'function') return;
    track.addEventListener('ended', () => {
      if (track !== this.mediaStream?.getAudioTracks()[0]) return;
      if (!this.tearingDown && this.shouldStayConnected) {
        this.errorSubject.next(this.i18n.t(
          'consultation.ai.realtimeMicLost',
          'Le microphone a cessé de fournir de l’audio. Reconnexion automatique…',
        ));
        this.handleConnectionClosed();
      }
    });
    track.addEventListener('mute', () => {
      if (track !== this.mediaStream?.getAudioTracks()[0]) return;
      if (this.tearingDown || !this.shouldStayConnected) return;
      this.clearTrackMutedTimer();
      this.trackMutedTimer = setTimeout(() => {
        this.trackMutedTimer = null;
        if (track === this.mediaStream?.getAudioTracks()[0] && track.muted && this.shouldStayConnected) {
          this.errorSubject.next(this.i18n.t(
            'consultation.ai.realtimeMicLost',
            'Le microphone ne fournit plus d’audio. Reconnexion automatique…',
          ));
          this.handleConnectionClosed();
        }
      }, 4000);
    });
    track.addEventListener('unmute', () => {
      if (track === this.mediaStream?.getAudioTracks()[0]) this.clearTrackMutedTimer();
    });
  }

  private handlePeerConnectionState(pc: RTCPeerConnection): void {
    if (this.tearingDown || pc !== this.peerConnection) return;
    if (pc.connectionState === 'connected') {
      this.clearDisconnectedTimer();
      return;
    }
    if (pc.connectionState === 'disconnected') {
      this.scheduleDisconnectedRecovery();
      return;
    }
    if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
      this.errorSubject.next(this.i18n.t(
        'consultation.ai.realtimeNetworkError',
        'La connexion audio temps réel a été interrompue. Reconnexion automatique…',
      ));
      this.handleConnectionClosed();
    }
  }

  private handleIceConnectionState(pc: RTCPeerConnection): void {
    if (this.tearingDown || pc !== this.peerConnection) return;
    if (pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed') {
      this.clearDisconnectedTimer();
      return;
    }
    if (pc.iceConnectionState === 'disconnected') {
      this.scheduleDisconnectedRecovery();
      return;
    }
    if (pc.iceConnectionState === 'failed' || pc.iceConnectionState === 'closed') {
      this.handleConnectionClosed();
    }
  }

  private scheduleDisconnectedRecovery(): void {
    if (this.disconnectedTimer || !this.shouldStayConnected) return;
    this.disconnectedTimer = setTimeout(() => {
      this.disconnectedTimer = null;
      if (!this.shouldStayConnected) return;
      const pc = this.peerConnection;
      const unhealthy = !pc
        || pc.connectionState === 'disconnected'
        || pc.connectionState === 'failed'
        || pc.iceConnectionState === 'disconnected'
        || pc.iceConnectionState === 'failed';
      if (unhealthy) this.handleConnectionClosed();
    }, 3500);
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
        if (transcript) {
          this.transcriptSubject.next({
            transcript,
            confidence: this.transcriptionConfidence(event.logprobs),
            eventId: event.event_id,
            itemId: event.item_id,
          });
        }
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

  private transcriptionConfidence(
    logprobs: RealtimeServerEvent['logprobs'],
  ): number | null {
    const probabilities = (logprobs ?? [])
      .map(entry => entry.logprob)
      .filter((value): value is number => typeof value === 'number' && Number.isFinite(value))
      .map(value => Math.exp(Math.max(-20, value)))
      .sort((left, right) => left - right);
    if (probabilities.length === 0) return null;

    // Use the lower quintile rather than the mean so a few uncertain clinical
    // tokens (drug name, dose, number, negation) cannot be hidden by easy words.
    const index = Math.floor((probabilities.length - 1) * 0.2);
    return probabilities[index];
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
    if (this.tearingDown) return;
    this.activeResponseId = null;
    this.clearDisconnectedTimer();
    this.clearTrackMutedTimer();
    this.teardownTransport();
    this.patchState({
      connected: false,
      connecting: false,
      userSpeaking: false,
      assistantSpeaking: false,
      muted: this.requestedMuted,
    });
    if (this.shouldStayConnected) this.scheduleReconnect();
  }

  private scheduleReconnect(): void {
    if (!this.shouldStayConnected || !this.desiredVisitId || this.reconnectTimer) return;
    const delays = [1000, 2000, 5000, 10000, 15000, 30000];
    const delay = delays[Math.min(this.reconnectAttempts, delays.length - 1)];
    this.reconnectAttempts += 1;
    this.patchState({ connecting: true });
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.patchState({ connecting: false });
      if (!this.shouldStayConnected) return;
      void this.establishConnection().catch(error => {
        if (!this.shouldStayConnected) return;
        if (this.reconnectAttempts <= 2 || this.reconnectAttempts % 4 === 0) {
          this.errorSubject.next(this.describeConnectionError(error));
        }
        this.scheduleReconnect();
      });
    }, delay);
  }

  private teardownTransport(): void {
    this.tearingDown = true;
    try {
      this.activeResponseId = null;
      this.senderMuteGeneration += 1;
      this.audioSender = null;
      if (this.dataChannel) {
        this.dataChannel.onclose = null;
        this.dataChannel.onerror = null;
        this.dataChannel.onmessage = null;
        this.dataChannel.close();
      }
      this.dataChannel = null;
      if (this.peerConnection) {
        this.peerConnection.onconnectionstatechange = null;
        this.peerConnection.oniceconnectionstatechange = null;
        this.peerConnection.ontrack = null;
        this.peerConnection.close();
      }
      this.peerConnection = null;
      if (this.mediaStreamOwned) {
        this.mediaStream?.getTracks().forEach(track => track.stop());
      }
      this.mediaStream = null;
      this.mediaStreamOwned = false;
      if (this.remoteAudio) {
        this.remoteAudio.pause();
        this.remoteAudio.srcObject = null;
      }
      this.remoteAudio = null;
    } finally {
      this.tearingDown = false;
    }
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
  }

  private clearDisconnectedTimer(): void {
    if (this.disconnectedTimer !== null) clearTimeout(this.disconnectedTimer);
    this.disconnectedTimer = null;
  }

  private clearTrackMutedTimer(): void {
    if (this.trackMutedTimer !== null) clearTimeout(this.trackMutedTimer);
    this.trackMutedTimer = null;
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
          'La connexion réseau au temps réel est impossible. Reconnexion automatique en cours.',
        );
      }
    }
    if (error instanceof Error && error.message === 'AI_REALTIME_SHARED_MIC_UNAVAILABLE') {
      return this.i18n.t(
        'consultation.ai.realtimeSharedMicUnavailable',
        'La capture de sécurité n’a pas de flux micro actif. Le temps réel reste désactivé pour éviter une capture non protégée.',
      );
    }
    if (error instanceof Error && error.message.startsWith('AI_REALTIME_')) {
      return this.i18n.t(
        'consultation.ai.realtimeConnectionFailed',
        'La liaison WebRTC n’a pas pu être établie. Reconnexion automatique en cours.',
      );
    }
    return this.i18n.t(
      'consultation.ai.realtimeUnavailable',
      'Le temps réel est momentanément indisponible. Reconnexion automatique en cours.',
    );
  }

  private extractBackendReason(error: HttpErrorResponse): string {
    if (typeof error.error === 'string') return error.error;
    if (error.error && typeof error.error === 'object') {
      const body = error.error as Record<string, unknown>;
      const value = body['detail'] ?? body['title'] ?? body['message'];
      return typeof value === 'string' ? value : '';
    }
    return '';
  }
}
