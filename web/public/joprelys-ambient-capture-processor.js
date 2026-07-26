class JoprelysAmbientCaptureProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.frameSize = Math.max(2048, Math.round(sampleRate / 2));
    this.buffer = new Float32Array(this.frameSize);
    this.offset = 0;
    this.port.onmessage = event => {
      if (event.data?.type === 'flush') {
        this.flushPartial();
        this.port.postMessage({ type: 'flushed' });
      }
    };
  }

  process(inputs, outputs) {
    const input = inputs[0]?.[0];
    const output = outputs[0]?.[0];
    if (input && input.length) {
      let sourceOffset = 0;
      while (sourceOffset < input.length) {
        const writable = Math.min(this.buffer.length - this.offset, input.length - sourceOffset);
        this.buffer.set(input.subarray(sourceOffset, sourceOffset + writable), this.offset);
        this.offset += writable;
        sourceOffset += writable;
        if (this.offset === this.buffer.length) {
          this.emitFrame(this.buffer);
          this.buffer = new Float32Array(this.frameSize);
          this.offset = 0;
        }
      }
      if (output) {
        output.set(input.subarray(0, Math.min(output.length, input.length)));
      }
    } else if (output) {
      output.fill(0);
    }
    return true;
  }

  flushPartial() {
    if (this.offset <= 0) return;
    const partial = this.buffer.slice(0, this.offset);
    this.emitFrame(partial);
    this.buffer = new Float32Array(this.frameSize);
    this.offset = 0;
  }

  emitFrame(frame) {
    this.port.postMessage(
      { type: 'frame', sampleRate, samples: frame },
      [frame.buffer],
    );
  }
}

registerProcessor('joprelys-ambient-capture', JoprelysAmbientCaptureProcessor);
