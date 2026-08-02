import 'dart:async';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:record/record.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:wakelock_plus/wakelock_plus.dart';

import '../data/clinical_transcript_draft_store.dart';
import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';
import 'clinical_speech_hypothesis.dart';
import 'clinical_voice_error_message.dart';
import 'clinical_voice_state.dart';

export 'clinical_voice_error_message.dart';
export 'clinical_voice_state.dart';

/// Captation clinique instantanée en deux temps :
/// 1. écoute et transcription segmentée durable ;
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
    ClinicalTranscriptDraftGateway? draftStore,
  }) {
    // Ces paramètres restent acceptés afin de ne pas casser les anciens points
    // d'injection pendant la migration vers la transcription native continue.
    return ClinicalSpeechService._(
      gateway,
      visitId,
      initialDraft,
      locale,
      speech: speech,
      draftStore: draftStore,
    );
  }

  ClinicalSpeechService._(
    this._gateway,
    this._visitId,
    Map<String, String> initialDraft,
    this._locale, {
    stt.SpeechToText? speech,
    ClinicalTranscriptDraftGateway? draftStore,
  }) : _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _speech = speech ?? stt.SpeechToText(),
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

  static const Duration _draftDebounce = Duration(milliseconds: 700);

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final stt.SpeechToText _speech;
  final ClinicalTranscriptDraftGateway _draftStore;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();

  final List<ClinicalTranscriptSegment> _segments =
      <ClinicalTranscriptSegment>[];
  String _currentPartial = '';
  Duration _currentPartialOffset = Duration.zero;
  bool _currentPartialFinalized = false;
  DateTime? _captureStartedAt;
  bool _sessionReady = false;
  bool _speechReady = false;
  bool _localDraftRestored = false;
  bool _hasLocalDraftSnapshot = false;
  bool _shouldKeepListening = false;
  bool _appActive = true;
  bool _resumeAfterLifecycle = false;
  bool _disposed = false;
  Timer? _restartTimer;
  Timer? _localDraftTimer;
  double _minimumSoundLevel = double.infinity;
  double _maximumSoundLevel = -double.infinity;
  Future<void> _draftPersistence = Future<void>.value();

  Future<void> restoreOrStart() async {
    await initialize();
    if (_disposed ||
        value.stage != ClinicalVoiceStage.capture ||
        !_speechReady) {
      return;
    }

    unawaited(_ensureSessionReady(silent: true));
    if (value.status == SpeechStatus.idle && !value.hasTranscript) {
      await startRealtimeListening();
    }
  }

  Future<void> initialize() async {
    if (_disposed) return;
    await _restoreLocalDraft();

    if (_speechReady) {
      _leaveInitializationState();
      return;
    }

    if (!value.hasTranscript) {
      value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    }

    try {
      _speechReady = await _speech.initialize(
        onStatus: _handleSpeechStatus,
        onError: (error) {
          if (_disposed) return;
          final raw = error.errorMsg;
          if (_isRecoverableSpeechGap(raw) && _shouldKeepListening) {
            _currentPartialFinalized = true;
            _publishCaptureState();
            _scheduleDraftSave();
            _restartListeningLoop();
            return;
          }

          _shouldKeepListening = false;
          _resumeAfterLifecycle = false;
          unawaited(_setAwake(false));
          _setError(
            raw,
            fallbackStatus: value.hasTranscript
                ? SpeechStatus.transcriptReview
                : SpeechStatus.idle,
          );
        },
      );
      if (!_speechReady) {
        throw StateError('SPEECH_RECOGNITION_UNAVAILABLE');
      }
      _leaveInitializationState();
    } catch (error) {
      _speechReady = false;
      _setError(
        error,
        fallbackStatus: value.hasTranscript
            ? SpeechStatus.transcriptReview
            : SpeechStatus.idle,
      );
    }
  }

  Future<bool> _ensureSessionReady({bool silent = false}) async {
    if (_disposed) return false;
    if (_sessionReady) return true;

    try {
      final existing = await _gateway.getSession(_visitId);
      if (existing == null) {
        await _gateway.startSession(_visitId, _initialDraft, locale: _locale);
      } else if (!_hasLocalDraftSnapshot) {
        _restoreExistingSession(existing);
      }
      _sessionReady = true;
      return true;
    } catch (error) {
      if (!silent) {
        _setError(
          error,
          fallbackStatus: value.hasTranscript
              ? SpeechStatus.transcriptReview
              : SpeechStatus.idle,
        );
      }
      return false;
    }
  }

  Future<void> _restoreLocalDraft() async {
    if (_localDraftRestored || _disposed) return;
    _localDraftRestored = true;

    try {
      final draft = await _draftStore.read(_visitId);
      if (draft == null) return;
      _hasLocalDraftSnapshot = true;
      _segments
        ..clear()
        ..addAll(draft.segments);

      final partial = draft.partialTranscript.trim();
      if (partial.isNotEmpty) {
        _segments.add(
          ClinicalTranscriptSegment(
            id: 'restored-local-partial-${DateTime.now().microsecondsSinceEpoch}',
            offset: draft.partialOffset,
            text: partial,
          ),
        );
      }

      _currentPartial = '';
      _currentPartialOffset = Duration.zero;
      _currentPartialFinalized = false;
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
      value = value.copyWith(transcriptSyncStatus: TranscriptSyncStatus.failed);
    }
  }

  void _leaveInitializationState() {
    if (value.status == SpeechStatus.processing &&
        value.stage == ClinicalVoiceStage.capture) {
      value = value.copyWith(
        status: value.hasTranscript
            ? SpeechStatus.transcriptReview
            : SpeechStatus.idle,
        clearError: true,
      );
    }
  }

  void _restoreExistingSession(ClinicalAiState existing) {
    final pendingWasProvided = existing.pendingTranscript != null;
    final transcriptWasExplicitlyCleared =
        existing.transcriptStatus.toUpperCase() == 'NONE';
    final transcript = pendingWasProvided
        ? existing.pendingTranscript!.trim()
        : transcriptWasExplicitlyCleared
        ? ''
        : existing.transcript?.trim() ?? '';

    _segments.clear();
    if (transcript.isEmpty) {
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

    _replaceSegmentsWithTranscript(transcript);
    value = value.copyWith(
      status: SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: clinicalTranscriptFromSegments(_segments),
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      transcriptSyncStatus: TranscriptSyncStatus.synced,
      clearPartial: true,
      clearError: true,
    );
  }

  void _replaceSegmentsWithTranscript(String transcript) {
    _segments.clear();
    final parts = RegExp(r'[^.!?\n]+[.!?]?')
        .allMatches(transcript)
        .map((match) => match.group(0)?.trim() ?? '')
        .where((text) => text.isNotEmpty)
        .toList(growable: false);
    final restored = parts.isEmpty ? <String>[transcript.trim()] : parts;
    for (var index = 0; index < restored.length; index++) {
      _segments.add(
        ClinicalTranscriptSegment(
          id: 'restored-$index-${DateTime.now().microsecondsSinceEpoch}',
          offset: Duration(seconds: index),
          text: restored[index],
        ),
      );
    }
    _captureStartedAt ??= DateTime.now();
  }

  Future<void> startRealtimeListening() async {
    if (_disposed || value.status == SpeechStatus.listening) return;
    if (value.stage == ClinicalVoiceStage.review) return;
    await initialize();
    if (!_speechReady || _disposed) return;

    _captureStartedAt ??= DateTime.now();
    _minimumSoundLevel = double.infinity;
    _maximumSoundLevel = -double.infinity;
    _shouldKeepListening = true;
    _resumeAfterLifecycle = false;
    _appActive = true;
    value = value.copyWith(
      status: SpeechStatus.listening,
      stage: ClinicalVoiceStage.capture,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      transcript: clinicalTranscriptFromSegments(_segments),
      soundLevel: 12,
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
          _ingestRecognition(
            result.recognizedWords,
            finalResult: result.finalResult,
          );
        },
        onSoundLevelChange: _publishSoundLevel,
        listenOptions: stt.SpeechListenOptions(
          listenFor: const Duration(hours: 1),
          pauseFor: const Duration(seconds: 4),
          partialResults: true,
          cancelOnError: false,
          listenMode: stt.ListenMode.dictation,
          localeId: _speechLocale,
        ),
      );
    } catch (error) {
      _shouldKeepListening = false;
      _resumeAfterLifecycle = false;
      unawaited(_setAwake(false));
      _setError(
        error,
        fallbackStatus: value.hasTranscript
            ? SpeechStatus.transcriptReview
            : SpeechStatus.idle,
      );
    }
  }

  String get _speechLocale =>
      _locale.toLowerCase().startsWith('en') ? 'en_US' : 'fr_FR';

  void _ingestRecognition(String rawWords, {required bool finalResult}) {
    final committed = clinicalTranscriptFromSegments(_segments);
    final incoming = stripCommittedClinicalTranscriptPrefix(
      rawWords,
      committed,
    ).trim();

    if (incoming.isEmpty) {
      if (finalResult && _currentPartial.isNotEmpty) {
        _currentPartialFinalized = true;
        _publishCaptureState();
        _scheduleDraftSave();
      }
      return;
    }

    if (_currentPartial.isEmpty) {
      _currentPartialOffset = _elapsedCaptureTime();
      _currentPartial = incoming;
    } else {
      final merged = mergeClinicalSpeechHypothesis(
        _currentPartial,
        incoming,
        currentFinalized: _currentPartialFinalized,
      );
      if (merged.startsNewSegment) {
        _commitCurrentPartial();
        _currentPartialOffset = _elapsedCaptureTime();
        _currentPartial = incoming;
      } else {
        _currentPartial = merged.text;
      }
    }

    _currentPartialFinalized = finalResult;
    _publishCaptureState();
    _scheduleDraftSave();
  }

  void _publishSoundLevel(double rawLevel) {
    if (_disposed ||
        value.status != SpeechStatus.listening ||
        !rawLevel.isFinite) {
      return;
    }

    _minimumSoundLevel = math.min(_minimumSoundLevel, rawLevel);
    _maximumSoundLevel = math.max(_maximumSoundLevel, rawLevel);
    final range = math.max(4.0, _maximumSoundLevel - _minimumSoundLevel);
    final normalized = ((rawLevel - _minimumSoundLevel) / range).clamp(
      0.0,
      1.0,
    );
    final boosted = 12.0 + math.pow(normalized, 0.45).toDouble() * 88.0;
    value = value.copyWith(soundLevel: boosted.clamp(12.0, 100.0).toDouble());
  }

  void _handleSpeechStatus(String status) {
    if (_disposed || !_shouldKeepListening) return;
    if (status == 'done' || status == 'notListening') {
      if (_currentPartial.isNotEmpty) {
        _currentPartialFinalized = true;
        _publishCaptureState();
        _scheduleDraftSave();
      }
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
    _restartTimer = Timer(const Duration(milliseconds: 180), () {
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
    final separator = RegExp(r'[.!?;:]$').hasMatch(committed) ? ' ' : '. ';
    final transcript = <String>[
      if (committed.isNotEmpty) committed,
      if (partial.isNotEmpty) partial,
    ].join(separator).trim();

    value = value.copyWith(
      status: _shouldKeepListening
          ? SpeechStatus.listening
          : SpeechStatus.transcriptReview,
      stage: ClinicalVoiceStage.capture,
      transcript: transcript,
      segments: List<ClinicalTranscriptSegment>.unmodifiable(_segments),
      partialTranscript: partial,
      partialOffset: _currentPartialOffset,
      clearError: !value.hasTranscriptSyncFailure,
    );
  }

  void _commitCurrentPartial() {
    var text = _currentPartial.trim();
    if (text.isEmpty) return;

    text = stripCommittedClinicalTranscriptPrefix(
      text,
      clinicalTranscriptFromSegments(_segments),
    ).trim();
    var changed = false;
    if (text.isNotEmpty) {
      if (_segments.isEmpty) {
        _segments.add(
          ClinicalTranscriptSegment(
            id: 'segment-${DateTime.now().microsecondsSinceEpoch}',
            offset: _currentPartialOffset,
            text: text,
          ),
        );
        changed = true;
      } else {
        final last = _segments.last;
        final merged = mergeClinicalSpeechHypothesis(
          last.text,
          text,
          currentFinalized: true,
        );
        if (!merged.startsNewSegment) {
          if (_foldForComparison(last.text) !=
              _foldForComparison(merged.text)) {
            _segments[_segments.length - 1] = last.copyWith(text: merged.text);
            changed = true;
          }
        } else {
          _segments.add(
            ClinicalTranscriptSegment(
              id: 'segment-${DateTime.now().microsecondsSinceEpoch}',
              offset: _currentPartialOffset,
              text: text,
            ),
          );
          changed = true;
        }
      }
    }

    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _currentPartialFinalized = false;
    _publishCaptureState();
    if (changed) _scheduleDraftSave();
  }

  String _foldForComparison(String text) {
    return text
        .toLowerCase()
        .replaceAll(RegExp(r"[^\p{L}\p{N}']+", unicode: true), ' ')
        .trim()
        .replaceAll(RegExp(r'\s+'), ' ');
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
    _localDraftTimer?.cancel();
    _localDraftTimer = Timer(_draftDebounce, () {
      unawaited(_persistCurrentTranscript(silent: true));
    });
  }

  Future<bool> _persistCurrentTranscript({
    bool silent = false,
    bool explicitlyCleared = false,
  }) {
    if (_disposed) return Future<bool>.value(false);
    _localDraftTimer?.cancel();
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
        _hasLocalDraftSnapshot = true;
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
    _localDraftTimer?.cancel();
    await _draftPersistence;
    if (_disposed) return false;
    return _persistCurrentTranscript();
  }

  Future<bool> retryDraftSave() => _persistCurrentTranscript();

  Future<void> stopListening() async {
    if (_disposed || value.status != SpeechStatus.listening) return;
    _shouldKeepListening = false;
    _resumeAfterLifecycle = false;
    _restartTimer?.cancel();
    _localDraftTimer?.cancel();

    if (_speech.isListening) await _speech.stop();
    _commitCurrentPartial();
    await _persistCurrentTranscript();
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
    );
  }

  Future<bool> updateSegment(String segmentId, String text) async {
    if (_disposed || value.status == SpeechStatus.listening) return false;
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
    if (_disposed || value.status == SpeechStatus.listening) return false;
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

  Future<bool> clearTranscript() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (value.status == SpeechStatus.listening) await stopListening();

    final previous = List<ClinicalTranscriptSegment>.from(_segments);
    _segments.clear();
    _currentPartial = '';
    _currentPartialOffset = Duration.zero;
    _currentPartialFinalized = false;
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
      _segments.addAll(previous);
      _publishReviewedTranscript(clearError: false);
    }
    return saved;
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
      transcriptSyncStatus: value.transcriptSyncStatus,
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
    _localDraftTimer?.cancel();
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
    if (!await _ensureTranscriptPersisted()) return;
    if (!await _ensureSessionReady() || _disposed) return;
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
    _localDraftTimer?.cancel();
    if (!_shouldKeepListening) return;

    _resumeAfterLifecycle = true;
    try {
      if (_speech.isListening) await _speech.stop();
    } catch (_) {
      // La plateforme peut déjà avoir arrêté le moteur lors du verrouillage.
    }
    _commitCurrentPartial();
    await _persistCurrentTranscript();
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
    return _ensureTranscriptPersisted();
  }

  Future<bool> discardCurrentCapture() => clearTranscript();
}
