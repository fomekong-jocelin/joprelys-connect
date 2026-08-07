import 'dart:collection';
import 'dart:typed_data';

/// Tampon borné conservant les derniers paquets PCM pendant une courte coupure
/// réseau. Les paquets les plus anciens sont évincés en premier.
class BoundedAudioReplayBuffer {
  BoundedAudioReplayBuffer({required this.maxBytes})
    : assert(maxBytes > 0, 'maxBytes must be positive');

  final int maxBytes;
  final ListQueue<Uint8List> _chunks = ListQueue<Uint8List>();
  int _bytes = 0;

  int get lengthInBytes => _bytes;
  bool get isEmpty => _chunks.isEmpty;

  void add(Uint8List chunk) {
    if (chunk.isEmpty) return;
    final copy = Uint8List.fromList(chunk);
    _chunks.addLast(copy);
    _bytes += copy.length;

    while (_bytes > maxBytes && _chunks.isNotEmpty) {
      _bytes -= _chunks.removeFirst().length;
    }
  }

  List<Uint8List> drain() {
    final result = List<Uint8List>.unmodifiable(_chunks);
    clear();
    return result;
  }

  void clear() {
    _chunks.clear();
    _bytes = 0;
  }
}

class CloudSpeechHealthPolicy {
  const CloudSpeechHealthPolicy._();

  static const Duration audioStallTimeout = Duration(seconds: 3);
  static const Duration heartbeatInterval = Duration(seconds: 10);
  static const Duration serverStallTimeout = Duration(seconds: 32);
  static const Duration stableConnectionDelay = Duration(seconds: 45);

  static bool audioIsStalled({
    required DateTime now,
    required DateTime? lastAudioChunkAt,
  }) {
    if (lastAudioChunkAt == null) return false;
    return now.difference(lastAudioChunkAt) > audioStallTimeout;
  }

  static bool serverIsStalled({
    required DateTime now,
    required DateTime? lastServerMessageAt,
  }) {
    if (lastServerMessageAt == null) return false;
    return now.difference(lastServerMessageAt) > serverStallTimeout;
  }

  static bool heartbeatIsDue({
    required DateTime now,
    required DateTime? lastHeartbeatAt,
  }) {
    if (lastHeartbeatAt == null) return true;
    return now.difference(lastHeartbeatAt) >= heartbeatInterval;
  }
}
