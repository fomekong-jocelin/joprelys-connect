import { AmbientAudioVaultService } from './ambient-audio-vault.service';

describe('AmbientAudioVaultService doctor calibration binding', () => {
  it('should never attach a new calibration to a chunk that started before activation', async () => {
    const service = preparedVault(1_000_000);
    const reference = new Uint8Array([9, 8, 7]).buffer;
    service.activateDoctorReference('visit-1', reference, 1_005_000);

    const before = await service.storeEncryptedChunk(
      'visit-1',
      4_000,
      10_000,
      'audio/wav',
      'fr',
      new Uint8Array([1]).buffer,
    );
    const after = await service.storeEncryptedChunk(
      'visit-1',
      6_000,
      10_000,
      'audio/wav',
      'fr',
      new Uint8Array([2]).buffer,
    );

    expect(before.doctorReferenceCiphertext).toBeNull();
    expect(before.doctorReferenceIv).toBeNull();
    expect(after.doctorReferenceCiphertext).not.toBeNull();
    expect(after.doctorReferenceIv).not.toBeNull();
    expect(after.doctorReferenceMimeType).toBe('audio/wav');
  });

  it('should keep calibration scoped to its visit', async () => {
    const service = preparedVault(2_000_000);
    service.activateDoctorReference('visit-a', new Uint8Array([4, 5, 6]).buffer, 2_000_000);

    const otherVisit = await service.storeEncryptedChunk(
      'visit-b',
      1_000,
      10_000,
      'audio/wav',
      'fr',
      new Uint8Array([1]).buffer,
    );

    expect(otherVisit.doctorReferenceCiphertext).toBeNull();
  });

  it('should allow an explicit null reference to prevent retroactive calibration', async () => {
    const service = preparedVault(3_000_000);
    service.activateDoctorReference('visit-1', new Uint8Array([4, 5, 6]).buffer, 3_000_000);

    const record = await service.storeEncryptedChunk(
      'visit-1',
      2_000,
      10_000,
      'audio/wav',
      'fr',
      new Uint8Array([1]).buffer,
      null,
    );

    expect(record.doctorReferenceCiphertext).toBeNull();
  });

  it('should copy the active reference rather than retain the caller buffer', async () => {
    const service = preparedVault(4_000_000);
    const referenceBytes = new Uint8Array([1, 2, 3]);
    service.activateDoctorReference('visit-1', referenceBytes.buffer, 4_000_000);
    referenceBytes[0] = 99;

    await service.storeEncryptedChunk(
      'visit-1',
      1_000,
      10_000,
      'audio/wav',
      'fr',
      new Uint8Array([7]).buffer,
    );

    const encryptCalls = (service as any).encrypt.mock.calls as Array<[unknown, ArrayBuffer, ArrayBuffer]>;
    const encryptedReferenceInput = new Uint8Array(encryptCalls[1][1]);
    expect(Array.from(encryptedReferenceInput)).toEqual([1, 2, 3]);
  });

  function preparedVault(originEpochMs: number): AmbientAudioVaultService {
    const service = new AmbientAudioVaultService();
    let sequence = 0;
    service.isSupported = vi.fn(() => true);
    (service as any).reserveSequence = vi.fn(async (visitId: string) => ({
      visitId,
      sessionId: `session-${visitId}`,
      originEpochMs,
      nextSequence: ++sequence,
      updatedAt: originEpochMs,
      sequence,
    }));
    (service as any).cryptoKey = vi.fn(async () => ({}));
    (service as any).encrypt = vi.fn(async (_key: unknown, plaintext: ArrayBuffer) => ({
      ciphertext: new Uint8Array(plaintext.byteLength || 1).fill(1).buffer,
      iv: new Uint8Array(12).fill(2).buffer,
    }));
    (service as any).putChunk = vi.fn(async () => undefined);
    return service;
  }
});
