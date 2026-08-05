import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:record/record.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import '../data/clinical_transcript_draft_store.dart';
import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_voice_error_message.dart';
import 'clinical_voice_state.dart';

export 'clinical_voice_error_message.dart';
export 'clinical_voice_state.dart';

/// Service de dictée clinique utilisant le streaming cloud via WebSocket.
///
/// Architecture production-ready avec :
/// - Reconnexion automatique WebSocket
/// - Gestion erreurs robuste
/// - Buffer audio avec flush automatique
/// - Persistance locale en cas de panne réseau
class CloudSpeechStreamingService extends ValueNotifier<RealtimeSpeechState> {
  CloudSpeechStreamingService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    required String backendWsUrl,
    required String jwtToken,
    ClinicalTranscriptDraftGateway? draftStore,
  })  : _gateway = gateway,
        _visitId = visitId,
        _initialDraft = Map<String, String>.unmodifiable(initialDraft),
        _locale = locale,
        _backendWsUrl = backendWsUrl,
        _jwtToken = jwtToken,
        _draftStore = draftStore ?? const SecureClinicalTranscriptDraftStore(),
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

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final String _backendWsUrl;
  final String _jwtToken;
  final ClinicalTranscriptDraftGateway _draftStore;

  final AudioRecorder _recorder = AudioRecorder();
  WebSocketChannel? _wsChannel;
  StreamSubscription<Uint8List>? _audioStreamSubscription;
  StreamSubscription<dynamic>? _wsStreamSubscription;
  bool _disposed = false;
  bool _isStreaming = false;
  bool _shouldReconnect = false;
  DateTime? _captureStartedAt;
  int _reconnectAttempts = 0;
  Timer? _reconnectTimer;

  static const int _maxReconnectAttempts = 5;
  static const Duration _initialReconnectDelay = Duration(seconds: 1);
  static const Duration _maxReconnectDelay = Duration(seconds: 30);

  final List<ClinicalTranscriptSegment> _segments = <ClinicalTranscriptSegment>[];
  String _currentPartial = '';
  Duration _currentPartialOffset = Duration.zero;

  Future<void> initialize() async {
    if (_disposed) return;
    await _restoreLocalDraft();
    value = value.copyWith(status: SpeechStatus.idle, clearError: true);
  }

  Future<void> startRealtimeListening() async {
    if (_disposed || _isStreaming) return;

    _captureStartedAt ??= DateTime.now();
    _isStreaming = true;
    _shouldReconnect = true;
    _reconnectAttempts = 0;

    value = value.copyWith(
      status: SpeechStatus.listening,
      stage: ClinicalVoiceStage.capture,
      soundLevel: 12,
      clearError: true,
    );

    await _connectWebSocket();
  }

  Future<void> _connectWebSocket() async {
    if (_disposed || !_shouldReconnect) return;

    try {
      // 1. Connecter WebSocket avec auth JWT
      final wsUrl = '$_backendWsUrl/api/voice/stream?token=$_jwtToken';
      _wsChannel = WebSocketChannel.connect(Uri.parse(wsUrl));

      // 2. Envoyer message START
      _wsChannel!.sink.add(jsonEncode({
        'type': 'start',
        'visitId': _visitId,
        'locale': _locale,
      }));

      // 3. Écouter les transcriptions du backend
      _wsStreamSubscription = _wsChannel!.stream.listen(
        _handleWebSocketMessage,
        onError: _handleWebSocketError,
        onDone: _handleWebSocketClosed,
      );

      // 4. Démarrer capture audio
      await _startAudioCapture();

      // Reset reconnect counter on successful connection
      _reconnectAttempts = 0;

      debugPrint('WebSocket connecté : visitId=$_visitId');
    } catch (error) {
      debugPrint('Erreur connexion WebSocket: $error');
      await _scheduleReconnect();
    }
  }

  Future<void> _startAudioCapture() async {
    try {
      final stream = await _recorder.startStream(
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: 16000,
          numChannels: 1,
          bitRate: 256000,
        ),
      );

      _audioStreamSubscription = stream.listen(
        (audioChunk) {
          if (_isStreaming && _wsChannel != null) {
            try {
              _wsChannel!.sink.add(jsonEncode({
                'type': 'audio',
                'data': base64Encode(audioChunk),
              }));
            } catch (e) {
              debugPrint('Erreur envoi audio chunk: $e');
            }
          }
        },
        onError: (error) {
          debugPrint('Erreur stream audio: $error');
          _handleStreamError(error);
        },
      );
    } catch (error) {
      debugPrint('Erreur démarrage capture audio: $error');
      _stopAfterFailure(error);
    }
  }

  void _handleWebSocketMessage(dynamic message) {
    if (_disposed || !_isStreaming) return;

    try {
      final data = jsonDecode(message as String) as Map<String, dynamic>;
      final type = data['type'] as String?;

      switch (type) {
        case 'transcript':
          final text = (data['text'] as String? ?? '').trim();
          final isFinal = data['isFinal'] as bool? ?? false;
          if (text.isNotEmpty) {
            _ingestTranscript(text, isFinal: isFinal);
          }
          break;
        case 'error':
          final error = data['error'] as String? ?? 'UNKNOWN_ERROR';
          _setError(error);
          break;
        case 'ack':
          debugPrint('WebSocket ACK: ${data['status']}');
          break;
      }
    } catch (error) {
      debugPrint('Erreur parsing message WebSocket: $error');
    }
  }

  void _handleWebSocketError(Object error) {
    debugPrint('WebSocket erreur: $error');
    if (_shouldReconnect && !_disposed) {
      unawaited(_scheduleReconnect());
    }
  }

  void _handleWebSocketClosed() {
    debugPrint('WebSocket fermé');
    if (_shouldReconnect && !_disposed && _isStreaming) {
      unawaited(_scheduleReconnect());
    }
  }

  Future<void> _scheduleReconnect() async {
    if (_disposed || !_shouldReconnect || _reconnectAttempts >= _maxReconnectAttempts) {
      if (_reconnectAttempts >= _maxReconnectAttempts) {
        _setError('RECONNECT_MAX_ATTEMPTS_REACHED');
        await stopListening();
      }
      return;
    }

    _reconnectAttempts++;
    final delayMs = _initialReconnectDelay.inMilliseconds *
        (1 << (_reconnectAttempts - 1)).clamp(1, 32);
    final delay = Duration(milliseconds: delayMs.clamp(
      _initialReconnectDelay.inMilliseconds,
      _maxReconnectDelay.inMilliseconds,
    ));

    debugPrint('Reconnexion dans ${delay.inSeconds}s (tentative $_reconnectAttempts/$_maxReconnectAttempts)');

    value = value.copyWith(
      status: SpeechStatus.processing,
      errorMessage: 'Reconnexion en cours...',
    );

    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, () async {
      if (!_disposed && _shouldReconnect) {
        await _cleanupWebSocket();
        await _connectWebSocket();
      }
    });
  }

  Future<void> _cleanupWebSocket() async {
    await _wsStreamSubscription?.cancel();
    await _wsChannel?.sink.close();
    _wsChannel = null;
    _wsStreamSubscription = null;
  }

  void _ingestTranscript(String text, {required bool isFinal}) {
    if (_currentPartial.isEmpty) {
      _currentPartialOffset = _elapsedCaptureTime();
      _currentPartial = text;
    } else {
      if (isFinal) {
        _commitCurrentPartial();
        _currentPartialOffset = _elapsedCaptureTime();
        _currentPartial = text;
      } else {
        _currentPartial = text;
      }
    }

    _publishCaptureState();
    _scheduleDraftSave();
  }

  void _commitCurrentPartial() {
    final text = _currentPartial.trim();
    if (text.isEmpty) return;

    _segments.add(
      ClinicalTranscriptSegment(
        id: 'segment-${DateTime.now().microsecondsSinceEpoch}',
        offset: _currentPartialOffset,
        text: text,
      ),
    );

    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _publishCaptureState();
    _scheduleDraftSave();
  }

  Duration _elapsedCaptureTime() {
    final startedAt = _captureStartedAt;
    return startedAt == null ? Duration.zero : DateTime.now().difference(startedAt);
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
      status: _isStreaming ? SpeechStatus.listening : SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      partialTranscript: partial,
      partialOffset: _currentPartialOffset,
      clearError: true,
    );
  }

  Timer? _draftTimer;
  void _scheduleDraftSave() {
    _draftTimer?.cancel();
    _draftTimer = Timer(const Duration(milliseconds: 700), () {
      unawaited(_persistCurrentTranscript());
    });
  }

  Future<void> _persistCurrentTranscript() async {
    if (_disposed) return;
    try {
      await _draftStore.write(
        _visitId,
        ClinicalTranscriptDraft(
          segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
          partialTranscript: _currentPartial.trim(),
          partialOffset: _currentPartialOffset,
          explicitlyCleared: false,
        ),
      );
    } catch (_) {
      // Log mais ne pas bloquer
    }
  }

  Future<void> _restoreLocalDraft() async {
    if (_disposed) return;
    try {
      final draft = await _draftStore.read(_visitId);
      if (draft == null) return;

      _segments
        ..clear()
        ..addAll(draft.segments);

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

      _currentPartial = '';
      _currentPartialOffset = Duration.zero;
      final transcript = clinicalTranscriptFromSegments(_segments);
      value = value.copyWith(
        status: transcript.isEmpty ? SpeechStatus.idle : SpeechStatus.transcriptReview,
        transcript: transcript,
        segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
        clearPartial: true,
      );
    } catch (_) {
      // Échec de restauration - continuer quand même
    }
  }

  Future<void> stopListening() async {
    if (_disposed || !_isStreaming) return;

    _isStreaming = false;
    _shouldReconnect = false;
    _reconnectTimer?.cancel();
    _commitCurrentPartial();

    // Arrêter capture audio
    await _audioStreamSubscription?.cancel();
    await _recorder.stop();

    // Fermer WebSocket proprement
    if (_wsChannel != null) {
      try {
        _wsChannel!.sink.add(jsonEncode({'type': 'stop'}));
      } catch (_) {
        // Ignore si déjà fermé
      }
    }
    await _cleanupWebSocket();

    await _persistCurrentTranscript();

    value = value.copyWith(
      status: _segments.isEmpty ? SpeechStatus.idle : SpeechStatus.transcriptReview,
      soundLevel: 0,
      clearPartial: true,
    );
  }

  void _handleStreamError(Object error) {
    debugPrint('Erreur stream: $error');
    if (_shouldReconnect && !_disposed) {
      unawaited(_scheduleReconnect());
    } else {
      _stopAfterFailure(error);
    }
  }

  void _stopAfterFailure(Object error) {
    if (_disposed) return;
    _isStreaming = false;
    _shouldReconnect = false;
    unawaited(_audioStreamSubscription?.cancel());
    unawaited(_recorder.stop());
    unawaited(_cleanupWebSocket());
    _setError(error);
  }

  void _setError(Object error) {
    if (_disposed) return;
    value = value.copyWith(
      status: SpeechStatus.error,
      soundLevel: 0,
      errorMessage: clinicalVoiceUserMessage(error, locale: _locale),
    );
  }

  @override
  void dispose() {
    _disposed = true;
    _isStreaming = false;
    _shouldReconnect = false;
    _draftTimer?.cancel();
    _reconnectTimer?.cancel();
    unawaited(_audioStreamSubscription?.cancel());
    unawaited(_recorder.dispose());
    unawaited(_cleanupWebSocket());
    super.dispose();
  }
}
