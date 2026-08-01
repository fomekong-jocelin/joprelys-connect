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

/// Capture vocale clinique mobile sécurisée.
///
/// Le téléphone enregistre un fichier WAV, le backend produit la transcription,
/// le praticien peut la corriger, puis l'IA génère uniquement des propositions.
/// Aucune donnée n'est appliquée au formulaire depuis ce service.
class ClinicalSpeechService extends ValueNotifier<RealtimeSpeechState> {
  ClinicalSpeechService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    AudioRecorder? recorder,
    Future<Directory> Function()? temporaryDirectoryProvider,
  }) : _gateway = gateway,
       _visitId = visitId,
       _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _locale = locale,
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

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final AudioRecorder _recorder;
  final Future<Directory> Function() _temporaryDirectoryProvider;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();

  StreamSubscription<Amplitude>? _amplitudeSubscription;
  ClinicalAiState? _aiState;
  String? _recordingPath;
  bool _sessionReady = false;
  bool _serverHasPendingTranscript = false;
  bool _disposed = false;

  Future<void> initialize() async {
    if (_sessionReady || _disposed) return;
    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      _aiState = await _gateway.startSession(
        _visitId,
        _initialDraft,
        locale: _locale,
      );
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

    try {
      if (_serverHasPendingTranscript) {
        await _gateway.discardPendingTranscript(_visitId);
        _serverHasPendingTranscript = false;
      }

      final hasPermission = await _recorder.hasPermission();
      if (!hasPermission) {
        throw StateError('MICROPHONE_PERMISSION_DENIED');
      }

      final directory = await _temporaryDirectoryProvider();
      final path =
          '${directory.path}/joprelys-${_visitId}-${DateTime.now().microsecondsSinceEpoch}.wav';
      _recordingPath = path;

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
            final normalized = (((amplitude.current + 60) / 60) * 55 + 5)
                .clamp(5.0, 60.0)
                .toDouble();
            value = value.copyWith(soundLevel: normalized);
          });

      _aiState = null;
      value = const RealtimeSpeechState(
        status: SpeechStatus.listening,
        transcript: '',
        vitals: PatientVitals(),
        note: ConsultationNote(),
        revisions: <ClinicalAiRevision>[],
        soundLevel: 8,
      );
    } catch (error) {
      _setError(error);
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
      _recordingPath = null;
      if (path == null || path.isEmpty) {
        throw StateError('AUDIO_RECORDING_EMPTY');
      }

      final file = File(path);
      final bytes = await file.readAsBytes();
      if (await file.exists()) {
        await file.delete();
      }
      if (bytes.length < 44) {
        throw StateError('AUDIO_RECORDING_EMPTY');
      }

      final transcript = await _gateway.transcribeAudio(_visitId, bytes);
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
      await _cleanupRecording();
      _setError(error);
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
    _aiState = state;
    final aiVitals = state.vitalsFrom(includePending: includePending);
    final explicitVitals = _parser.parse(value.transcript).vitals;
    final resolvedVitals = aiVitals.isEmpty ? explicitVitals : aiVitals;
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
      needsClarification: state.needsClarification,
    );
  }

  Future<void> cancelListening() async {
    if (_disposed) return;
    try {
      await _amplitudeSubscription?.cancel();
      _amplitudeSubscription = null;
      await _recorder.cancel();
      await _cleanupRecording();
      if (_serverHasPendingTranscript) {
        await _gateway.discardPendingTranscript(_visitId);
        _serverHasPendingTranscript = false;
      }
    } catch (_) {
      // Best effort cleanup: cancellation must never apply clinical data.
    }
    _aiState = null;
    value = const RealtimeSpeechState(
      status: SpeechStatus.idle,
      transcript: '',
      vitals: PatientVitals(),
      note: ConsultationNote(),
      revisions: <ClinicalAiRevision>[],
    );
  }

  Future<void> _cleanupRecording() async {
    final path = _recordingPath;
    _recordingPath = null;
    if (path == null || path.isEmpty) return;
    final file = File(path);
    if (await file.exists()) {
      await file.delete();
    }
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
    _amplitudeSubscription?.cancel();
    _recorder.cancel();
    _recorder.dispose();
    _cleanupRecording();
    super.dispose();
  }
}
