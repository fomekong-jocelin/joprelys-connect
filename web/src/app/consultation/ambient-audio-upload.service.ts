import { DOCUMENT } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { AmbientAudioVaultService, AmbientStoredChunk } from './ambient-audio-vault.service';

export interface AmbientUploadState {
  uploading: boolean;
  online: boolean;
  pendingChunks: number;
  pendingBytes: number;
  storagePressure: boolean;
  lastError: string | null;
}

const RETRY_DELAYS_MS = [2_000, 5_000, 15_000, 30_000, 60_000] as const;

@Injectable({ providedIn: 'root' })
export class AmbientAudioUploadService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly vault = inject(AmbientAudioVaultService);
  private readonly document = inject(DOCUMENT);
  private readonly stateSubject = new BehaviorSubject<AmbientUploadState>({
    uploading: false,
    online: this.navigatorOnline(),
    pendingChunks: 0,
    pendingBytes: 0,
    storagePressure: false,
    lastError: null,
  });

  readonly state$ = this.stateSubject.asObservable();

  private flushPromise: Promise<void> | null = null;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private preferredVisitId: string | null = null;

  constructor() {
    const windowRef = this.document.defaultView;
    windowRef?.addEventListener('online', this.onOnline);
    windowRef?.addEventListener('offline', this.onOffline);
    void this.refreshState();
    if (this.navigatorOnline()) {
      queueMicrotask(() => void this.flush());
    }
  }

  async flush(visitId?: string): Promise<void> {
    if (visitId?.trim()) this.preferredVisitId = visitId.trim();
    if (this.flushPromise) return this.flushPromise;
    this.clearRetry();
    this.flushPromise = this.flushLoop(this.preferredVisitId ?? undefined)
      .finally(() => {
        this.flushPromise = null;
        void this.refreshState();
      });
    return this.flushPromise;
  }

  async refreshState(visitId?: string): Promise<void> {
    try {
      const stats = await this.vault.stats(visitId);
      const pressure = await this.storagePressure();
      this.patchState({
        online: this.navigatorOnline(),
        pendingChunks: stats.pendingChunks,
        pendingBytes: stats.encryptedBytes,
        storagePressure: pressure,
      });
    } catch (error) {
      this.patchState({ lastError: this.describeError(error) });
    }
  }

  ngOnDestroy(): void {
    this.clearRetry();
    const windowRef = this.document.defaultView;
    windowRef?.removeEventListener('online', this.onOnline);
    windowRef?.removeEventListener('offline', this.onOffline);
    this.stateSubject.complete();
  }

  private async flushLoop(visitId?: string): Promise<void> {
    if (!this.navigatorOnline()) {
      this.patchState({ online: false, uploading: false });
      return;
    }

    this.patchState({ online: true, uploading: true, lastError: null });
    try {
      while (this.navigatorOnline()) {
        const pending = await this.vault.listPending(visitId);
        if (!pending.length) break;

        const record = pending[0];
        try {
          await this.upload(record);
          await this.vault.acknowledge(record.id);
          await this.refreshState(visitId);
        } catch (error) {
          const reason = this.describeError(error);
          await this.vault.markAttempt(record.id, reason);
          this.patchState({ lastError: reason });
          if (this.isTransient(error)) {
            const nextAttempt = record.attempts + 1;
            this.scheduleRetry(this.retryDelay(nextAttempt), visitId);
          }
          break;
        }
      }
    } finally {
      this.patchState({ uploading: false, online: this.navigatorOnline() });
    }
  }

  private async upload(record: AmbientStoredChunk): Promise<void> {
    const plaintext = await this.vault.decrypt(record);
    const audioBlob = new Blob([plaintext], { type: record.mimeType || 'audio/wav' });
    const doctorReference = await this.vault.decryptDoctorReference(record);
    const commonHeaders = new HttpHeaders({
      'X-Joprelys-Ambient-Chunk-Id': record.id,
      'X-Joprelys-Ambient-Start-Ms': String(record.startOffsetMs),
      'X-Joprelys-Locale': record.locale || 'fr',
    });

    if (doctorReference) {
      const formData = new FormData();
      formData.append('audio', audioBlob, 'ambient.wav');
      formData.append(
        'doctorReference',
        new Blob([doctorReference], { type: 'audio/wav' }),
        'doctor-reference.wav',
      );
      await firstValueFrom(this.http.post<unknown>(
        `/api/ai/consultations/${record.visitId}/ambient/transcriptions/audio-with-doctor-reference`,
        formData,
        { headers: commonHeaders },
      ));
      return;
    }

    await firstValueFrom(this.http.post<unknown>(
      `/api/ai/consultations/${record.visitId}/ambient/transcriptions/audio`,
      audioBlob,
      {
        headers: commonHeaders.set('Content-Type', record.mimeType || 'audio/wav'),
      },
    ));
  }

  private isTransient(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse)) {
      const message = error instanceof Error ? error.message : '';
      return !message.startsWith('AMBIENT_DOCTOR_REFERENCE_');
    }
    if (error.status === 0 || error.status === 408 || error.status === 425 || error.status === 429) {
      return true;
    }
    if (error.status >= 500) return true;
    if (error.status === 409) {
      const reason = this.backendReason(error);
      return reason === 'AI_AMBIENT_CHUNK_PROCESSING'
        || reason === 'AI_AMBIENT_CHUNK_LEASE_LOST';
    }
    return false;
  }

  private retryDelay(attempt: number): number {
    const index = Math.min(Math.max(attempt - 1, 0), RETRY_DELAYS_MS.length - 1);
    return RETRY_DELAYS_MS[index];
  }

  private scheduleRetry(delayMs: number, visitId?: string): void {
    this.clearRetry();
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      if (this.navigatorOnline()) void this.flush(visitId);
    }, delayMs);
  }

  private clearRetry(): void {
    if (!this.retryTimer) return;
    clearTimeout(this.retryTimer);
    this.retryTimer = null;
  }

  private async storagePressure(): Promise<boolean> {
    try {
      const estimate = await navigator.storage?.estimate?.();
      const quota = estimate?.quota ?? 0;
      const usage = estimate?.usage ?? 0;
      return quota > 0 && usage / quota >= 0.85;
    } catch {
      return false;
    }
  }

  private navigatorOnline(): boolean {
    return typeof navigator === 'undefined' || navigator.onLine !== false;
  }

  private backendReason(error: HttpErrorResponse): string {
    const payload = error.error;
    if (payload && typeof payload === 'object') {
      const detail = (payload as { detail?: unknown; title?: unknown }).detail
        ?? (payload as { detail?: unknown; title?: unknown }).title;
      return typeof detail === 'string' ? detail : '';
    }
    return typeof payload === 'string' ? payload : '';
  }

  private describeError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      return this.backendReason(error) || `HTTP_${error.status || 0}`;
    }
    return error instanceof Error && error.message ? error.message : 'AMBIENT_UPLOAD_FAILED';
  }

  private patchState(patch: Partial<AmbientUploadState>): void {
    this.stateSubject.next({ ...this.stateSubject.value, ...patch });
  }

  private readonly onOnline = (): void => {
    this.patchState({ online: true });
    void this.flush();
  };

  private readonly onOffline = (): void => {
    this.patchState({ online: false, uploading: false });
  };
}
