import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { AiConsultationApiService } from './ai-consultation-api.service';

/**
 * Single, backend-approved speech channel for conversational Realtime.
 *
 * Dictation never calls this service. It deliberately owns no microphone
 * control: playback must not mute or disconnect clinical capture.
 */
@Injectable()
export class ClinicalVoicePlaybackService implements OnDestroy {
  private readonly api = inject(AiConsultationApiService);

  readonly speaking = signal(false);

  private request: Subscription | null = null;
  private audio: HTMLAudioElement | null = null;
  private audioUrl: string | null = null;
  private lastMessage = '';
  private generation = 0;

  play(message: string): void {
    const text = message.trim();
    if (!text || text === this.lastMessage) return;

    this.stopAudio();
    this.request?.unsubscribe();
    this.request = null;
    this.lastMessage = text;
    const generation = ++this.generation;
    const synthesize = (
      this.api as Partial<Pick<AiConsultationApiService, 'synthesizeSpeech'>>
    ).synthesizeSpeech;
    if (!synthesize) return;

    this.request = synthesize.call(this.api, text).subscribe({
      next: blob => {
        if (generation !== this.generation) return;
        const url = URL.createObjectURL(blob);
        const audio = new Audio(url);
        this.audio = audio;
        this.audioUrl = url;
        this.speaking.set(true);
        audio.onended = () => this.finishPlayback(generation);
        audio.onerror = () => this.finishPlayback(generation);
        void audio.play().catch(() => this.finishPlayback(generation));
      },
      error: () => {
        if (generation === this.generation) this.speaking.set(false);
      },
    });
  }

  stop(): void {
    this.generation += 1;
    this.request?.unsubscribe();
    this.request = null;
    this.stopAudio();
    this.speaking.set(false);
  }

  ngOnDestroy(): void {
    this.stop();
  }

  private finishPlayback(generation: number): void {
    if (generation !== this.generation) return;
    this.stopAudio();
    this.speaking.set(false);
  }

  private stopAudio(): void {
    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
      this.audio = null;
    }
    if (this.audioUrl) {
      URL.revokeObjectURL(this.audioUrl);
      this.audioUrl = null;
    }
  }
}
