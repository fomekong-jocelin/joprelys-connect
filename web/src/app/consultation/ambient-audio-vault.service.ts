import { Injectable } from '@angular/core';

const DB_NAME = 'joprelys-clinical-safety';
const DB_VERSION = 1;
const CHUNK_STORE = 'ambient_chunks';
const KEY_STORE = 'crypto_keys';
const SESSION_STORE = 'ambient_sessions';
const KEY_ID = 'ambient-aes-gcm-v1';

export interface AmbientStoredChunk {
  id: string;
  visitId: string;
  sessionId: string;
  sequence: number;
  startOffsetMs: number;
  durationMs: number;
  mimeType: string;
  ciphertext: ArrayBuffer;
  iv: ArrayBuffer;
  createdAt: number;
  attempts: number;
  lastAttemptAt: number | null;
  lastError: string | null;
}

export interface AmbientCaptureTimeline {
  visitId: string;
  sessionId: string;
  originEpochMs: number;
  nextSequence: number;
  updatedAt: number;
}

export interface AmbientVaultStats {
  pendingChunks: number;
  encryptedBytes: number;
}

@Injectable({ providedIn: 'root' })
export class AmbientAudioVaultService {
  private dbPromise: Promise<IDBDatabase> | null = null;

  isSupported(): boolean {
    return typeof indexedDB !== 'undefined'
      && typeof crypto !== 'undefined'
      && !!crypto.subtle;
  }

  async requestPersistentStorage(): Promise<boolean> {
    try {
      return await navigator.storage?.persist?.() ?? false;
    } catch {
      return false;
    }
  }

  async getOrCreateTimeline(visitId: string): Promise<AmbientCaptureTimeline> {
    const normalizedVisitId = visitId.trim();
    if (!normalizedVisitId) throw new Error('AMBIENT_VISIT_REQUIRED');
    const db = await this.db();
    return new Promise<AmbientCaptureTimeline>((resolve, reject) => {
      const tx = db.transaction(SESSION_STORE, 'readwrite');
      const store = tx.objectStore(SESSION_STORE);
      const getRequest = store.get(normalizedVisitId);
      getRequest.onerror = () => reject(getRequest.error ?? new Error('AMBIENT_SESSION_READ_FAILED'));
      getRequest.onsuccess = () => {
        const existing = getRequest.result as AmbientCaptureTimeline | undefined;
        if (existing) {
          resolve(existing);
          return;
        }
        const created: AmbientCaptureTimeline = {
          visitId: normalizedVisitId,
          sessionId: this.uuid(),
          originEpochMs: Date.now(),
          nextSequence: 0,
          updatedAt: Date.now(),
        };
        const putRequest = store.put(created);
        putRequest.onerror = () => reject(putRequest.error ?? new Error('AMBIENT_SESSION_WRITE_FAILED'));
        putRequest.onsuccess = () => resolve(created);
      };
    });
  }

  async storeEncryptedChunk(
    visitId: string,
    startOffsetMs: number,
    durationMs: number,
    mimeType: string,
    plaintext: ArrayBuffer,
  ): Promise<AmbientStoredChunk> {
    if (!this.isSupported()) throw new Error('AMBIENT_VAULT_UNSUPPORTED');
    if (!Number.isFinite(startOffsetMs) || startOffsetMs < 0) throw new Error('AMBIENT_OFFSET_INVALID');
    if (!Number.isFinite(durationMs) || durationMs <= 0) throw new Error('AMBIENT_DURATION_INVALID');
    if (!plaintext.byteLength) throw new Error('AMBIENT_AUDIO_EMPTY');

    const reservation = await this.reserveSequence(visitId);
    const id = `${reservation.sessionId}:${reservation.sequence.toString().padStart(8, '0')}`;
    const key = await this.cryptoKey();
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const additionalData = this.additionalData(visitId, id);
    const encrypted = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv, additionalData },
      key,
      plaintext,
    );
    const record: AmbientStoredChunk = {
      id,
      visitId,
      sessionId: reservation.sessionId,
      sequence: reservation.sequence,
      startOffsetMs: Math.round(startOffsetMs),
      durationMs: Math.round(durationMs),
      mimeType: mimeType || 'audio/wav',
      ciphertext: encrypted,
      iv: iv.buffer.slice(iv.byteOffset, iv.byteOffset + iv.byteLength),
      createdAt: Date.now(),
      attempts: 0,
      lastAttemptAt: null,
      lastError: null,
    };
    await this.putChunk(record);
    return record;
  }

  async listPending(visitId?: string): Promise<AmbientStoredChunk[]> {
    const db = await this.db();
    const records = await new Promise<AmbientStoredChunk[]>((resolve, reject) => {
      const tx = db.transaction(CHUNK_STORE, 'readonly');
      const store = tx.objectStore(CHUNK_STORE);
      const request = visitId
        ? store.index('visitId').getAll(visitId)
        : store.getAll();
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_CHUNK_LIST_FAILED'));
      request.onsuccess = () => resolve((request.result ?? []) as AmbientStoredChunk[]);
    });
    return records.sort((a, b) => a.startOffsetMs - b.startOffsetMs || a.sequence - b.sequence);
  }

  async decrypt(record: AmbientStoredChunk): Promise<ArrayBuffer> {
    const key = await this.cryptoKey();
    const iv = new Uint8Array(record.iv);
    return crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv,
        additionalData: this.additionalData(record.visitId, record.id),
      },
      key,
      record.ciphertext,
    );
  }

  async markAttempt(id: string, error: string | null): Promise<void> {
    const db = await this.db();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(CHUNK_STORE, 'readwrite');
      const store = tx.objectStore(CHUNK_STORE);
      const getRequest = store.get(id);
      getRequest.onerror = () => reject(getRequest.error ?? new Error('AMBIENT_CHUNK_READ_FAILED'));
      getRequest.onsuccess = () => {
        const record = getRequest.result as AmbientStoredChunk | undefined;
        if (!record) {
          resolve();
          return;
        }
        record.attempts += 1;
        record.lastAttemptAt = Date.now();
        record.lastError = error;
        const putRequest = store.put(record);
        putRequest.onerror = () => reject(putRequest.error ?? new Error('AMBIENT_CHUNK_WRITE_FAILED'));
        putRequest.onsuccess = () => resolve();
      };
    });
  }

  async acknowledge(id: string): Promise<void> {
    const db = await this.db();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(CHUNK_STORE, 'readwrite');
      const request = tx.objectStore(CHUNK_STORE).delete(id);
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_CHUNK_DELETE_FAILED'));
      request.onsuccess = () => resolve();
    });
  }

  async stats(visitId?: string): Promise<AmbientVaultStats> {
    const records = await this.listPending(visitId);
    return {
      pendingChunks: records.length,
      encryptedBytes: records.reduce((sum, item) => sum + item.ciphertext.byteLength, 0),
    };
  }

  private async reserveSequence(visitId: string): Promise<AmbientCaptureTimeline & { sequence: number }> {
    const normalizedVisitId = visitId.trim();
    const db = await this.db();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(SESSION_STORE, 'readwrite');
      const store = tx.objectStore(SESSION_STORE);
      const request = store.get(normalizedVisitId);
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_SESSION_READ_FAILED'));
      request.onsuccess = () => {
        const current = request.result as AmbientCaptureTimeline | undefined;
        const timeline: AmbientCaptureTimeline = current ?? {
          visitId: normalizedVisitId,
          sessionId: this.uuid(),
          originEpochMs: Date.now(),
          nextSequence: 0,
          updatedAt: Date.now(),
        };
        const sequence = timeline.nextSequence + 1;
        const updated: AmbientCaptureTimeline = {
          ...timeline,
          nextSequence: sequence,
          updatedAt: Date.now(),
        };
        const putRequest = store.put(updated);
        putRequest.onerror = () => reject(putRequest.error ?? new Error('AMBIENT_SESSION_WRITE_FAILED'));
        putRequest.onsuccess = () => resolve({ ...updated, sequence });
      };
    });
  }

  private async putChunk(record: AmbientStoredChunk): Promise<void> {
    const db = await this.db();
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(CHUNK_STORE, 'readwrite');
      const request = tx.objectStore(CHUNK_STORE).put(record);
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_CHUNK_WRITE_FAILED'));
      request.onsuccess = () => resolve();
    });
  }

  private async cryptoKey(): Promise<CryptoKey> {
    const db = await this.db();
    const existing = await new Promise<CryptoKey | null>((resolve, reject) => {
      const tx = db.transaction(KEY_STORE, 'readonly');
      const request = tx.objectStore(KEY_STORE).get(KEY_ID);
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_KEY_READ_FAILED'));
      request.onsuccess = () => {
        const value = request.result as { id: string; key: CryptoKey } | undefined;
        resolve(value?.key ?? null);
      };
    });
    if (existing) return existing;

    const generated = await crypto.subtle.generateKey(
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt'],
    );
    await new Promise<void>((resolve, reject) => {
      const tx = db.transaction(KEY_STORE, 'readwrite');
      const request = tx.objectStore(KEY_STORE).put({ id: KEY_ID, key: generated });
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_KEY_WRITE_FAILED'));
      request.onsuccess = () => resolve();
    });
    return generated;
  }

  private additionalData(visitId: string, chunkId: string): Uint8Array {
    return new TextEncoder().encode(`joprelys-ambient:${visitId}:${chunkId}`);
  }

  private db(): Promise<IDBDatabase> {
    if (this.dbPromise) return this.dbPromise;
    if (typeof indexedDB === 'undefined') {
      return Promise.reject(new Error('AMBIENT_INDEXEDDB_UNSUPPORTED'));
    }
    this.dbPromise = new Promise<IDBDatabase>((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);
      request.onerror = () => reject(request.error ?? new Error('AMBIENT_DB_OPEN_FAILED'));
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(CHUNK_STORE)) {
          const chunks = db.createObjectStore(CHUNK_STORE, { keyPath: 'id' });
          chunks.createIndex('visitId', 'visitId', { unique: false });
          chunks.createIndex('createdAt', 'createdAt', { unique: false });
        }
        if (!db.objectStoreNames.contains(KEY_STORE)) {
          db.createObjectStore(KEY_STORE, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(SESSION_STORE)) {
          db.createObjectStore(SESSION_STORE, { keyPath: 'visitId' });
        }
      };
      request.onsuccess = () => resolve(request.result);
    });
    return this.dbPromise;
  }

  private uuid(): string {
    return typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
  }
}
