import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:record/record.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:wakelock_plus/wakelock_plus.dart';

import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';
import 'clinical_voice_error_message.dart';
import 'clinical_voice_state.dart';

export 'clinical_voice_error_message.dart';
export 'clinical_voice_state.dart';

/// Captation clinique instantanée en deux temps :
/// 1. écoute et transcription segmentée ;
/// 2. analyse IA du texte relu, puis revue des propositions cliniques.
class ClinicalSpeechService extends ValueNotifier<RealtimeSpeechState> {
  factory ClinicalSpeechService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    AudioRecorder? recorder,
    Future<Directory> Function()? temporaryDirectoryProvider,
    stt.SpeechToText? speech,
  }) {
    // Ces paramètres restent acceptés afin de ne pas casser les anciens points
    // d’injection pendant la migration vers la transcription native continue.
    return ClinicalSpeechService._(
      gateway,
      visitId,
      initialDraft,
      locale,
      speech: speech,
    );
  }

  ClinicalSpeechService._(
    this._gateway,
    this._visitId,
    Map<String, String> initialDraft,
    this._locale, {
    stt.SpeechToText? speech,
  }) : _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _speech = speech ?? stt.SpeechToText(),
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
  final stt.SpeechToText _speech;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();

  final List<ClinicalTranscriptSegment> _segments =
      <ClinicalTranscriptSegment>[];
  String _currentPartial = '';
  Duration _currentPartialOffset = Duration.zero;
  DateTime? _captureStartedAt;
  bool _sessionReady = false;
  bool _speechReady = false;
  bool _shouldKeepListening = false;
  bool _appActive = true;
  bool _resumeAfterLifecycle = false;
  bool _disposed = false;
  Timer? _restartTimer;

  Future<void> restoreOrStart() async {
    await initialize();
    if (_disposed || value.stage != ClinicalVoiceStage.capture) return;
    if (value.status == SpeechStatus.idle && !value.hasTranscript) {
      await startRealtimeListening();
    }
  }

  Future<void> initialize() async {
    if (_disposed) return;
    if (!_sessionReady) {
      value = value.copyWith(status: SpeechStatus.processing, clearError: true);
      try {
        final existing = await _gateway.getSession(_visitId);
        if (existing == null) {
          await _gateway.startSession(_visitId, _initialDraft, locale: _locale);
        } else {
          _restoreExistingSession(existing);
        }
        _sessionReady = true;
      } catch (error) {
        _setError(error);
        return;
      }
    }

    if (_speechReady) {
      _leaveInitializationState();
      return;
    }

    try {
      _speechReady = await _speech.initialize(
        onStatus: _handleSpeechStatus,
        onError: (error) {
          if (_disposed) return;
          final raw = error.errorMsg;
          if (_isRecoverableSpeechGap(raw) && _shouldKeepListening) {
            _commitCurrentPartial();
            _restartListeningLoop();
            return;
          }
          if (_shouldKeepListening) {
            value = value.copyWith(errorMessage: _userMessage(raw));
            _restartListeningLoop();
          } else {
            _setError(raw);
          }
        },
      );
      if (!_speechReady) {
        throw StateError('SPEECH_RECOGNITION_UNAVAILABLE');
      }
      _leaveInitializationState();
    } catch (error) {
      _speechReady = false;
      _setError(error);
    }
  }

  void _leaveInitializationState() {
    if (value.status == SpeechStatus.processing &&
        value.stage == ClinicalVoiceStage.capture) {
      value = value.copyWith(status: SpeechStatus.idle, clearError: true);
    }
  }

  void _restoreExistingSession(ClinicalAiState existing) {
    final pending = existing.pendingTranscript?.trim();
    final transcript = pending?.isNotEmpty == true
        ? pending!
        : existing.transcript?.trim();

    if (transcript?.isNotEmpty == true) {
      _replaceSegmentsWithTranscript(transcript!);
      value = value.copyWith(
        status: SpeechStatus.transcriptReview,
        stage: ClinicalVoiceStage.capture,
        transcript: transcript,
        segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
        clearPartial: true,
        clearError: true,
      );
    }
  }

  void _replaceSegmentsWithTranscript(String transcript) {
    _segments
      ..clear()
      ..add(
        ClinicalTranscriptSegment(
          id: 'restored-${DateTime.now().microsecondsSinceEpoch}',
          offset: Duration.zero,
          text: transcript.trim(),
        ),
      );
    _captureStartedAt ??= DateTime.now();
  }

  Future<void> startRealtimeListening() async {
    if (_disposed || value.status == SpeechStatus.listening) return;
    if (value.stage == ClinicalVoiceStage.review) return;
    await initialize();
    if (!_sessionReady || !_speechReady || _disposed) return;

    _captureStartedAt ??= DateTime.now();
    _shouldKeepListening = true;
    _resumeAfterLifecycle = false;
    _appActive = true;
    value = value.copyWith(
      status: SpeechStatus.listening,
      stage: ClinicalVoiceStage.capture,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      transcript: clinicalTranscriptFromSegments(_segments),
      soundLevel: 8,
      clearError: true,
      clearAssistant: true,
    );
    await _setAwake(true);
    await _listenInternal();
  }

  Future<void> _listenInternal() async {
    if (_disposed ||
        !_shouldKeepListening ||
        !_appActive ||
        !_speechReady ||
        _speech.isListening) {
      return;
    }

    try {
      await _speech.listen(
        onResult: (result) {
          if (_disposed || !_shouldKeepListening) return;
          final words = result.recognizedWords.trim();
          if (words.isNotEmpty && _currentPartial.isEmpty) {
            _currentPartialOffset = _elapsedCaptureTime();
          }
          _currentPartial = words;
          _publishCaptureState();
          if (result.finalResult) _commitCurrentPartial();
        },
        onSoundLevelChange: (level) {
          if (_disposed || value.status != SpeechStatus.listening) return;
          value = value.copyWith(
            soundLevel: (level + 2.0).clamp(5.0, 60.0).toDouble(),
          );
        },
        listenOptions: stt.SpeechListenOptions(
          listenFor: const Duration(hours: 1),
          pauseFor: const Duration(seconds: 8),
          partialResults: true,
          cancelOnError: false,
          listenMode: stt.ListenMode.dictation,
          localeId: _speechLocale,
        ),
      );
    } catch (error) {
      if (_shouldKeepListening) {
        value = value.copyWith(errorMessage: _userMessage(error));
        _restartListeningLoop();
      } else {
        _setError(error);
      }
    }
  }

  String get _speechLocale =>
      _locale.toLowerCase().startsWith('en') ? 'en_US' : 'fr_FR';

  void _handleSpeechStatus(String status) {
    if (_disposed || !_shouldKeepListening) return;
    if (status == 'done' || status == 'notListening') {
      _commitCurrentPartial();
      _restartListeningLoop();
    }
  }

  bool _isRecoverableSpeechGap(String raw) {
    return raw.contains('error_speech_timeout') ||
        raw.contains('error_no_match');
  }

  void _restartListeningLoop() {
    _restartTimer?.cancel();
    if (!_appActive || !_shouldKeepListening) return;
    _restartTimer = Timer(const Duration(milliseconds: 220), () {
      if (_shouldKeepListening && _appActive && !_disposed) {
        unawaited(_listenInternal());
      }
    });
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
    final transcript = <String>[
      if (committed.isNotEmpty) committed,
      if (partial.isNotEmpty) partial,
    ].join('. ').trim();

    value = value.copyWith(
      status: _shouldKeepListening
          ? SpeechStatus.listening
          : SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      partialTranscript: partial,
      partialOffset: _currentPartialOffset,
      clearError: true,
    );
  }

  void _commitCurrentPartial() {
    var text = _currentPartial.trim();
    if (text.isEmpty) return;

    final committed = clinicalTranscriptFromSegments(_segments);
    if (committed.isNotEmpty && text.startsWith(committed)) {
      text = text.substring(committed.length).trim();
      text = text.replaceFirst(RegExp(r'^[\s.,;:!?-]+'), '').trim();
    }

    if (text.isNotEmpty) {
      if (_segments.isNotEmpty && text.startsWith(_segments.last.text)) {
        _segments[_segments.length - 1] = _segments.last.copyWith(text: text);
      } else if (_segments.isEmpty || _segments.last.text != text) {
        _segments.add(
          ClinicalTranscriptSegment(
            id: 'segment-${DateTime.now().microsecondsSinceEpoch}',
            offset: _currentPartialOffset,
            text: text,
          ),
        );
      }
    }

    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _publishCaptureState();
  }

  Future<void> stopListening() async {
    if (_disposed || value.status != SpeechStatus.listening) return;
    _shouldKeepListening = false;
    _resumeAfterLifecycle = false;
    _restartTimer?.cancel();

    if (_speech.isListening) await _speech.stop();
    _commitCurrentPartial();
    await _setAwake(false);

    value = value.copyWith(
      status: _segments.isEmpty
          ? SpeechStatus.idle
          : SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: clinicalTranscriptFromSegments(_segments),
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      soundLevel: 0,
      clearPartial: true,
      clearError: true,
    );
  }

  void updateSegment(String segmentId, String text) {
    if (_disposed || value.status == SpeechStatus.listening) return;
    final index = _segments.indexWhere((segment) => segment.id == segmentId);
    if (index < 0) return;
    final normalized = text.trim();
    if (normalized.isEmpty) {
      deleteSegment(segmentId);
      return;
    }
    _segments[index] = _segments[index].copyWith(text: normalized);
    _publishReviewedTranscript();
  }

  void deleteSegment(String segmentId) {
    if (_disposed || value.status == SpeechStatus.listening) return;
    _segments.removeWhere((segment) => segment.id == segmentId);
    _publishReviewedTranscript();
  }

  void _publishReviewedTranscript() {
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
      clearError: true,
    );
  }

  Future<void> clearTranscript() async {
    if (_disposed || value.status == SpeechStatus.processing) return;
    if (value.status == SpeechStatus.listening) await stopListening();
    _segments.clear();
    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _captureStartedAt = null;
    try {
      await _gateway.discardPendingTranscript(_visitId);
    } catch (_) {
      // Le flux realtime n'a pas toujours de transcription serveur en attente.
    }
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
    );
  }

  void _applyAiState(ClinicalAiState state) {
    final aiVitals = state.vitalsFrom(includePending: true);
    final explicitVitals = _parser.parse(value.transcript).vitals;
    value = RealtimeSpeechState(
      status: state.hasPendingProposals
          ? SpeechStatus.proposalReview
          : SpeechStatus.done,
      stage: ClinicalVoiceStage.review,
      transcript: value.transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      partialTranscript: '',
      partialOffset: Duration.zero,
      vitals: explicitVitals.mergePrefer(aiVitals),
      note: state.noteFrom(includePending: true),
      revisions: state.revisions,
      assistantMessage: state.assistantMessage,
      needsClarification: state.needsClarification,
    );
  }

  Future<void> _setAwake(bool enabled) async {
    try {
      if (enabled) {
        await WakelockPlus.enable();
      } else {
        await WakelockPlus.disable();
      }
    } catch (_) {
      // Une erreur du plugin ne doit jamais invalider la transcription.
    }
  }

  String _userMessage(Object error) {
    return clinicalVoiceUserMessage(error, locale: _locale);
  }

  void _setError(
    Object error, {
    SpeechStatus fallbackStatus = SpeechStatus.error,
  }) {
    if (_disposed) return;
    value = value.copyWith(
      status: fallbackStatus,
      soundLevel: 0,
      errorMessage: _userMessage(error),
    );
  }

  @override
  void dispose() {
    _disposed = true;
    _shouldKeepListening = false;
    _resumeAfterLifecycle = false;
    _restartTimer?.cancel();
    unawaited(_speech.cancel());
    unawaited(_setAwake(false));
    super.dispose();
  }
}

extension ClinicalSpeechAnalysis on ClinicalSpeechService {
  Future<void> analyzeTranscript() async {
    if (_disposed) return;
    if (value.status == SpeechStatus.listening) await stopListening();
    final transcript = clinicalTranscriptFromSegments(_segments);
    if (transcript.isEmpty) return;

    await initialize();
    if (!_sessionReady || _disposed) return;
    value = value.copyWith(
      status: SpeechStatus.processing,
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      clearError: true,
      clearAssistant: true,
    );
    try {
      final state = await _gateway.analyzeTranscript(_visitId, transcript);
      _applyAiState(state);
    } catch (error) {
      _setError(error, fallbackStatus: SpeechStatus.transcriptReview);
    }
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
      _applyAiState(state);
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
      _applyAiState(state);
    } catch (error) {
      _setError(error, fallbackStatus: SpeechStatus.proposalReview);
    }
  }
}

extension ClinicalSpeechLifecycle on ClinicalSpeechService {
  Future<void> suspendForLifecycle() async {
    if (_disposed) return;
    _appActive = false;
    _restartTimer?.cancel();
    if (!_shouldKeepListening) return;

    _resumeAfterLifecycle = true;
    try {
      if (_speech.isListening) await _speech.stop();
    } catch (_) {
      // La plateforme peut déjà avoir arrêté le moteur lors du verrouillage.
    }
    _commitCurrentPartial();
    await _setAwake(false);
    value = value.copyWith(soundLevel: 0);
  }

  Future<void> resumeAfterLifecycle() async {
    if (_disposed) return;
    _appActive = true;
    if (!_resumeAfterLifecycle || !_shouldKeepListening) return;

    _resumeAfterLifecycle = false;
    value = value.copyWith(
      status: SpeechStatus.listening,
      stage: ClinicalVoiceStage.capture,
      clearError: true,
    );
    await _setAwake(true);
    _restartListeningLoop();
  }

  Future<bool> prepareForClose() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (value.status == SpeechStatus.listening) await stopListening();
    return true;
  }

  Future<void> discardCurrentCapture() => clearTranscript();
}
