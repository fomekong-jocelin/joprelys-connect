import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/cloud_speech_streaming_health.dart';

void main() {
  group('bounded audio replay buffer', () {
    test('keeps recent chunks and evicts the oldest first', () {
      final buffer = BoundedAudioReplayBuffer(maxBytes: 6);

      buffer.add(Uint8List.fromList([1, 2, 3]));
      buffer.add(Uint8List.fromList([4, 5, 6]));
      buffer.add(Uint8List.fromList([7, 8]));

      expect(buffer.lengthInBytes, 5);
      expect(
        buffer.drain().map((chunk) => chunk.toList()).toList(),
        [
          [4, 5, 6],
          [7, 8],
        ],
      );
      expect(buffer.isEmpty, isTrue);
    });
  });

  group('cloud speech health policy', () {
    final now = DateTime.utc(2026, 8, 7, 20);

    test('detects a dead microphone stream independently from silence', () {
      expect(
        CloudSpeechHealthPolicy.audioIsStalled(
          now: now,
          lastAudioChunkAt: now.subtract(const Duration(seconds: 4)),
        ),
        isTrue,
      );
      expect(
        CloudSpeechHealthPolicy.audioIsStalled(
          now: now,
          lastAudioChunkAt: now.subtract(const Duration(seconds: 1)),
        ),
        isFalse,
      );
    });

    test('detects a half-open websocket and schedules heartbeats', () {
      expect(
        CloudSpeechHealthPolicy.serverIsStalled(
          now: now,
          lastServerMessageAt: now.subtract(const Duration(seconds: 40)),
        ),
        isTrue,
      );
      expect(
        CloudSpeechHealthPolicy.heartbeatIsDue(
          now: now,
          lastHeartbeatAt: now.subtract(const Duration(seconds: 11)),
        ),
        isTrue,
      );
    });
  });
}
