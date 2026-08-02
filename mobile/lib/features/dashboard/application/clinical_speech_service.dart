import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

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

/// Capture vocale clinique mobile stabilisée.
///
/// Cette version utilise le moteur `record` validé sur Android. Elle ne lance
/// aucun transport WebRTC mobile. Le WAV est transcrit côté serveur, relu par
/// le praticien puis analysé, sans application automatique au formulaire.
class ClinicalSpeechService extends ValueNotifier<RealtimeSpeechState> {
  factory ClinicalSpeechService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    AudioRecorder? recorder,
    Future<Directory> Function()? temporaryDirectoryProvider,
  }) {
    return ClinicalSpeechService._(
      gateway,
      visitId,
      initialDraft,
      locale,
      recorder: recorder,
      temporaryDirectoryProvider: temporaryDirectoryProvider,
    );
  }

  ClinicalSpeechService._(
    this._gateway,
    this._visitId,
    Map<String, String> initialDraft,
    this._locale, {
    AudioRecorder? recorder,
    Future<Directory> Function()? temporaryDirectoryProvider,
  }) : _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _recorder = recorder ?? AudioRecorder(),
       _temporaryDirectoryProvider =
           temporaryDirectoryProvider ?? getTemporaryDirectory,
       super(
         const RealtimeSpeechState(
           status: SpeechStatus.idle,
           transcript: '',
           vitals: PatientVitals(),
           note: ConsultationNote(),
           revisions: <ClinicalAiRevision>[],
         ),
       );

  static const double _voiceFrameThresholdDb = -48;
  static const int _minimumVoiceFrames = 3;
  static const Duration _minimumRecordingDuration = Duration(milliseconds: 450);

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final AudioRecorder _recorder;
  final Future<Directory> Function() _temporaryDirectoryProvider;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();

  StreamSubscription<Amplitude>? _amplitudeSubscription;
  String? _recordingPath;
  DateTime? _recordingStartedAt;
  int _activeVoiceFrames = 0;
  bool _sessionReady = false;
  bool _serverHasPendingTranscript = false;
  bool _disposed = false;

  bool get _isFrench => _locale != 'en';

  /// Ouvre/restaure la session sans démarrer automatiquement le microphone.
  /// L'enregistrement ne commence qu'après l'action explicite sur « Démarrer ».
  Future<void> restoreOrStart() async {
    await initialize();
  }

  Future<void> initialize() async {
    if (_sessionReady || _disposed) return;
    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      final existing = await _gateway.getSession(_visitId);
      if (existing != null) {
        _sessionReady = true;
        final pending = existing.pendingTranscript?.trim();
        if (pending != null && pending.isNotEmpty) {
          _serverHasPendingTranscript = true;
          final parsed = _parser.parse(pending);
          value = RealtimeSpeechState(
            status: SpeechStatus.transcriptReview,
            transcript: pending,
            vitals: parsed.vitals,
            note: const ConsultationNote(),
            revisions: existing.revisions,
            assistantMessage: _isFrench
                ? 'Une transcription précédente a été restaurée. Relisez-la ou supprimez-la avant de recommencer.'
                : 'A previous transcript was restored. Review or delete it before recording again.',
          );
          return;
        }
        if (existing.hasAcceptedChanges ||
            !existing.noteFrom().isEmpty ||
            !existing.vitalsFrom().isEmpty) {
          _applyAiState(existing, includePending: false);
          return;
        }
        value = value.copyWith(status: SpeechStatus.idle, clearError: true);
        return;
      }

      await _gateway.startSession(_visitId, _initialDraft, locale: _locale);
      _sessionReady = true;
      value = value.copyWith(status: SpeechStatus.idle, clearError: true);
    } catch (error) {
      _setError(error);
    }
  }

  Future<void> startRealtimeListening() async {
    if (_disposed || value.status == SpeechStatus.listening) return;
    await initialize();
    if (!_sessionReady || _disposed) return;
    if (_serverHasPendingTranscript) {
      _setError(
        StateError('AI_TRANSCRIPT_REVIEW_REQUIRED'),
        fallbackStatus: SpeechStatus.transcriptReview,
      );
      return;
    }

    try {
      if (_recordingPath != null) await _cleanupRecording();
      final hasPermission = await _recorder.hasPermission();
      if (!hasPermission) throw StateError('MICROPHONE_PERMISSION_DENIED');

      final directory = await _temporaryDirectoryProvider();
      final path =
          '${directory.path}/joprelys-$_visitId-${DateTime.now().microsecondsSinceEpoch}.wav';
      _recordingPath = path;
      _recordingStartedAt = DateTime.now();
      _activeVoiceFrames = 0;

      await _recorder.start(
        const RecordConfig(
          encoder: AudioEncoder.wav,
          sampleRate: 16000,
          numChannels: 1,
          autoGain: true,
          noiseSuppress: true,
        ),
        path: path,
      );

      await _amplitudeSubscription?.cancel();
      _amplitudeSubscription = _recorder
          .onAmplitudeChanged(const Duration(milliseconds: 100))
          .listen((amplitude) {
            if (_disposed || value.status != SpeechStatus.listening) return;
            if (amplitude.current >= _voiceFrameThresholdDb) {
              _activeVoiceFrames++;
            }
            final normalized = (((amplitude.current + 60) / 60) * 55 + 5)
                .clamp(5.0, 60.0)
                .toDouble();
            value = value.copyWith(soundLevel: normalized);
          });

      value = const RealtimeSpeechState(
        status: SpeechStatus.listening,
        transcript: '',
        vitals: PatientVitals(),
        note: ConsultationNote(),
        revisions: <ClinicalAiRevision>[],
        soundLevel: 8,
      );
    } catch (error) {
      await _safeCancelRecorder();
      _setError(error, fallbackStatus: SpeechStatus.idle);
    }
  }

  Future<void> stopListening() async {
    if (_disposed || value.status != SpeechStatus.listening) return;
    value = value.copyWith(
      status: SpeechStatus.processing,
      soundLevel: 0,
      clearError: true,
    );

    try {
      await _amplitudeSubscription?.cancel();
      _amplitudeSubscription = null;
      final path = await _recorder.stop() ?? _recordingPath;
      if (path == null || path.isEmpty) {
        throw StateError('AUDIO_RECORDING_EMPTY');
      }

      final duration = DateTime.now().difference(
        _recordingStartedAt ?? DateTime.now(),
      );
      if (_activeVoiceFrames < _minimumVoiceFrames ||
          duration < _minimumRecordingDuration) {
        await _cleanupRecording();
        _setError(
          StateError('AI_AUDIO_SILENCE'),
          fallbackStatus: SpeechStatus.idle,
        );
        return;
      }

      final file = File(path);
      final bytes = await file.readAsBytes();
      if (bytes.length < 44) throw StateError('AUDIO_RECORDING_EMPTY');

      final transcript = await _gateway.transcribeAudio(_visitId, bytes);
      if (await file.exists()) await file.delete();
      _recordingPath = null;
      _serverHasPendingTranscript = true;
      final parsed = _parser.parse(transcript);
      value = RealtimeSpeechState(
        status: SpeechStatus.transcriptReview,
        transcript: transcript,
        vitals: parsed.vitals,
        note: const ConsultationNote(),
        revisions: const <ClinicalAiRevision>[],
      );
    } catch (error) {
      _setError(error);
    } finally {
      _recordingStartedAt = null;
      _activeVoiceFrames = 0;
    }
  }

  void updateTranscript(String text) {
    final parsed = _parser.parse(text);
    value = value.copyWith(
      transcript: text,
      vitals: parsed.vitals,
      status: SpeechStatus.transcriptReview,
      clearError: true,
    );
  }

  Future<void> analyzeTranscript() async {
    final transcript = value.transcript.trim();
    if (_disposed || transcript.isEmpty || !_serverHasPendingTranscript) return;

    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      final state = await _gateway.analyzeTranscript(_visitId, transcript);
      await _gateway.discardPendingTranscript(_visitId);
      _serverHasPendingTranscript = false;
      _applyAiState(state, includePending: true);
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
      _applyAiState(state, includePending: true);
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
      _applyAiState(state, includePending: true);
    } catch (error) {
      _setError(error, fallbackStatus: SpeechStatus.proposalReview);
    }
  }

  void _applyAiState(ClinicalAiState state, {required bool includePending}) {
    final aiVitals = state.vitalsFrom(includePending: includePending);
    final explicitVitals = _parser.parse(value.transcript).vitals;
    final resolvedVitals = explicitVitals.mergePrefer(aiVitals);
    final hasPending = state.hasPendingProposals;
    value = RealtimeSpeechState(
      status: hasPending ? SpeechStatus.proposalReview : SpeechStatus.done,
      transcript: state.transcript?.trim().isNotEmpty == true
          ? state.transcript!.trim()
          : value.transcript,
      vitals: resolvedVitals,
      note: state.noteFrom(includePending: includePending),
      revisions: state.revisions,
      assistantMessage: state.assistantMessage,
      needsClarification: false,
    );
  }

  Future<bool> prepareForClose() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (value.status == SpeechStatus.listening) await stopListening();
    if (_recordingPath != null && value.status == SpeechStatus.error) {
      return false;
    }
    return true;
  }

  Future<void> discardCurrentCapture() async {
    if (_disposed || value.status == SpeechStatus.processing) return;
    final previous = value;
    value = value.copyWith(
      status: SpeechStatus.processing,
      soundLevel: 0,
      clearError: true,
      assistantMessage: _isFrench
          ? 'Suppression de la transcription…'
          : 'Deleting the transcript…',
    );

    Object? deletionError;
    try {
      await _safeCancelRecorder();
      await _cleanupRecording();
    } catch (error) {
      deletionError = error;
    }
    try {
      await _gateway.clearLiveTranscript(_visitId);
    } catch (error) {
      deletionError ??= error;
    }
    try {
      await _gateway.discardPendingTranscript(_visitId);
    } catch (error) {
      deletionError ??= error;
    }

    if (deletionError != null) {
      value = previous.copyWith(
        status: SpeechStatus.transcriptReview,
        errorMessage: _isFrench
            ? 'La transcription n’a pas pu être supprimée du serveur. Réessayez.'
            : 'The transcript could not be deleted from the server. Try again.',
      );
      return;
    }

    _serverHasPendingTranscript = false;
    value = const RealtimeSpeechState(
      status: SpeechStatus.idle,
      transcript: '',
      vitals: PatientVitals(),
      note: ConsultationNote(),
      revisions: <ClinicalAiRevision>[],
    );
  }

  Future<void> _safeCancelRecorder() async {
    await _amplitudeSubscription?.cancel();
    _amplitudeSubscription = null;
    try {
      await _recorder.cancel();
    } catch (_) {
      // Annulation best effort : aucune donnée clinique n'est appliquée.
    }
  }

  Future<void> _cleanupRecording() async {
    final path = _recordingPath;
    _recordingPath = null;
    if (path == null || path.isEmpty) return;
    final file = File(path);
    if (await file.exists()) await file.delete();
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
    unawaited(_amplitudeSubscription?.cancel());
    unawaited(_recorder.cancel());
    unawaited(_recorder.dispose());
    unawaited(_cleanupRecording());
    super.dispose();
  }
}
