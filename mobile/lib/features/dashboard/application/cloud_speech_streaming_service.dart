// ignore_for_file: prefer_initializing_formals

import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:record/record.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import '../data/clinical_transcript_draft_store.dart';
import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';
import 'clinical_speech_hypothesis.dart';
import 'clinical_voice_error_message.dart';
import 'clinical_voice_state.dart';
import 'cloud_speech_streaming_health.dart';

export 'clinical_voice_error_message.dart';
export 'clinical_voice_state.dart';

@visibleForTesting
Uri buildClinicalVoiceWebSocketUri({
  required Uri apiBaseUri,
  required String jwtToken,
}) {
  final scheme = switch (apiBaseUri.scheme.toLowerCase()) {
    'https' => 'wss',
    'http' => 'ws',
    'wss' => 'wss',
    'ws' => 'ws',
    _ => throw ArgumentError.value(
      apiBaseUri,
      'apiBaseUri',
      'Le schéma doit être HTTP, HTTPS, WS ou WSS.',
    ),
  };
  final basePath = apiBaseUri.path.replaceFirst(RegExp(r'/+$'), '');
  final path = basePath.endsWith('/api')
      ? '$basePath/voice/stream'
      : '$basePath/api/voice/stream';
  return apiBaseUri.replace(
    scheme: scheme,
    path: path.replaceAll(RegExp(r'/{2,}'), '/'),
    queryParameters: <String, String>{'token': jwtToken},
    fragment: '',
  );
}

/// Dictée clinique cloud via WebSocket.
///
/// La capture microphone et le transport réseau sont volontairement découplés.
/// Une courte rupture WebSocket ne stoppe donc plus le microphone : les paquets
/// récents sont conservés localement puis rejoués après reconnexion. Un watchdog
/// contrôle séparément la santé du microphone et celle du WebSocket.
class CloudSpeechStreamingService extends ValueNotifier<RealtimeSpeechState> {
  CloudSpeechStreamingService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    required Uri apiBaseUri,
    required String jwtToken,
    ClinicalTranscriptDraftGateway? draftStore,
    AudioRecorder? recorder,
    WebSocketChannel Function(Uri uri)? channelFactory,
  }) : _gateway = gateway,
       _visitId = visitId,
       _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _locale = locale,
       _apiBaseUri = apiBaseUri,
       _jwtToken = jwtToken,
       _draftStore = draftStore ?? const SecureClinicalTranscriptDraftStore(),
       _recorder = recorder ?? AudioRecorder(),
       _channelFactory = channelFactory ?? WebSocketChannel.connect,
       super(
         const RealtimeSpeechState(
           status: SpeechStatus.idle,
           stage: ClinicalVoiceStage.capture,
           transcript: '',
           segments: <ClinicalTranscriptSegment>[],
           partialTranscript: '',
           partialOffset: Duration.zero,
           vitals: PatientVitals(),
           note: ConsultationNote(),
           revisions: <ClinicalAiRevision>[],
         ),
       );

  static const int _maxReconnectAttempts = 6;
  static const int _maxPendingAudioBytes = 16_000 * 2 * 20;
  static const int _recentReplayAudioBytes = 16_000 * 2 * 3;
  static const Duration _connectTimeout = Duration(seconds: 12);
  static const Duration _startAckTimeout = Duration(seconds: 10);
  static const Duration _stopAckTimeout = Duration(seconds: 20);
  static const Duration _initialReconnectDelay = Duration(milliseconds: 500);
  static const Duration _maxReconnectDelay = Duration(seconds: 4);
  static const Duration _draftDebounce = Duration(milliseconds: 700);
  static const Duration _microphoneRetryDelay = Duration(milliseconds: 350);

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final Uri _apiBaseUri;
  final String _jwtToken;
  final ClinicalTranscriptDraftGateway _draftStore;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();
  final AudioRecorder _recorder;
  final WebSocketChannel Function(Uri uri) _channelFactory;
  final List<ClinicalTranscriptSegment> _segments =
      <ClinicalTranscriptSegment>[];
  final BoundedAudioReplayBuffer _pendingAudio = BoundedAudioReplayBuffer(
    maxBytes: _maxPendingAudioBytes,
  );
  final BoundedAudioReplayBuffer _recentAudio = BoundedAudioReplayBuffer(
    maxBytes: _recentReplayAudioBytes,
  );

  WebSocketChannel? _wsChannel;
  StreamSubscription<Uint8List>? _audioStreamSubscription;
  StreamSubscription<dynamic>? _wsStreamSubscription;
  Timer? _reconnectTimer;
  Timer? _draftTimer;
  Timer? _amplitudeTimer;
  Timer? _healthTimer;
  Completer<void>? _startAckCompleter;
  Completer<void>? _stopAckCompleter;
  Future<void> _draftPersistence = Future<void>.value();

  bool _disposed = false;
  bool _isStreaming = false;
  bool _shouldReconnect = false;
  bool _reconnectScheduled = false;
  bool _connecting = false;
  bool _transportReady = false;
  bool _audioRecoveryInProgress = false;
  bool _resumeAfterLifecycle = false;
  bool _localDraftRestored = false;
  bool _captureCompleted = false;
  DateTime? _captureStartedAt;
  DateTime? _lastAudioChunkAt;
  DateTime? _lastServerMessageAt;
  DateTime? _lastHeartbeatAt;
  DateTime? _connectedAt;
  DateTime? _lastAudioRecoveryAt;
  int _reconnectAttempts = 0;
  int _connectionGeneration = 0;
  int _audioRecoveryAttempts = 0;
  int _replaySeededGeneration = -1;
  String _currentPartial = '';
  Duration _currentPartialOffset = Duration.zero;

  Uri get _streamUri => buildClinicalVoiceWebSocketUri(
    apiBaseUri: _apiBaseUri,
    jwtToken: _jwtToken,
  );

  Future<void> initialize() async {
    if (_disposed) return;
    await _restoreLocalDraft();
    if (_disposed) return;
    value = value.copyWith(
      status: value.hasTranscript
          ? SpeechStatus.transcriptReview
          : SpeechStatus.idle,
      stage: ClinicalVoiceStage.capture,
      clearError: true,
    );
  }

  Future<void> startRealtimeListening() async {
    if (_disposed ||
        _isStreaming ||
        _connecting ||
        value.stage == ClinicalVoiceStage.review) {
      return;
    }
    if (_jwtToken.trim().isEmpty) {
      _setError(StateError('AUTH_SESSION_MISSING'));
      return;
    }

    _captureStartedAt ??= DateTime.now();
    _isStreaming = true;
    _shouldReconnect = true;
    _resumeAfterLifecycle = false;
    _reconnectAttempts = 0;
    _reconnectScheduled = false;
    _audioRecoveryAttempts = 0;
    _replaySeededGeneration = -1;
    _transportReady = false;
    _pendingAudio.clear();
    _recentAudio.clear();
    _lastAudioChunkAt = null;
    _lastServerMessageAt = null;
    _lastHeartbeatAt = null;
    _connectedAt = null;
    _lastAudioRecoveryAt = null;

    value = value.copyWith(
      status: SpeechStatus.processing,
      stage: ClinicalVoiceStage.capture,
      soundLevel: 0,
      clearError: true,
    );
    await _connectWebSocket();
  }

  Future<void> _connectWebSocket() async {
    if (_disposed || !_shouldReconnect || !_isStreaming || _connecting) return;

    _connecting = true;
    await _cleanupWebSocket();
    final generation = ++_connectionGeneration;

    try {
      final channel = _channelFactory(_streamUri);
      _wsChannel = channel;
      _wsStreamSubscription = channel.stream.listen(
        (message) => _handleWebSocketMessage(message, generation),
        onError: (Object error) => _handleWebSocketError(error, generation),
        onDone: () => _handleWebSocketClosed(generation),
        cancelOnError: true,
      );

      await channel.ready.timeout(_connectTimeout);
      if (!_isCurrentConnection(generation)) {
        await channel.sink.close();
        return;
      }

      final startAck = Completer<void>();
      _startAckCompleter = startAck;
      channel.sink.add(
        jsonEncode(<String, Object?>{
          'type': 'start',
          'visitId': _visitId,
          'locale': _locale,
        }),
      );
      await startAck.future.timeout(_startAckTimeout);
      if (!_isCurrentConnection(generation)) return;

      _transportReady = true;
      _lastServerMessageAt = DateTime.now();
      _lastHeartbeatAt = DateTime.now();
      _connectedAt = DateTime.now();

      if (_audioStreamSubscription == null) {
        await _startAudioCapture();
      } else {
        _flushBufferedAudio();
      }
      if (!_isCurrentConnection(generation)) return;

      _reconnectScheduled = false;
      _startHealthMonitoring();
      value = value.copyWith(
        status: SpeechStatus.listening,
        stage: ClinicalVoiceStage.capture,
        soundLevel: value.soundLevel <= 0 ? 12 : value.soundLevel,
        clearError: true,
      );
      debugPrint('WebSocket vocal prêt : $_streamUri');
    } catch (error) {
      debugPrint('Échec connexion WebSocket vocal: $error');
      _transportReady = false;
      await _cleanupWebSocket();
      if (_disposed || !_isStreaming || !_shouldReconnect) return;
      if (error is _VoiceSocketException && !error.retryable) {
        _stopAfterFailure(error);
      } else {
        await _scheduleReconnect(error);
      }
    } finally {
      if (generation == _connectionGeneration) {
        _connecting = false;
        _startAckCompleter = null;
      }
    }
  }

  bool _isCurrentConnection(int generation) {
    return !_disposed &&
        _isStreaming &&
        _shouldReconnect &&
        generation == _connectionGeneration &&
        _wsChannel != null;
  }

  Future<void> _startAudioCapture({bool fallback = false}) async {
    await _cleanupAudioCapture();
    if (_disposed || !_isStreaming || !_shouldReconnect) return;

    final hasPermission = await _recorder.hasPermission();
    if (!hasPermission) {
      throw StateError('MICROPHONE_PERMISSION_DENIED');
    }
    final supported = await _recorder.isEncoderSupported(
      AudioEncoder.pcm16bits,
    );
    if (!supported) {
      throw StateError('MICROPHONE_PCM_UNSUPPORTED');
    }

    final config = RecordConfig(
      encoder: AudioEncoder.pcm16bits,
      sampleRate: 16000,
      numChannels: 1,
      bitRate: 256000,
      streamBufferSize: 4096,
      androidConfig: fallback
          ? const AndroidRecordConfig(
              manageBluetooth: false,
              audioSource: AndroidAudioSource.voiceRecognition,
            )
          : const AndroidRecordConfig(),
    );

    final stream = await _recorder.startStream(config);
    _audioStreamSubscription = stream.listen(
      _sendAudioChunk,
      onError: _handleAudioStreamError,
      onDone: _handleAudioStreamDone,
      cancelOnError: true,
    );
    _lastAudioChunkAt = DateTime.now();
    _startAmplitudeMonitoring();
    _startHealthMonitoring();
  }

  void _sendAudioChunk(Uint8List audioChunk) {
    if (_disposed || !_isStreaming || audioChunk.isEmpty) return;
    _lastAudioChunkAt = DateTime.now();
    _recentAudio.add(audioChunk);

    if (!_transportReady || _wsChannel == null) {
      _pendingAudio.add(audioChunk);
      return;
    }
    _sendAudioChunkToSocket(audioChunk);
  }

  void _sendAudioChunkToSocket(Uint8List audioChunk) {
    final channel = _wsChannel;
    if (!_transportReady || channel == null) {
      _pendingAudio.add(audioChunk);
      return;
    }
    try {
      channel.sink.add(
        jsonEncode(<String, Object?>{
          'type': 'audio',
          'data': base64Encode(audioChunk),
        }),
      );
    } catch (error) {
      _handleWebSocketError(error, _connectionGeneration);
    }
  }

  void _seedReplayBeforeReconnect(int generation) {
    if (_replaySeededGeneration == generation) return;
    _replaySeededGeneration = generation;

    final alreadyPending = _pendingAudio.drain();
    for (final chunk in _recentAudio.snapshot()) {
      _pendingAudio.add(chunk);
    }
    for (final chunk in alreadyPending) {
      _pendingAudio.add(chunk);
    }
  }

  void _flushBufferedAudio() {
    if (!_transportReady || _wsChannel == null || _pendingAudio.isEmpty) return;
    final buffered = _pendingAudio.drain();
    for (var index = 0; index < buffered.length; index++) {
      if (!_transportReady || _wsChannel == null) {
        for (
          var pendingIndex = index;
          pendingIndex < buffered.length;
          pendingIndex++
        ) {
          _pendingAudio.add(buffered[pendingIndex]);
        }
        return;
      }
      _sendAudioChunkToSocket(buffered[index]);
    }
  }

  void _handleAudioStreamError(Object error) {
    debugPrint('Erreur capture microphone: $error');
    if (_disposed || !_isStreaming || !_shouldReconnect) return;
    unawaited(_recoverAudioCapture(error));
  }

  void _handleAudioStreamDone() {
    if (_disposed || !_isStreaming || !_shouldReconnect) return;
    debugPrint('Flux microphone terminé alors que la dictée reste active');
    unawaited(_recoverAudioCapture(StateError('MICROPHONE_STREAM_ENDED')));
  }

  Future<void> _recoverAudioCapture([Object? reason]) async {
    if (_disposed ||
        !_isStreaming ||
        !_shouldReconnect ||
        _audioRecoveryInProgress) {
      return;
    }
    _audioRecoveryInProgress = true;
    _audioRecoveryAttempts += 1;
    _lastAudioRecoveryAt = DateTime.now();

    if (_audioRecoveryAttempts > 3) {
      _audioRecoveryInProgress = false;
      _stopAfterFailure(
        StateError('MICROPHONE_INITIALIZATION_FAILED: ${reason ?? 'stalled'}'),
      );
      return;
    }

    try {
      await _cleanupAudioCapture();
      await Future<void>.delayed(_microphoneRetryDelay);
      if (_disposed || !_isStreaming || !_shouldReconnect) return;
      await _startAudioCapture(fallback: true);
    } catch (error) {
      _stopAfterFailure(StateError('MICROPHONE_INITIALIZATION_FAILED: $error'));
    } finally {
      _audioRecoveryInProgress = false;
    }
  }

  void _startAmplitudeMonitoring() {
    _amplitudeTimer?.cancel();
    _amplitudeTimer = Timer.periodic(const Duration(milliseconds: 160), (
      _,
    ) async {
      if (_disposed ||
          !_isStreaming ||
          value.status != SpeechStatus.listening) {
        return;
      }
      try {
        final amplitude = await _recorder.getAmplitude();
        final current = amplitude.current.isFinite ? amplitude.current : -80.0;
        final normalized = ((current + 60.0) / 60.0).clamp(0.0, 1.0);
        final boosted = 12.0 + math.pow(normalized, 0.45).toDouble() * 88.0;
        if (!_disposed && _isStreaming) {
          value = value.copyWith(soundLevel: boosted.clamp(12.0, 100.0));
        }
      } catch (_) {
        // L'animation n'est pas critique pour la dictée.
      }
    });
  }

  void _startHealthMonitoring() {
    _healthTimer ??= Timer.periodic(const Duration(seconds: 1), (_) {
      if (_disposed || !_isStreaming) {
        _healthTimer?.cancel();
        _healthTimer = null;
        return;
      }

      final now = DateTime.now();
      if (_audioStreamSubscription != null &&
          !_audioRecoveryInProgress &&
          CloudSpeechHealthPolicy.audioIsStalled(
            now: now,
            lastAudioChunkAt: _lastAudioChunkAt,
          )) {
        unawaited(
          _recoverAudioCapture(StateError('MICROPHONE_STREAM_STALLED')),
        );
      }

      final lastRecovery = _lastAudioRecoveryAt;
      final lastAudio = _lastAudioChunkAt;
      if (lastRecovery != null &&
          lastAudio != null &&
          now.difference(lastRecovery) > const Duration(seconds: 10) &&
          now.difference(lastAudio) < const Duration(seconds: 2)) {
        _audioRecoveryAttempts = 0;
        _lastAudioRecoveryAt = null;
      }

      if (_transportReady && _wsChannel != null) {
        if (CloudSpeechHealthPolicy.serverIsStalled(
          now: now,
          lastServerMessageAt: _lastServerMessageAt,
        )) {
          unawaited(
            _recoverTransport(
              StateError('VOICE_SOCKET_STALLED'),
              _connectionGeneration,
            ),
          );
          return;
        }
        if (CloudSpeechHealthPolicy.heartbeatIsDue(
          now: now,
          lastHeartbeatAt: _lastHeartbeatAt,
        )) {
          _lastHeartbeatAt = now;
          try {
            _wsChannel!.sink.add(jsonEncode(<String, Object?>{'type': 'ping'}));
          } catch (error) {
            _handleWebSocketError(error, _connectionGeneration);
          }
        }
      }

      final connectedAt = _connectedAt;
      if (_transportReady &&
          connectedAt != null &&
          now.difference(connectedAt) >=
              CloudSpeechHealthPolicy.stableConnectionDelay) {
        _reconnectAttempts = 0;
      }
    });
  }

  void _handleWebSocketMessage(dynamic message, int generation) {
    if (!_isCurrentConnection(generation)) return;
    _lastServerMessageAt = DateTime.now();
    try {
      final decoded = jsonDecode(message as String);
      if (decoded is! Map) return;
      final data = Map<String, dynamic>.from(decoded);
      final type = data['type']?.toString() ?? '';

      switch (type) {
        case 'transcript':
          final text = (data['text']?.toString() ?? '').trim();
          if (text.isNotEmpty) {
            _ingestTranscript(text, isFinal: data['isFinal'] == true);
          }
          break;
        case 'ack':
          final status = data['status']?.toString() ?? '';
          debugPrint('WebSocket ACK: $status');
          if (status == 'STREAMING_STARTED') {
            _completeStartWaiter();
          } else if (status == 'STREAMING_STOPPED') {
            _completeStopWaiter();
          }
          break;
        case 'error':
          final code = data['error']?.toString() ?? 'UNKNOWN_ERROR';
          final error = _VoiceSocketException(
            code,
            retryable: !const <String>{
              'UNAUTHORIZED',
              'VISIT_ID_REQUIRED',
              'VISIT_ID_INVALID',
              'STREAM_ALREADY_STARTED',
              'MESSAGE_TYPE_UNKNOWN',
            }.contains(code),
          );
          _failStartWaiter(error);
          _completeStopWaiter();
          if (_startAckCompleter == null || _startAckCompleter!.isCompleted) {
            if (error.retryable) {
              unawaited(_recoverTransport(error, generation));
            } else {
              _stopAfterFailure(error);
            }
          }
          break;
      }
    } catch (error) {
      debugPrint('Message WebSocket vocal illisible: $error');
    }
  }

  void _handleWebSocketError(Object error, int generation) {
    if (generation != _connectionGeneration || _disposed) return;
    debugPrint('WebSocket vocal en erreur: $error');
    _failStartWaiter(error);
    _completeStopWaiter();
    if (!_connecting && _shouldReconnect && _isStreaming) {
      unawaited(_recoverTransport(error, generation));
    }
  }

  void _handleWebSocketClosed(int generation) {
    if (generation != _connectionGeneration || _disposed) return;
    debugPrint('WebSocket vocal fermé');
    _failStartWaiter(StateError('VOICE_SOCKET_CLOSED'));
    _completeStopWaiter();
    if (!_connecting && _shouldReconnect && _isStreaming) {
      unawaited(
        _recoverTransport(StateError('VOICE_SOCKET_CLOSED'), generation),
      );
    }
  }

  Future<void> _recoverTransport(Object error, int generation) async {
    if (generation != _connectionGeneration || _reconnectScheduled) return;
    _seedReplayBeforeReconnect(generation);
    _transportReady = false;
    await _cleanupWebSocket();
    await _scheduleReconnect(error);
  }

  void _completeStartWaiter() {
    final completer = _startAckCompleter;
    if (completer != null && !completer.isCompleted) completer.complete();
  }

  void _failStartWaiter(Object error) {
    final completer = _startAckCompleter;
    if (completer != null && !completer.isCompleted) {
      completer.completeError(error);
    }
  }

  void _completeStopWaiter() {
    final completer = _stopAckCompleter;
    if (completer != null && !completer.isCompleted) completer.complete();
  }

  Future<void> _scheduleReconnect(Object error) async {
    if (_disposed ||
        !_shouldReconnect ||
        !_isStreaming ||
        _reconnectScheduled) {
      return;
    }
    if (_reconnectAttempts >= _maxReconnectAttempts) {
      _isStreaming = false;
      _shouldReconnect = false;
      await _cleanupAudioCapture();
      await _cleanupWebSocket();
      _pendingAudio.clear();
      _recentAudio.clear();
      _setError(StateError('VOICE_CONNECTION_UNAVAILABLE: $error'));
      return;
    }

    _reconnectAttempts += 1;
    _reconnectScheduled = true;
    final factor = 1 << (_reconnectAttempts - 1).clamp(0, 3).toInt();
    final delayMs = (_initialReconnectDelay.inMilliseconds * factor)
        .clamp(
          _initialReconnectDelay.inMilliseconds,
          _maxReconnectDelay.inMilliseconds,
        )
        .toInt();
    final delay = Duration(milliseconds: delayMs);
    final keepsCapturing = _audioStreamSubscription != null;

    value = value.copyWith(
      status: keepsCapturing ? SpeechStatus.listening : SpeechStatus.processing,
      soundLevel: keepsCapturing ? value.soundLevel : 0,
      errorMessage: _locale.toLowerCase().startsWith('en')
          ? (keepsCapturing
                ? 'Voice network interrupted. Audio stays buffered locally while reconnecting.'
                : 'Voice connection interrupted. Reconnecting '
                      '($_reconnectAttempts/$_maxReconnectAttempts)...')
          : (keepsCapturing
                ? 'Réseau vocal interrompu. L’audio reste capturé localement pendant la reconnexion.'
                : 'Connexion vocale interrompue. Reconnexion '
                      '($_reconnectAttempts/$_maxReconnectAttempts)…'),
    );

    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, () async {
      _reconnectScheduled = false;
      if (_disposed || !_shouldReconnect || !_isStreaming) return;
      await _connectWebSocket();
    });
  }

  Future<void> _cleanupAudioCapture() async {
    _amplitudeTimer?.cancel();
    _amplitudeTimer = null;
    final subscription = _audioStreamSubscription;
    _audioStreamSubscription = null;
    try {
      await subscription?.cancel();
    } catch (_) {
      // Le flux peut être déjà fermé.
    }
    try {
      await _recorder.stop();
    } catch (_) {
      // Aucun enregistrement actif.
    }
  }

  Future<void> _cleanupWebSocket() async {
    _transportReady = false;
    _connectedAt = null;
    final subscription = _wsStreamSubscription;
    _wsStreamSubscription = null;
    try {
      await subscription?.cancel();
    } catch (_) {
      // La souscription peut être déjà fermée.
    }
    final channel = _wsChannel;
    _wsChannel = null;
    try {
      await channel?.sink.close();
    } catch (_) {
      // Le canal peut être déjà fermé.
    }
  }

  void _ingestTranscript(String text, {required bool isFinal}) {
    if (_currentPartial.isEmpty) {
      _currentPartialOffset = _elapsedCaptureTime();
    }
    _currentPartial = text.trim();
    if (isFinal) {
      _commitCurrentPartial();
      return;
    }
    _publishCaptureState();
    _scheduleDraftSave();
  }

  void _commitCurrentPartial() {
    final text = _currentPartial.trim();
    if (text.isEmpty) return;

    if (_segments.isEmpty) {
      _segments.add(
        ClinicalTranscriptSegment(
          id: 'segment-${DateTime.now().microsecondsSinceEpoch}',
          offset: _currentPartialOffset,
          text: text,
        ),
      );
    } else {
      final last = _segments.last;
      final merged = mergeClinicalSpeechHypothesis(
        last.text,
        text,
        currentFinalized: true,
      );
      if (merged.startsNewSegment) {
        _segments.add(
          ClinicalTranscriptSegment(
            id: 'segment-${DateTime.now().microsecondsSinceEpoch}',
            offset: _currentPartialOffset,
            text: text,
          ),
        );
      } else if (merged.text != last.text) {
        _segments[_segments.length - 1] = last.copyWith(text: merged.text);
      }
    }

    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _publishCaptureState();
    _scheduleDraftSave();
  }

  Duration _elapsedCaptureTime() {
    final startedAt = _captureStartedAt;
    return startedAt == null
        ? Duration.zero
        : DateTime.now().difference(startedAt);
  }

  void _publishCaptureState() {
    final committed = clinicalTranscriptFromSegments(_segments);
    final partial = _currentPartial.trim();
    final separator = RegExp(r'[.!?;:]$').hasMatch(committed) ? ' ' : '. ';
    final transcript = <String>[
      if (committed.isNotEmpty) committed,
      if (partial.isNotEmpty) partial,
    ].join(separator).trim();

    value = value.copyWith(
      status: _isStreaming
          ? SpeechStatus.listening
          : (transcript.isEmpty
                ? SpeechStatus.idle
                : SpeechStatus.transcriptReview),
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      partialTranscript: partial,
      partialOffset: _currentPartialOffset,
      clearError: !value.hasTranscriptSyncFailure,
    );
  }

  void _publishReviewedTranscript({bool clearError = true}) {
    final transcript = clinicalTranscriptFromSegments(_segments);
    value = value.copyWith(
      status: transcript.isEmpty
          ? SpeechStatus.idle
          : SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      vitals: const PatientVitals(),
      note: const ConsultationNote(),
      revisions: const <ClinicalAiRevision>[],
      needsClarification: false,
      clearPartial: true,
      clearAssistant: true,
      clearError: clearError,
    );
  }

  ClinicalTranscriptDraft _currentDraft({bool explicitlyCleared = false}) {
    return ClinicalTranscriptDraft(
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      partialTranscript: _currentPartial.trim(),
      partialOffset: _currentPartialOffset,
      explicitlyCleared: explicitlyCleared,
    );
  }

  void _scheduleDraftSave() {
    if (_disposed) return;
    _draftTimer?.cancel();
    _draftTimer = Timer(_draftDebounce, () {
      unawaited(_persistCurrentTranscript(silent: true));
    });
  }

  Future<bool> _persistCurrentTranscript({
    bool silent = false,
    bool explicitlyCleared = false,
  }) {
    if (_disposed) return Future<bool>.value(false);
    _draftTimer?.cancel();
    final snapshot = _currentDraft(explicitlyCleared: explicitlyCleared);
    final completion = Completer<bool>();

    if (!silent) {
      value = value.copyWith(
        transcriptSyncStatus: TranscriptSyncStatus.syncing,
        clearError: true,
      );
    }

    _draftPersistence = _draftPersistence.then((_) async {
      try {
        await _draftStore.write(_visitId, snapshot);
        if (!_disposed) {
          value = value.copyWith(
            transcriptSyncStatus: TranscriptSyncStatus.synced,
          );
        }
        completion.complete(true);
      } catch (_) {
        if (!_disposed) {
          value = value.copyWith(
            transcriptSyncStatus: TranscriptSyncStatus.failed,
          );
        }
        completion.complete(false);
      }
    });
    return completion.future;
  }

  Future<bool> _ensureTranscriptPersisted() async {
    _draftTimer?.cancel();
    await _draftPersistence;
    if (_disposed) return false;
    if (!value.hasTranscript && _currentPartial.trim().isEmpty) return true;
    return _persistCurrentTranscript();
  }

  Future<void> _restoreLocalDraft() async {
    if (_localDraftRestored || _disposed) return;
    _localDraftRestored = true;
    try {
      final draft = await _draftStore.read(_visitId);
      if (draft == null || _disposed) return;

      _segments.clear();
      _currentPartial = '';
      _currentPartialOffset = Duration.zero;
      if (draft.explicitlyCleared) {
        value = value.copyWith(
          status: SpeechStatus.idle,
          stage: ClinicalVoiceStage.capture,
          transcript: '',
          segments: const <ClinicalTranscriptSegment>[],
          transcriptSyncStatus: TranscriptSyncStatus.synced,
          clearPartial: true,
          clearError: true,
        );
        return;
      }

      _segments.addAll(draft.segments);
      final partial = draft.partialTranscript.trim();
      if (partial.isNotEmpty) {
        _segments.add(
          ClinicalTranscriptSegment(
            id: 'restored-${DateTime.now().microsecondsSinceEpoch}',
            offset: draft.partialOffset,
            text: partial,
          ),
        );
      }
      _captureStartedAt = _segments.isEmpty ? null : DateTime.now();
      final transcript = clinicalTranscriptFromSegments(_segments);
      value = value.copyWith(
        status: transcript.isEmpty
            ? SpeechStatus.idle
            : SpeechStatus.transcriptReview,
        stage: ClinicalVoiceStage.capture,
        transcript: transcript,
        segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
        transcriptSyncStatus: TranscriptSyncStatus.synced,
        clearPartial: true,
        clearError: true,
      );
    } catch (_) {
      if (!_disposed) {
        value = value.copyWith(
          transcriptSyncStatus: TranscriptSyncStatus.failed,
        );
      }
    }
  }

  Future<void> _stopStreaming({bool sendStopMessage = true}) async {
    _healthTimer?.cancel();
    _healthTimer = null;
    _reconnectTimer?.cancel();
    _reconnectScheduled = false;
    _shouldReconnect = false;
    final wasStreaming = _isStreaming;

    await _cleanupAudioCapture();
    if (_transportReady) _flushBufferedAudio();
    if (sendStopMessage && wasStreaming && _wsChannel != null) {
      final stopAck = Completer<void>();
      _stopAckCompleter = stopAck;
      try {
        _wsChannel!.sink.add(jsonEncode(<String, Object?>{'type': 'stop'}));
        await stopAck.future.timeout(_stopAckTimeout);
      } on TimeoutException {
        debugPrint('Timeout de finalisation du streaming vocal');
      } catch (error) {
        debugPrint('Erreur finalisation du streaming vocal: $error');
      } finally {
        if (identical(_stopAckCompleter, stopAck)) _stopAckCompleter = null;
      }
    }

    _isStreaming = false;
    _commitCurrentPartial();
    await _cleanupWebSocket();
    _pendingAudio.clear();
    _recentAudio.clear();
  }

  Future<bool> retryDraftSave() => _persistCurrentTranscript();

  Future<bool> saveDictationForReview() async {
    if (_disposed ||
        value.stage == ClinicalVoiceStage.review ||
        value.status == SpeechStatus.processing) {
      return false;
    }
    _resumeAfterLifecycle = false;
    await _stopStreaming();
    final saved = await _persistCurrentTranscript();
    if (_disposed) return false;

    final transcript = clinicalTranscriptFromSegments(_segments);
    value = value.copyWith(
      status: transcript.isEmpty
          ? SpeechStatus.idle
          : SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      soundLevel: 0,
      transcriptSyncStatus: saved
          ? TranscriptSyncStatus.synced
          : TranscriptSyncStatus.failed,
      clearPartial: true,
      clearError: saved,
    );
    return saved;
  }

  Future<void> stopListening() async {
    if (_disposed) return;
    if (_isStreaming || value.hasTranscript || _currentPartial.isNotEmpty) {
      await saveDictationForReview();
      return;
    }
    value = value.copyWith(
      status: SpeechStatus.idle,
      soundLevel: 0,
      clearPartial: true,
      clearError: true,
    );
  }

  Future<bool> updateSegment(String segmentId, String text) async {
    if (_disposed ||
        value.status == SpeechStatus.listening ||
        value.status == SpeechStatus.processing) {
      return false;
    }
    final index = _segments.indexWhere((segment) => segment.id == segmentId);
    if (index < 0) return false;
    final normalized = text.trim();
    if (normalized.isEmpty) return deleteSegment(segmentId);

    final previous = List<ClinicalTranscriptSegment>.from(_segments);
    _segments[index] = _segments[index].copyWith(text: normalized);
    _publishReviewedTranscript();
    final saved = await _persistCurrentTranscript();
    if (!saved && !_disposed) {
      _segments
        ..clear()
        ..addAll(previous);
      _publishReviewedTranscript(clearError: false);
    }
    return saved;
  }

  Future<bool> deleteSegment(String segmentId) async {
    if (_disposed ||
        value.status == SpeechStatus.listening ||
        value.status == SpeechStatus.processing) {
      return false;
    }
    final index = _segments.indexWhere((segment) => segment.id == segmentId);
    if (index < 0) return false;

    final previous = List<ClinicalTranscriptSegment>.from(_segments);
    _segments.removeAt(index);
    _publishReviewedTranscript();
    final saved = await _persistCurrentTranscript();
    if (!saved && !_disposed) {
      _segments
        ..clear()
        ..addAll(previous);
      _publishReviewedTranscript(clearError: false);
    }
    return saved;
  }

  Future<bool> clearTranscript() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (_isStreaming) await _stopStreaming();

    final previous = List<ClinicalTranscriptSegment>.from(_segments);
    final previousPartial = _currentPartial;
    final previousOffset = _currentPartialOffset;
    _segments.clear();
    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _captureStartedAt = null;
    value = const RealtimeSpeechState(
      status: SpeechStatus.idle,
      stage: ClinicalVoiceStage.capture,
      transcript: '',
      segments: <ClinicalTranscriptSegment>[],
      partialTranscript: '',
      partialOffset: Duration.zero,
      vitals: PatientVitals(),
      note: ConsultationNote(),
      revisions: <ClinicalAiRevision>[],
      transcriptSyncStatus: TranscriptSyncStatus.syncing,
    );

    final saved = await _persistCurrentTranscript(explicitlyCleared: true);
    if (!saved && !_disposed) {
      _segments
        ..clear()
        ..addAll(previous);
      _currentPartial = previousPartial;
      _currentPartialOffset = previousOffset;
      _publishCaptureState();
    }
    return saved;
  }

  Future<void> analyzeTranscript() async {
    if (_disposed || !value.isTranscriptReadyForAnalysis) return;
    final transcript = clinicalTranscriptFromSegments(_segments);
    if (transcript.isEmpty || !await _ensureTranscriptPersisted()) return;

    value = value.copyWith(
      status: SpeechStatus.processing,
      stage: ClinicalVoiceStage.capture,
      clearError: true,
      clearAssistant: true,
    );
    try {
      final existing = await _gateway.getSession(_visitId);
      if (existing == null) {
        await _gateway.startSession(_visitId, _initialDraft, locale: _locale);
      }
      final state = await _gateway.analyzeTranscript(_visitId, transcript);
      if (!_disposed) _applyAiState(state);
    } catch (error) {
      _setError(error, fallbackStatus: SpeechStatus.transcriptReview);
    }
  }

  void _applyAiState(ClinicalAiState state) {
    final explicitVitals = _parser.parse(value.transcript).vitals;
    final aiVitals = state.vitalsFrom(includePending: true);
    value = value.copyWith(
      status: state.hasPendingProposals
          ? SpeechStatus.proposalReview
          : SpeechStatus.done,
      stage: ClinicalVoiceStage.review,
      vitals: explicitVitals.mergePrefer(aiVitals),
      note: state.noteFrom(includePending: true),
      revisions: state.revisions,
      assistantMessage: state.assistantMessage,
      needsClarification: state.needsClarification,
      clearPartial: true,
      clearError: true,
    );
  }

  Future<void> decideProposal(
    ClinicalAiRevision revision,
    ClinicalAiFieldProposal proposal,
    ClinicalAiDecision decision,
  ) async {
    if (_disposed || !proposal.isPending) return;
    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      final state = await _gateway.decideProposal(
        _visitId,
        revision.id,
        proposal.id,
        decision,
      );
      if (!_disposed) _applyAiState(state);
    } catch (error) {
      _setError(error, fallbackStatus: SpeechStatus.proposalReview);
    }
  }

  Future<void> decideRevision(
    ClinicalAiRevision revision,
    ClinicalAiDecision decision,
  ) async {
    if (_disposed || !revision.isPending) return;
    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      final state = await _gateway.decideRevision(
        _visitId,
        revision.id,
        decision,
      );
      if (!_disposed) _applyAiState(state);
    } catch (error) {
      _setError(error, fallbackStatus: SpeechStatus.proposalReview);
    }
  }

  Future<void> suspendForLifecycle() async {
    if (_disposed) return;
    final shouldResume = _isStreaming;
    _resumeAfterLifecycle = shouldResume;
    if (!shouldResume) return;
    await _stopStreaming();
    await _persistCurrentTranscript();
    if (!_disposed) value = value.copyWith(soundLevel: 0);
  }

  Future<void> resumeAfterLifecycle() async {
    if (_disposed || !_resumeAfterLifecycle) return;
    _resumeAfterLifecycle = false;
    await startRealtimeListening();
  }

  Future<bool> prepareForClose() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (_isStreaming) return saveDictationForReview();
    return _ensureTranscriptPersisted();
  }

  Future<void> completeCapture() async {
    if (_disposed) return;
    if (_isStreaming) await saveDictationForReview();
    _draftTimer?.cancel();
    await _draftPersistence;
    try {
      await _draftStore.delete(_visitId);
      _captureCompleted = true;
    } catch (_) {
      // Le résultat reste applicable même si le nettoyage local doit être repris.
    }
  }

  Future<bool> discardCurrentCapture() => clearTranscript();

  void _stopAfterFailure(Object error) {
    if (_disposed) return;
    _failStartWaiter(error);
    _completeStopWaiter();
    _isStreaming = false;
    _shouldReconnect = false;
    _reconnectScheduled = false;
    _connecting = false;
    _transportReady = false;
    _reconnectTimer?.cancel();
    _healthTimer?.cancel();
    _healthTimer = null;
    _pendingAudio.clear();
    _recentAudio.clear();
    unawaited(_cleanupAudioCapture());
    unawaited(_cleanupWebSocket());
    _setError(error);
    _scheduleDraftSave();
  }

  void _setError(
    Object error, {
    SpeechStatus fallbackStatus = SpeechStatus.error,
  }) {
    if (_disposed) return;
    value = value.copyWith(
      status: fallbackStatus,
      soundLevel: 0,
      errorMessage: clinicalVoiceUserMessage(error, locale: _locale),
    );
  }

  @override
  void dispose() {
    if (_disposed) return;
    _draftTimer?.cancel();
    _reconnectTimer?.cancel();
    _amplitudeTimer?.cancel();
    _healthTimer?.cancel();
    _pendingAudio.clear();
    _recentAudio.clear();
    _completeStopWaiter();
    if (!_captureCompleted &&
        (_segments.isNotEmpty || _currentPartial.trim().isNotEmpty)) {
      unawaited(_draftStore.write(_visitId, _currentDraft()));
    }

    _disposed = true;
    _isStreaming = false;
    _shouldReconnect = false;
    _reconnectScheduled = false;
    _connecting = false;
    _transportReady = false;
    unawaited(_audioStreamSubscription?.cancel());
    unawaited(_cleanupWebSocket());
    unawaited(_recorder.dispose());
    super.dispose();
  }
}

final class _VoiceSocketException implements Exception {
  const _VoiceSocketException(this.code, {required this.retryable});

  final String code;
  final bool retryable;

  @override
  String toString() => code;
}
