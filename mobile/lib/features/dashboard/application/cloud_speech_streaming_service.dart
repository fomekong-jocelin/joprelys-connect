// ignore_for_file: prefer_initializing_formals

import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:record/record.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import '../data/clinical_transcript_draft_store.dart';
import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';
import 'clinical_voice_error_message.dart';
import 'clinical_voice_state.dart';

export 'clinical_voice_error_message.dart';
export 'clinical_voice_state.dart';

/// Service de dictée clinique utilisant le streaming cloud via WebSocket.
///
/// Le service conserve le même contrat fonctionnel que le moteur local :
/// capture, sauvegarde du brouillon, correction des segments, cycle de vie,
/// revue des propositions IA et nettoyage après application.
class CloudSpeechStreamingService extends ValueNotifier<RealtimeSpeechState> {
  CloudSpeechStreamingService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    required String backendWsUrl,
    required String jwtToken,
    ClinicalTranscriptDraftGateway? draftStore,
  }) : _gateway = gateway,
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

  static const int _maxReconnectAttempts = 5;
  static const Duration _initialReconnectDelay = Duration(seconds: 1);
  static const Duration _maxReconnectDelay = Duration(seconds: 30);
  static const Duration _draftDebounce = Duration(milliseconds: 700);
  static const Duration _stopAckTimeout = Duration(seconds: 15);

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final String _backendWsUrl;
  final String _jwtToken;
  final ClinicalTranscriptDraftGateway _draftStore;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();
  final AudioRecorder _recorder = AudioRecorder();

  final List<ClinicalTranscriptSegment> _segments =
      <ClinicalTranscriptSegment>[];

  WebSocketChannel? _wsChannel;
  StreamSubscription<Uint8List>? _audioStreamSubscription;
  StreamSubscription<dynamic>? _wsStreamSubscription;
  Timer? _reconnectTimer;
  Timer? _draftTimer;
  Completer<void>? _stopAckCompleter;
  Future<void> _draftPersistence = Future<void>.value();

  bool _disposed = false;
  bool _isStreaming = false;
  bool _shouldReconnect = false;
  bool _reconnectScheduled = false;
  bool _resumeAfterLifecycle = false;
  bool _localDraftRestored = false;
  bool _captureCompleted = false;
  DateTime? _captureStartedAt;
  int _reconnectAttempts = 0;
  String _currentPartial = '';
  Duration _currentPartialOffset = Duration.zero;

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
    if (_disposed || _isStreaming || value.stage == ClinicalVoiceStage.review) {
      return;
    }

    _captureStartedAt ??= DateTime.now();
    _isStreaming = true;
    _shouldReconnect = true;
    _resumeAfterLifecycle = false;
    _reconnectAttempts = 0;
    _reconnectScheduled = false;

    value = value.copyWith(
      status: SpeechStatus.listening,
      stage: ClinicalVoiceStage.capture,
      soundLevel: 12,
      clearError: true,
    );

    await _connectWebSocket();
  }

  Future<void> _connectWebSocket() async {
    if (_disposed || !_shouldReconnect || !_isStreaming) return;

    try {
      final wsUrl = '$_backendWsUrl/api/voice/stream?token=$_jwtToken';
      _wsChannel = WebSocketChannel.connect(Uri.parse(wsUrl));

      _wsStreamSubscription = _wsChannel!.stream.listen(
        _handleWebSocketMessage,
        onError: _handleWebSocketError,
        onDone: _handleWebSocketClosed,
      );

      _wsChannel!.sink.add(
        jsonEncode(<String, Object?>{
          'type': 'start',
          'visitId': _visitId,
          'locale': _locale,
        }),
      );

      await _startAudioCapture();
      if (_disposed || !_isStreaming) return;

      _reconnectAttempts = 0;
      _reconnectScheduled = false;
      value = value.copyWith(
        status: SpeechStatus.listening,
        stage: ClinicalVoiceStage.capture,
        clearError: true,
      );
      debugPrint('WebSocket connecté : visitId=$_visitId');
    } catch (error) {
      debugPrint('Erreur connexion WebSocket: $error');
      await _scheduleReconnect();
    }
  }

  Future<void> _startAudioCapture() async {
    await _cleanupAudioCapture();
    if (_disposed || !_isStreaming) return;

    try {
      final stream = await _recorder.startStream(
        const RecordConfig(
          encoder: AudioEncoder.pcm16bits,
          sampleRate: 16000,
          numChannels: 1,
          bitRate: 256000,
        ),
      );

      _audioStreamSubscription = stream.listen((audioChunk) {
        if (!_isStreaming || _wsChannel == null) return;
        try {
          _wsChannel!.sink.add(
            jsonEncode(<String, Object?>{
              'type': 'audio',
              'data': base64Encode(audioChunk),
            }),
          );
        } catch (error) {
          debugPrint('Erreur envoi audio chunk: $error');
        }
      }, onError: _handleStreamError);
    } catch (error) {
      debugPrint('Erreur démarrage capture audio: $error');
      _stopAfterFailure(error);
    }
  }

  void _handleWebSocketMessage(dynamic message) {
    if (_disposed || !_isStreaming) return;

    try {
      final decoded = jsonDecode(message as String);
      if (decoded is! Map) return;
      final data = Map<String, dynamic>.from(decoded);
      final type = data['type']?.toString();

      switch (type) {
        case 'transcript':
          final text = (data['text']?.toString() ?? '').trim();
          final isFinal = data['isFinal'] == true;
          if (text.isNotEmpty) {
            _ingestTranscript(text, isFinal: isFinal);
          }
          break;
        case 'error':
          _completeStopWaiter();
          _stopAfterFailure(data['error']?.toString() ?? 'UNKNOWN_ERROR');
          break;
        case 'ack':
          final status = data['status']?.toString() ?? '';
          debugPrint('WebSocket ACK: $status');
          if (status == 'STREAMING_STOPPED') {
            _completeStopWaiter();
          }
          break;
      }
    } catch (error) {
      debugPrint('Erreur parsing message WebSocket: $error');
    }
  }

  void _handleWebSocketError(Object error) {
    debugPrint('WebSocket erreur: $error');
    if (!_shouldReconnect) {
      _completeStopWaiter();
      return;
    }
    if (!_disposed && _isStreaming) {
      unawaited(_scheduleReconnect());
    }
  }

  void _handleWebSocketClosed() {
    debugPrint('WebSocket fermé');
    if (!_shouldReconnect) {
      _completeStopWaiter();
      return;
    }
    if (!_disposed && _isStreaming) {
      unawaited(_scheduleReconnect());
    }
  }

  void _completeStopWaiter() {
    final completer = _stopAckCompleter;
    if (completer != null && !completer.isCompleted) {
      completer.complete();
    }
  }

  Future<void> _scheduleReconnect() async {
    if (_disposed ||
        !_shouldReconnect ||
        !_isStreaming ||
        _reconnectScheduled) {
      return;
    }

    if (_reconnectAttempts >= _maxReconnectAttempts) {
      await _stopStreaming(sendStopMessage: false);
      _setError('RECONNECT_MAX_ATTEMPTS_REACHED');
      return;
    }

    _reconnectAttempts += 1;
    _reconnectScheduled = true;
    final factor = 1 << (_reconnectAttempts - 1).clamp(0, 5).toInt();
    final delayMs = (_initialReconnectDelay.inMilliseconds * factor)
        .clamp(
          _initialReconnectDelay.inMilliseconds,
          _maxReconnectDelay.inMilliseconds,
        )
        .toInt();
    final delay = Duration(milliseconds: delayMs);

    debugPrint(
      'Reconnexion dans ${delay.inSeconds}s '
      '(tentative $_reconnectAttempts/$_maxReconnectAttempts)',
    );

    value = value.copyWith(
      status: SpeechStatus.processing,
      errorMessage: _locale.toLowerCase().startsWith('en')
          ? 'Reconnecting...'
          : 'Reconnexion en cours...',
    );

    _reconnectTimer?.cancel();
    _reconnectTimer = Timer(delay, () async {
      _reconnectScheduled = false;
      if (_disposed || !_shouldReconnect || !_isStreaming) return;
      await _cleanupAudioCapture();
      await _cleanupWebSocket();
      await _connectWebSocket();
    });
  }

  Future<void> _cleanupAudioCapture() async {
    try {
      await _recorder.stop();
    } catch (_) {
      // Aucun enregistrement actif.
    }

    try {
      await _audioStreamSubscription?.cancel();
    } catch (_) {
      // La plateforme peut avoir déjà fermé le flux.
    }
    _audioStreamSubscription = null;
  }

  Future<void> _cleanupWebSocket() async {
    try {
      await _wsStreamSubscription?.cancel();
    } catch (_) {
      // Le canal peut être déjà fermé.
    }
    _wsStreamSubscription = null;

    try {
      await _wsChannel?.sink.close();
    } catch (_) {
      // Le canal peut être déjà fermé.
    }
    _wsChannel = null;
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
    _reconnectTimer?.cancel();
    _reconnectScheduled = false;
    _shouldReconnect = false;
    final wasStreaming = _isStreaming;

    // Garder _isStreaming=true jusqu'à l'accusé final permet de recevoir et
    // d'intégrer les dernières transcriptions envoyées après le message stop.
    await _cleanupAudioCapture();

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
        if (identical(_stopAckCompleter, stopAck)) {
          _stopAckCompleter = null;
        }
      }
    }

    _isStreaming = false;
    _commitCurrentPartial();
    await _cleanupWebSocket();
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
      // La synthèse reste applicable même si le nettoyage local doit être repris.
    }
  }

  Future<bool> discardCurrentCapture() => clearTranscript();

  void _handleStreamError(Object error) {
    debugPrint('Erreur stream: $error');
    if (_shouldReconnect && !_disposed && _isStreaming) {
      unawaited(_scheduleReconnect());
    } else {
      _completeStopWaiter();
      _stopAfterFailure(error);
    }
  }

  void _stopAfterFailure(Object error) {
    if (_disposed) return;
    _completeStopWaiter();
    _isStreaming = false;
    _shouldReconnect = false;
    _reconnectScheduled = false;
    _reconnectTimer?.cancel();
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
    _completeStopWaiter();
    if (!_captureCompleted &&
        (_segments.isNotEmpty || _currentPartial.trim().isNotEmpty)) {
      unawaited(_draftStore.write(_visitId, _currentDraft()));
    }

    _disposed = true;
    _isStreaming = false;
    _shouldReconnect = false;
    _reconnectScheduled = false;
    unawaited(_audioStreamSubscription?.cancel());
    unawaited(_recorder.dispose());
    unawaited(_cleanupWebSocket());
    super.dispose();
  }
}
