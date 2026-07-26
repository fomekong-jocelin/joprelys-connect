import { describe, expect, it } from 'vitest';
import { downsamplePcm16, encodePcm16Wav } from './ambient-audio-capture.service';

describe('ambient audio safety encoding', () => {
  it('should downsample 48 kHz mono PCM to 16 kHz without changing duration', () => {
    const input = new Float32Array(48_000);
    input.fill(0.5);

    const output = downsamplePcm16(input, 48_000, 16_000);

    expect(output.length).toBe(16_000);
    expect(output[0]).toBeGreaterThan(16_000);
    expect(output[0]).toBeLessThan(17_000);
    expect(output.at(-1)).toBe(output[0]);
  });

  it('should clamp out-of-range samples before PCM16 conversion', () => {
    const output = downsamplePcm16(new Float32Array([-2, 2]), 16_000, 16_000);

    expect(output[0]).toBe(-32_768);
    expect(output[1]).toBe(32_767);
  });

  it('should create a standalone mono PCM16 WAV with correct duration metadata', () => {
    const samples = new Int16Array(16_000);
    const buffer = encodePcm16Wav(samples, 16_000);
    const view = new DataView(buffer);

    expect(ascii(view, 0, 4)).toBe('RIFF');
    expect(ascii(view, 8, 4)).toBe('WAVE');
    expect(ascii(view, 12, 4)).toBe('fmt ');
    expect(view.getUint16(20, true)).toBe(1);
    expect(view.getUint16(22, true)).toBe(1);
    expect(view.getUint32(24, true)).toBe(16_000);
    expect(view.getUint16(34, true)).toBe(16);
    expect(ascii(view, 36, 4)).toBe('data');
    expect(view.getUint32(40, true)).toBe(32_000);
    expect(buffer.byteLength).toBe(32_044);
  });
});

function ascii(view: DataView, offset: number, length: number): string {
  return Array.from({ length }, (_, index) => String.fromCharCode(view.getUint8(offset + index))).join('');
}
