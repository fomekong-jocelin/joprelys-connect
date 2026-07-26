import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AmbientAudioUploadService } from './ambient-audio-upload.service';
import { AmbientAudioVaultService, AmbientStoredChunk } from './ambient-audio-vault.service';

describe('AmbientAudioUploadService', () => {
  let service: AmbientAudioUploadService;
  let http: HttpTestingController;
  let pending: AmbientStoredChunk[];
  let vault: {
    stats: ReturnType<typeof vi.fn>;
    listPending: ReturnType<typeof vi.fn>;
    decrypt: ReturnType<typeof vi.fn>;
    acknowledge: ReturnType<typeof vi.fn>;
    markAttempt: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    pending = [];
    vault = {
      stats: vi.fn().mockImplementation(async () => ({
        pendingChunks: pending.length,
        encryptedBytes: pending.reduce((sum, item) => sum + item.ciphertext.byteLength, 0),
      })),
      listPending: vi.fn().mockImplementation(async () => [...pending]),
      decrypt: vi.fn().mockResolvedValue(new Uint8Array([82, 73, 70, 70]).buffer),
      acknowledge: vi.fn().mockImplementation(async (id: string) => {
        pending = pending.filter(item => item.id !== id);
      }),
      markAttempt: vi.fn().mockResolvedValue(undefined),
    };

    TestBed.configureTestingModule({
      providers: [
        AmbientAudioUploadService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AmbientAudioVaultService, useValue: vault },
      ],
    });
    service = TestBed.inject(AmbientAudioUploadService);
    http = TestBed.inject(HttpTestingController);
    await settle();
  });

  afterEach(() => {
    service.ngOnDestroy();
    http.verify();
    TestBed.resetTestingModule();
  });

  it('should keep encrypted chunk until a successful server acknowledgement', async () => {
    pending = [chunk('session:00000001')];

    const upload = service.flush('visit-1');
    await settle();

    const request = http.expectOne('/api/ai/consultations/visit-1/ambient/transcriptions/audio');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Joprelys-Ambient-Chunk-Id')).toBe('session:00000001');
    expect(request.request.headers.get('X-Joprelys-Ambient-Start-Ms')).toBe('12000');
    expect(request.request.headers.get('X-Joprelys-Locale')).toBe('fr');
    expect(vault.acknowledge).not.toHaveBeenCalled();

    request.flush([]);
    await upload;

    expect(vault.acknowledge).toHaveBeenCalledWith('session:00000001');
    expect(pending).toHaveLength(0);
  });

  it('should retain chunk and record attempt when server is unavailable', async () => {
    pending = [chunk('session:00000002')];

    const upload = service.flush('visit-1');
    await settle();
    http.expectOne('/api/ai/consultations/visit-1/ambient/transcriptions/audio')
      .flush({ detail: 'AI_AMBIENT_UPSTREAM_UNAVAILABLE' }, { status: 503, statusText: 'Unavailable' });
    await upload;

    expect(vault.markAttempt).toHaveBeenCalledWith(
      'session:00000002',
      'AI_AMBIENT_UPSTREAM_UNAVAILABLE',
    );
    expect(vault.acknowledge).not.toHaveBeenCalled();
    expect(pending).toHaveLength(1);
    expect((service as any).retryTimer).not.toBeNull();
  });

  it('should retain and schedule retry when a stale worker loses the server lease', async () => {
    pending = [chunk('session:00000004')];

    const upload = service.flush('visit-1');
    await settle();
    http.expectOne('/api/ai/consultations/visit-1/ambient/transcriptions/audio')
      .flush({ detail: 'AI_AMBIENT_CHUNK_LEASE_LOST' }, { status: 409, statusText: 'Conflict' });
    await upload;

    expect(vault.markAttempt).toHaveBeenCalledWith(
      'session:00000004',
      'AI_AMBIENT_CHUNK_LEASE_LOST',
    );
    expect(vault.acknowledge).not.toHaveBeenCalled();
    expect(pending).toHaveLength(1);
    expect((service as any).retryTimer).not.toBeNull();
  });

  it('should retain and schedule retry while another worker is processing the chunk', async () => {
    pending = [chunk('session:00000005')];

    const upload = service.flush('visit-1');
    await settle();
    http.expectOne('/api/ai/consultations/visit-1/ambient/transcriptions/audio')
      .flush({ detail: 'AI_AMBIENT_CHUNK_PROCESSING' }, { status: 409, statusText: 'Conflict' });
    await upload;

    expect(vault.markAttempt).toHaveBeenCalledWith(
      'session:00000005',
      'AI_AMBIENT_CHUNK_PROCESSING',
    );
    expect(vault.acknowledge).not.toHaveBeenCalled();
    expect(pending).toHaveLength(1);
    expect((service as any).retryTimer).not.toBeNull();
  });

  it('should not retry a hash mismatch as if it were a transient race', async () => {
    pending = [chunk('session:00000003')];

    const upload = service.flush('visit-1');
    await settle();
    http.expectOne('/api/ai/consultations/visit-1/ambient/transcriptions/audio')
      .flush({ detail: 'AI_AMBIENT_CHUNK_HASH_MISMATCH' }, { status: 409, statusText: 'Conflict' });
    await upload;

    expect(vault.markAttempt).toHaveBeenCalledWith(
      'session:00000003',
      'AI_AMBIENT_CHUNK_HASH_MISMATCH',
    );
    expect(vault.acknowledge).not.toHaveBeenCalled();
    expect(pending).toHaveLength(1);
    expect((service as any).retryTimer).toBeNull();
  });

  function chunk(id: string): AmbientStoredChunk {
    return {
      id,
      visitId: 'visit-1',
      sessionId: 'session',
      sequence: Number(id.slice(-1)),
      startOffsetMs: 12_000,
      durationMs: 10_000,
      mimeType: 'audio/wav',
      locale: 'fr',
      ciphertext: new Uint8Array([1, 2, 3]).buffer,
      iv: new Uint8Array(12).buffer,
      createdAt: Date.now(),
      attempts: 0,
      lastAttemptAt: null,
      lastError: null,
    };
  }
});

async function settle(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}
