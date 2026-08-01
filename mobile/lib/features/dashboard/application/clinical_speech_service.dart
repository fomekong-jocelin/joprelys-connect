import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../core/network/api_exception.dart';
import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';
import 'clinical_realtime_transcription_bridge.dart';

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

/// Capture vocale clinique mobile sécurisée et progressive.
///
/// Le microphone est transmis par WebRTC. Les tours finalisés sont affichés puis
/// persistés immédiatement dans Joprelys. La génération clinique ne commence
/// qu'après relecture explicite du transcript consolidé.
class ClinicalSpeechService extends ValueNotifier<RealtimeSpeechState> {
  factory ClinicalSpeechService({
    required ClinicalVoiceAiGateway gateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
    ClinicalRealtimeTranscriptionTransport? transport,
  }) {
    return ClinicalSpeechService._(
      gateway,
      visitId,
      initialDraft,
      locale,
      transport: transport,
    );
  }

  ClinicalSpeechService._(
    this._gateway,
    this._visitId,
    Map<String, String> initialDraft,
    this._locale, {
    ClinicalRealtimeTranscriptionTransport? transport,
  }) : _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _transport =
           transport ?? WebRtcClinicalRealtimeTranscriptionTransport(_gateway),
       super(
         const RealtimeSpeechState(
           status: SpeechStatus.idle,
           transcript: '',
           vitals: PatientVitals(),
           note: ConsultationNote(),
           revisions: <ClinicalAiRevision>[],
         ),
       ) {
    _transportSubscription = _transport.events.listen(_handleRealtimeEvent);
  }

  final ClinicalVoiceAiGateway _gateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final ClinicalRealtimeTranscriptionTransport _transport;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();

  late final StreamSubscription<ClinicalRealtimeEvent> _transportSubscription;
  Future<void> _persistenceTail = Future<void>.value();
  String _confirmedTranscript = '';
  String _partialTranscript = '';
  int _liveSequence = 0;
  final Set<String> _completedTurnIds = <String>{};
  Object? _lastPersistenceError;
  bool _sessionReady = false;
  bool _serverHasPendingTranscript = false;
  bool _disposed = false;

  bool get _isFrench => _locale != 'en';

  Future<void> restoreOrStart() async {
    await initialize();
    if (!_disposed && value.status == SpeechStatus.idle) {
      await startRealtimeListening();
    }
  }

  Future<void> initialize() async {
    if (_sessionReady || _disposed) return;
    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      var existing = await _gateway.getSession(_visitId);
      if (existing == null) {
        existing = await _gateway.startSession(
          _visitId,
          _initialDraft,
          locale: _locale,
        );
      }
      _sessionReady = true;

      final pending = existing.pendingTranscript?.trim();
      if (pending != null && pending.isNotEmpty) {
        _restoreTranscript(pending, sequence: 0, serverPending: true);
        return;
      }

      final live = await _gateway.getLiveTranscript(_visitId);
      if (live != null && live.transcript.isNotEmpty) {
        _restoreTranscript(
          live.transcript,
          sequence: live.sequence,
          serverPending: false,
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
    } catch (error) {
      _setError(error);
    }
  }

  void _restoreTranscript(
    String transcript, {
    required int sequence,
    required bool serverPending,
  }) {
    _confirmedTranscript = transcript.trim();
    _partialTranscript = '';
    _liveSequence = sequence;
    _serverHasPendingTranscript = serverPending;
    final parsed = _parser.parse(_confirmedTranscript);
    value = RealtimeSpeechState(
      status: SpeechStatus.transcriptReview,
      transcript: _confirmedTranscript,
      vitals: parsed.vitals,
      note: const ConsultationNote(),
      revisions: const <ClinicalAiRevision>[],
      assistantMessage: _isFrench
          ? 'La dictée précédente a été restaurée. Relisez-la ou reprenez l’enregistrement.'
          : 'The previous dictation was restored. Review it or resume recording.',
    );
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

    value = value.copyWith(
      status: SpeechStatus.processing,
      soundLevel: 0,
      clearError: true,
      assistantMessage: _isFrench
          ? 'Connexion au microphone temps réel…'
          : 'Connecting the live microphone…',
    );
    try {
      await _transport.connect(visitId: _visitId, locale: _locale);
      value = value.copyWith(
        status: SpeechStatus.listening,
        soundLevel: 8,
        clearError: true,
        assistantMessage: _isFrench
            ? 'Parlez normalement. La transcription apparaît au fil de la consultation.'
            : 'Speak normally. The transcript appears during the consultation.',
      );
    } catch (error) {
      _setError(error);
    }
  }

  void _handleRealtimeEvent(ClinicalRealtimeEvent event) {
    if (_disposed) return;
    switch (event.type) {
      case ClinicalRealtimeEventType.connected:
        if (value.status == SpeechStatus.processing ||
            value.status == SpeechStatus.listening) {
          value = value.copyWith(
            status: SpeechStatus.listening,
            clearError: true,
          );
        }
        break;
      case ClinicalRealtimeEventType.speechStarted:
        _partialTranscript = '';
        value = value.copyWith(soundLevel: 52, clearError: true);
        break;
      case ClinicalRealtimeEventType.speechStopped:
        value = value.copyWith(soundLevel: 12);
        break;
      case ClinicalRealtimeEventType.transcriptDelta:
        final delta = event.text;
        if (delta == null || delta.isEmpty) return;
        _partialTranscript += delta;
        _publishTranscript(
          _joinTranscript(_confirmedTranscript, _partialTranscript),
          soundLevel: 40,
        );
        break;
      case ClinicalRealtimeEventType.transcriptCompleted:
        _acceptCompletedTurn(event);
        break;
      case ClinicalRealtimeEventType.reconnecting:
        value = value.copyWith(
          status: SpeechStatus.listening,
          soundLevel: 5,
          assistantMessage: _isFrench
              ? 'Connexion instable. Reconnexion automatique en cours…'
              : 'Connection unstable. Reconnecting automatically…',
        );
        break;
      case ClinicalRealtimeEventType.error:
        value = value.copyWith(
          errorMessage: _friendlyError(
            event.error ?? StateError('AI_REALTIME_ERROR'),
          ),
          soundLevel: 5,
        );
        break;
    }
  }

  void _acceptCompletedTurn(ClinicalRealtimeEvent event) {
    final transcript = event.text?.trim();
    if (transcript == null || transcript.isEmpty) return;
    final turnId = _safeEventId(event, transcript);
    if (!_completedTurnIds.add(turnId)) return;

    _confirmedTranscript = _joinTranscript(_confirmedTranscript, transcript);
    _partialTranscript = '';
    _liveSequence++;
    _publishTranscript(_confirmedTranscript, soundLevel: 10);
    _queueLivePersistence(turnId);
  }

  String _safeEventId(ClinicalRealtimeEvent event, String transcript) {
    final candidate = event.itemId?.trim().isNotEmpty == true
        ? event.itemId!.trim()
        : event.eventId?.trim().isNotEmpty == true
        ? event.eventId!.trim()
        : 'mobile-${_liveSequence + 1}-${transcript.hashCode.abs()}';
    return candidate.replaceAll(RegExp(r'[^A-Za-z0-9._:-]'), '_');
  }

  String _joinTranscript(String confirmed, String addition) {
    final left = confirmed.trim();
    final right = addition.trim();
    if (left.isEmpty) return right;
    if (right.isEmpty) return left;
    return '$left\n$right';
  }

  void _publishTranscript(String transcript, {required double soundLevel}) {
    final parsed = _parser.parse(transcript);
    value = value.copyWith(
      status: SpeechStatus.listening,
      transcript: transcript,
      vitals: parsed.vitals,
      soundLevel: soundLevel,
      clearError: true,
    );
  }

  void _queueLivePersistence(String eventId) {
    final transcript = _confirmedTranscript;
    final sequence = _liveSequence;
    _persistenceTail = _persistenceTail.then((_) async {
      try {
        await _gateway.upsertLiveTranscript(
          _visitId,
          transcript: transcript,
          sequence: sequence,
          eventId: eventId,
        );
        _lastPersistenceError = null;
      } catch (error) {
        _lastPersistenceError = error;
      }
    });
  }

  Future<void> stopListening() async {
    if (_disposed || value.status != SpeechStatus.listening) return;
    value = value.copyWith(
      status: SpeechStatus.processing,
      soundLevel: 0,
      clearError: true,
      assistantMessage: _isFrench
          ? 'Finalisation du dernier passage…'
          : 'Finalizing the last passage…',
    );

    try {
      await _transport.finalize();
      await Future<void>.delayed(Duration.zero);
      await _persistenceTail;

      final transcript = _confirmedTranscript.trim();
      if (transcript.isEmpty) {
        _setError(
          StateError('AI_AUDIO_SILENCE'),
          fallbackStatus: SpeechStatus.idle,
        );
        return;
      }

      await _gateway.stageRealtimeTranscript(_visitId, transcript);
      _serverHasPendingTranscript = true;
      await _gateway.clearLiveTranscript(_visitId);
      _lastPersistenceError = null;

      final parsed = _parser.parse(transcript);
      value = RealtimeSpeechState(
        status: SpeechStatus.transcriptReview,
        transcript: transcript,
        vitals: parsed.vitals,
        note: const ConsultationNote(),
        revisions: const <ClinicalAiRevision>[],
        assistantMessage: _isFrench
            ? 'Transcription finalisée. Relisez-la avant de générer le compte rendu.'
            : 'Transcript finalized. Review it before generating the clinical note.',
      );
    } catch (error) {
      final fallback = _confirmedTranscript.trim().isEmpty
          ? SpeechStatus.error
          : SpeechStatus.transcriptReview;
      _setError(error, fallbackStatus: fallback);
    }
  }

  void updateTranscript(String text) {
    _confirmedTranscript = text.trim();
    _partialTranscript = '';
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
    if (_disposed || transcript.isEmpty) return;

    value = value.copyWith(status: SpeechStatus.processing, clearError: true);
    try {
      if (!_serverHasPendingTranscript) {
        await _gateway.stageRealtimeTranscript(_visitId, transcript);
        _serverHasPendingTranscript = true;
        await _gateway.clearLiveTranscript(_visitId);
      }
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

  /// Une fermeture pendant l'écoute finalise le tour courant et conserve le
  /// transcript côté serveur avant de laisser la feuille disparaître.
  Future<bool> prepareForClose() async {
    if (_disposed || value.status == SpeechStatus.processing) return false;
    if (value.status == SpeechStatus.listening) await stopListening();
    if (_lastPersistenceError != null && _confirmedTranscript.isNotEmpty) {
      return false;
    }
    return value.status != SpeechStatus.processing;
  }

  /// Action destructive explicite. Aucune fermeture normale ne l'appelle.
  Future<void> discardCurrentCapture() async {
    if (_disposed) return;
    try {
      await _transport.disconnect();
      await _gateway.clearLiveTranscript(_visitId);
      if (_serverHasPendingTranscript) {
        await _gateway.discardPendingTranscript(_visitId);
      }
    } catch (_) {
      // La suppression explicite reste best effort et n'applique aucune donnée.
    }
    _serverHasPendingTranscript = false;
    _confirmedTranscript = '';
    _partialTranscript = '';
    _liveSequence = 0;
    _completedTurnIds.clear();
    _lastPersistenceError = null;
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
      errorMessage: _friendlyError(error),
    );
  }

  String _friendlyError(Object error) {
    final code = switch (error) {
      ApiException exception => exception.code,
      StateError state => state.message.toString(),
      TimeoutException timeout => timeout.message ?? 'AI_REALTIME_TIMEOUT',
      _ => error.toString(),
    };
    if (code.contains('AI_AUDIO_SILENCE')) {
      return _isFrench
          ? 'Aucune parole suffisamment claire n’a été détectée. Reprenez l’enregistrement.'
          : 'No sufficiently clear speech was detected. Start recording again.';
    }
    if (code.contains('AI_TRANSCRIPT_REVIEW_REQUIRED')) {
      return _isFrench
          ? 'Une transcription est déjà en attente. Relisez-la avant de reprendre le micro.'
          : 'A transcript is already awaiting review. Review it before recording again.';
    }
    if (code.contains('MICROPHONE') || code.contains('PERMISSION')) {
      return _isFrench
          ? 'Le microphone n’est pas disponible. Vérifiez son autorisation puis réessayez.'
          : 'The microphone is unavailable. Check its permission and try again.';
    }
    if (code.contains('AI_AUDIO_TOO_LARGE')) {
      return _isFrench
          ? 'Cette ancienne capture audio est trop volumineuse. Reprenez-la avec le mode temps réel.'
          : 'This legacy audio capture is too large. Record it again using live mode.';
    }
    if (code.contains('AI_REALTIME') ||
        code.contains('CONNECTION') ||
        code.contains('TIMEOUT')) {
      return _isFrench
          ? 'La liaison de transcription temps réel est indisponible. Votre texte déjà reçu reste conservé ; réessayez.'
          : 'The live transcription link is unavailable. Text already received is preserved; try again.';
    }
    return _isFrench
        ? 'La dictée n’a pas pu être traitée. Votre transcription déjà reçue reste conservée.'
        : 'The dictation could not be processed. Text already received remains preserved.';
  }

  @override
  void dispose() {
    _disposed = true;
    unawaited(_transportSubscription.cancel());
    unawaited(_transport.dispose());
    super.dispose();
  }
}
