import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;

import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';

enum SpeechStatus {
  idle,
  listening,
  processing,
  transcriptReview,
  proposalReview,
  done,
  error,
}

@immutable
final class RealtimeSpeechState {
  const RealtimeSpeechState({
    required this.status,
    required this.transcript,
    required this.vitals,
    required this.note,
    required this.revisions,
    this.soundLevel = 0.0,
    this.errorMessage,
    this.assistantMessage,
    this.needsClarification = false,
  });

  final SpeechStatus status;
  final String transcript;
  final PatientVitals vitals;
  final ConsultationNote note;
  final List<ClinicalAiRevision> revisions;
  final double soundLevel;
  final String? errorMessage;
  final String? assistantMessage;
  final bool needsClarification;

  bool get hasPendingProposals => revisions.any(
    (revision) => revision.isPending && revision.hasPendingProposals,
  );

  bool get hasApplicableResult => !note.isEmpty || !vitals.isEmpty;

  RealtimeSpeechState copyWith({
    SpeechStatus? status,
    String? transcript,
    PatientVitals? vitals,
    ConsultationNote? note,
    List<ClinicalAiRevision>? revisions,
    double? soundLevel,
    String? errorMessage,
    String? assistantMessage,
    bool? needsClarification,
    bool clearError = false,
  }) {
    return RealtimeSpeechState(
      status: status ?? this.status,
      transcript: transcript ?? this.transcript,
      vitals: vitals ?? this.vitals,
      note: note ?? this.note,
      revisions: revisions ?? this.revisions,
      soundLevel: soundLevel ?? this.soundLevel,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      assistantMessage: assistantMessage ?? this.assistantMessage,
      needsClarification: needsClarification ?? this.needsClarification,
    );
  }
}

/// Captation clinique instantanée.
///
/// La reconnaissance native fournit les mots partiels immédiatement. Chaque
/// segment final est envoyé au moteur IA en arrière-plan afin d'en extraire et
/// reformuler progressivement les éléments cliniques, sans interrompre l'écoute.
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
    // recorder et temporaryDirectoryProvider restent acceptés pour préserver le
    // contrat d'injection existant pendant la migration du pipeline différé.
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
           transcript: '',
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

  final List<String> _committedTurns = <String>[];
  String _currentPartial = '';
  bool _sessionReady = false;
  bool _speechReady = false;
  bool _shouldKeepListening = false;
  bool _disposed = false;
  Timer? _restartTimer;
  Future<void> _analysisChain = Future<void>.value();

  Future<void> restoreOrStart() async {
    await initialize();
    if (!_disposed && value.status == SpeechStatus.idle) {
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
          await _gateway.startSession(
            _visitId,
            _initialDraft,
            locale: _locale,
          );
        } else if (existing.hasAcceptedChanges ||
            !existing.noteFrom().isEmpty ||
            !existing.vitalsFrom().isEmpty ||
            existing.revisions.isNotEmpty) {
          _applyAiState(existing, statusOverride: SpeechStatus.idle);
        }
        _sessionReady = true;
      } catch (error) {
        _setError(error);
        return;
      }
    }

    if (_speechReady) {
      if (value.status == SpeechStatus.processing) {
        value = value.copyWith(status: SpeechStatus.idle, clearError: true);
      }
      return;
    }

    try {
      _speechReady = await _speech.initialize(
        onStatus: (status) {
          if (_disposed || !_shouldKeepListening) return;
          if (status == 'done' || status == 'notListening') {
            _commitCurrentPartial(analyze: true);
            _restartListeningLoop();
          }
        },
        onError: (error) {
          if (_disposed) return;
          if (_shouldKeepListening) {
            value = value.copyWith(errorMessage: error.errorMsg);
            _restartListeningLoop();
          } else {
            _setError(error.errorMsg);
          }
        },
      );
      if (!_speechReady) {
        throw StateError('SPEECH_RECOGNITION_UNAVAILABLE');
      }
      if (value.status == SpeechStatus.processing) {
        value = value.copyWith(status: SpeechStatus.idle, clearError: true);
      }
    } catch (error) {
      _speechReady = false;
      _setError(error);
    }
  }

  Future<void> startRealtimeListening() async {
    if (_disposed || value.status == SpeechStatus.listening) return;
    await initialize();
    if (!_sessionReady || !_speechReady || _disposed) return;

    _shouldKeepListening = true;
    _committedTurns.clear();
    _currentPartial = '';
    value = value.copyWith(
      status: SpeechStatus.listening,
      transcript: '',
      soundLevel: 8,
      clearError: true,
    );
    await _listenInternal();
  }

  Future<void> _listenInternal() async {
    if (_disposed || !_shouldKeepListening || !_speechReady) return;
    if (_speech.isListening) return;

    try {
      await _speech.listen(
        onResult: (result) {
          if (_disposed || !_shouldKeepListening) return;
          _currentPartial = result.recognizedWords.trim();
          _publishInstantTranscript();
          if (result.finalResult) {
            _commitCurrentPartial(analyze: true);
          }
        },
        onSoundLevelChange: (level) {
          if (_disposed || value.status != SpeechStatus.listening) return;
          final normalized = (level + 2.0).clamp(5.0, 60.0).toDouble();
          value = value.copyWith(soundLevel: normalized);
        },
        listenFor: const Duration(hours: 1),
        pauseFor: const Duration(seconds: 8),
        partialResults: true,
        cancelOnError: false,
        listenMode: stt.ListenMode.dictation,
        localeId: _speechLocale,
      );
    } catch (error) {
      if (_shouldKeepListening) {
        value = value.copyWith(errorMessage: error.toString());
        _restartListeningLoop();
      } else {
        _setError(error);
      }
    }
  }

  String get _speechLocale {
    final normalized = _locale.toLowerCase();
    return normalized.startsWith('en') ? 'en_US' : 'fr_FR';
  }

  void _restartListeningLoop() {
    _restartTimer?.cancel();
    _restartTimer = Timer(const Duration(milliseconds: 180), () {
      if (_shouldKeepListening && !_disposed) {
        unawaited(_listenInternal());
      }
    });
  }

  void _publishInstantTranscript() {
    final parts = List<String>.from(_committedTurns);
    final partial = _currentPartial.trim();
    if (partial.isNotEmpty) {
      if (parts.isNotEmpty && partial.startsWith(parts.last)) {
        parts.removeLast();
      }
      if (!parts.contains(partial)) parts.add(partial);
    }
    final transcript = parts.join('. ').trim();
    final parsed = _parser.parse(transcript);
    value = value.copyWith(
      status: SpeechStatus.listening,
      transcript: transcript,
      vitals: parsed.vitals.mergePrefer(value.vitals),
      soundLevel: value.soundLevel,
      clearError: true,
    );
  }

  void _commitCurrentPartial({required bool analyze}) {
    final text = _currentPartial.trim();
    if (text.isEmpty) return;

    if (_committedTurns.isNotEmpty && text.startsWith(_committedTurns.last)) {
      _committedTurns.removeLast();
    }
    if (!_committedTurns.contains(text)) {
      _committedTurns.add(text);
      if (analyze) _enqueueProgressiveAnalysis(text);
    }
    _currentPartial = '';
    _publishInstantTranscript();
  }

  void _enqueueProgressiveAnalysis(String segment) {
    if (segment.trim().isEmpty || _disposed) return;
    _analysisChain = _analysisChain.then((_) async {
      if (_disposed) return;
      try {
        final state = await _gateway.analyzeTranscript(_visitId, segment);
        if (_disposed) return;
        _applyAiState(
          state,
          statusOverride: _shouldKeepListening
              ? SpeechStatus.listening
              : state.hasPendingProposals
              ? SpeechStatus.proposalReview
              : SpeechStatus.transcriptReview,
        );
      } catch (error) {
        if (!_disposed) {
          value = value.copyWith(errorMessage: error.toString());
        }
      }
    });
  }

  Future<void> stopListening() async {
    if (_disposed || value.status != SpeechStatus.listening) return;
    _shouldKeepListening = false;
    _restartTimer?.cancel();

    if (_speech.isListening) await _speech.stop();
    _commitCurrentPartial(analyze: true);
    await _analysisChain;

    value = value.copyWith(
      status: value.hasPendingProposals
          ? SpeechStatus.proposalReview
          : SpeechStatus.transcriptReview,
      soundLevel: 0,
      clearError: true,
    );
  }

  void updateTranscript(String text) {
    final normalized = text.trim();
    final parsed = _parser.parse(normalized);
    value = value.copyWith(
      transcript: normalized,
      vitals: parsed.vitals.mergePrefer(value.vitals),
      status: SpeechStatus.transcriptReview,
      clearError: true,
    );
  }

  Future<void> analyzeTranscript() async {
    final transcript = value.transcript.trim();
    if (_disposed || transcript.isEmpty) return;

    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
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

  void _applyAiState(
    ClinicalAiState state, {
    SpeechStatus? statusOverride,
  }) {
    final aiVitals = state.vitalsFrom(includePending: true);
    final explicitVitals = _parser.parse(value.transcript).vitals;
    final resolvedVitals = explicitVitals.mergePrefer(aiVitals);
    value = RealtimeSpeechState(
      status:
          statusOverride ??
          (state.hasPendingProposals
              ? SpeechStatus.proposalReview
              : SpeechStatus.done),
      transcript: value.transcript,
      vitals: resolvedVitals,
      note: state.noteFrom(includePending: true),
      revisions: state.revisions,
      soundLevel: _shouldKeepListening ? value.soundLevel : 0,
      assistantMessage: state.assistantMessage,
      needsClarification: state.needsClarification,
      errorMessage: value.errorMessage,
    );
  }

  Future<bool> prepareForClose() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (value.status == SpeechStatus.listening) await stopListening();
    return true;
  }

  Future<void> discardCurrentCapture() async {
    if (_disposed) return;
    _shouldKeepListening = false;
    _restartTimer?.cancel();
    if (_speech.isListening) await _speech.cancel();
    _committedTurns.clear();
    _currentPartial = '';
    try {
      await _gateway.discardPendingTranscript(_visitId);
    } catch (_) {
      // Le flux realtime n'a pas toujours de transcription serveur en attente.
    }
    value = const RealtimeSpeechState(
      status: SpeechStatus.idle,
      transcript: '',
      vitals: PatientVitals(),
      note: ConsultationNote(),
      revisions: <ClinicalAiRevision>[],
    );
  }

  void _setError(
    Object error, {
    SpeechStatus fallbackStatus = SpeechStatus.error,
  }) {
    if (_disposed) return;
    value = value.copyWith(
      status: fallbackStatus,
      soundLevel: 0,
      errorMessage: error.toString(),
    );
  }

  @override
  void dispose() {
    _disposed = true;
    _shouldKeepListening = false;
    _restartTimer?.cancel();
    unawaited(_speech.cancel());
    super.dispose();
  }
}
