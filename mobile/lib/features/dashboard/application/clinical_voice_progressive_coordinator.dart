// ignore_for_file: prefer_initializing_formals

import 'dart:async';

import 'package:flutter/foundation.dart';

import '../data/clinical_voice_ai_api.dart';
import '../data/clinical_voice_capture_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';
import 'clinical_speech_service.dart';

/// Coordinates the non-blocking mobile voice pipeline:
///
/// speech_to_text -> durable intake ACK -> progressive AI preview -> final rebuild.
///
/// The microphone service remains responsible only for capture and local recovery.
/// This coordinator serializes network mutations and never turns an AI timeout into
/// a terminal speech-recognition error while the clinician is still speaking.
final class ClinicalVoiceProgressiveCoordinator {
  ClinicalVoiceProgressiveCoordinator({
    required ValueNotifier<RealtimeSpeechState> speechService,
    required ClinicalVoiceAiGateway aiGateway,
    required ClinicalVoiceCaptureGateway captureGateway,
    required String visitId,
    required Map<String, String> initialDraft,
    required String locale,
  }) : _speechService = speechService,
       _aiGateway = aiGateway,
       _captureGateway = captureGateway,
       _visitId = visitId,
       _initialDraft = Map<String, String>.unmodifiable(initialDraft),
       _locale = locale;

  static const Duration _syncDebounce = Duration(milliseconds: 450);

  final ValueNotifier<RealtimeSpeechState> _speechService;
  final ClinicalVoiceAiGateway _aiGateway;
  final ClinicalVoiceCaptureGateway _captureGateway;
  final String _visitId;
  final Map<String, String> _initialDraft;
  final String _locale;
  final ClinicalDictationParser _parser = const ClinicalDictationParser();
  final Map<String, ClinicalVoiceIntake> _remoteBySegmentId =
      <String, ClinicalVoiceIntake>{};
  final Map<String, String> _syncedTextBySegmentId = <String, String>{};

  Future<void> _serial = Future<void>.value();
  Timer? _syncTimer;
  String _lastObservedFingerprint = '';
  bool _sessionReady = false;
  bool _started = false;
  bool _disposed = false;

  Future<void> start() async {
    if (_started || _disposed) return;
    _started = true;
    _speechService.addListener(_onSpeechChanged);
    _lastObservedFingerprint = _fingerprint(_speechService.value.segments);
    await _enqueue(_restoreServerWorkingSet);
    _scheduleSync();
  }

  Future<bool> synchronizeNow() async {
    _syncTimer?.cancel();
    return _enqueue(_synchronizeCurrentSegments);
  }

  Future<void> finishAndReview() async {
    if (_disposed) return;
    final synchronized = await synchronizeNow();
    if (!synchronized || _disposed || !_speechService.value.hasTranscript) {
      return;
    }

    final current = _speechService.value;
    _speechService.value = current.copyWith(
      status: SpeechStatus.processing,
      stage: ClinicalVoiceStage.capture,
      transcriptSyncStatus: TranscriptSyncStatus.synced,
      clearError: true,
      clearAssistant: true,
    );

    try {
      final state = await _captureGateway.rebuild(
        _visitId,
        _initialDraft,
        locale: _locale,
      );
      _sessionReady = true;
      if (_disposed) return;
      _applyAiState(state, finalReview: true);
    } catch (error) {
      _sessionReady = false;
      if (_disposed) return;
      _speechService.value = _speechService.value.copyWith(
        status: SpeechStatus.transcriptReview,
        stage: ClinicalVoiceStage.capture,
        transcriptSyncStatus: TranscriptSyncStatus.failed,
        errorMessage: clinicalVoiceUserMessage(error, locale: _locale),
      );
    }
  }

  Future<bool> clearAll() async {
    if (_disposed) return false;
    final remoteCleared = await _enqueue(() async {
      await _captureGateway.discardAll(_visitId);
      await _captureGateway.deleteSession(_visitId);
      _sessionReady = false;
      _remoteBySegmentId.clear();
      _syncedTextBySegmentId.clear();
    });
    if (!remoteCleared || _disposed) return false;
    // Dynamic call for both ClinicalSpeechService and CloudSpeechStreamingService
    return await (_speechService as dynamic).clearTranscript();
  }

  /// Seals the assistant result locally but deliberately keeps durable intake rows
  /// recoverable until the consultation itself has been saved successfully.
  Future<bool> completeCapture() async {
    if (_disposed) return false;
    final synchronized = await synchronizeNow();
    if (!synchronized || _disposed) return false;

    try {
      // Dynamic call for both ClinicalSpeechService and CloudSpeechStreamingService
      await (_speechService as dynamic).completeCapture();
    } catch (error) {
      _publishSynchronizationFailure(error);
      return false;
    }

    // The recoverable local draft must already be gone before the result is
    // applied to a form. Session cleanup is secondary and will be retried on the
    // next assistant opening if the backend is temporarily unavailable.
    try {
      await _captureGateway.deleteSession(_visitId);
      _sessionReady = false;
    } catch (_) {
      _sessionReady = false;
    }
    return true;
  }

  void dispose() {
    if (_disposed) return;
    _disposed = true;
    _syncTimer?.cancel();
    if (_started) _speechService.removeListener(_onSpeechChanged);
  }

  void _onSpeechChanged() {
    if (_disposed || _speechService.value.stage != ClinicalVoiceStage.capture) {
      return;
    }
    final fingerprint = _fingerprint(_speechService.value.segments);
    if (fingerprint == _lastObservedFingerprint) return;
    _lastObservedFingerprint = fingerprint;
    _scheduleSync();
  }

  void _scheduleSync() {
    if (_disposed) return;
    _syncTimer?.cancel();
    _syncTimer = Timer(_syncDebounce, () {
      unawaited(_enqueue(_synchronizeCurrentSegments));
    });
  }

  Future<void> _restoreServerWorkingSet() async {
    final active = await _captureGateway.listActive(_visitId);
    if (_disposed) return;

    for (final intake in active) {
      final segmentId = _segmentIdFor(intake);
      _remoteBySegmentId[segmentId] = intake;
      _syncedTextBySegmentId[segmentId] = intake.transcript.trim();
    }

    if (!_speechService.value.hasTranscript && active.isNotEmpty) {
      final segments = active
          .map(
            (intake) => ClinicalTranscriptSegment(
              id: _segmentIdFor(intake),
              offset: Duration(seconds: intake.sequence),
              text: intake.transcript.trim(),
            ),
          )
          .toList(growable: false);
      _speechService.value = _speechService.value.copyWith(
        status: SpeechStatus.transcriptReview,
        stage: ClinicalVoiceStage.capture,
        transcript: clinicalTranscriptFromSegments(segments),
        segments: segments,
        transcriptSyncStatus: TranscriptSyncStatus.synced,
        clearPartial: true,
        clearError: true,
      );
      _lastObservedFingerprint = _fingerprint(segments);
    }

    await _captureGateway.deleteSession(_visitId);
    _sessionReady = false;
    if (active.isEmpty) {
      await _ensureSessionReady();
      return;
    }

    final rebuilt = await _captureGateway.rebuild(
      _visitId,
      _initialDraft,
      locale: _locale,
    );
    _sessionReady = true;
    if (!_disposed) _applyAiState(rebuilt, finalReview: false);
  }

  Future<void> _ensureSessionReady() async {
    if (_sessionReady) return;
    await _captureGateway.deleteSession(_visitId);
    await _aiGateway.startSession(_visitId, _initialDraft, locale: _locale);
    _sessionReady = true;
  }

  Future<void> _synchronizeCurrentSegments() async {
    if (_disposed || _speechService.value.stage != ClinicalVoiceStage.capture) {
      return;
    }
    final segments = List<ClinicalTranscriptSegment>.from(
      _speechService.value.segments,
    );
    final currentById = <String, ClinicalTranscriptSegment>{
      for (final segment in segments) segment.id: segment,
    };

    _publishSynchronizationStart();
    var requiresRebuild = false;

    for (final remoteEntry in _remoteBySegmentId.entries.toList()) {
      if (currentById.containsKey(remoteEntry.key)) continue;
      await _captureGateway.discardSegment(_visitId, remoteEntry.value.id);
      _remoteBySegmentId.remove(remoteEntry.key);
      _syncedTextBySegmentId.remove(remoteEntry.key);
      requiresRebuild = true;
    }

    for (final segment in segments) {
      final text = segment.text.trim();
      if (text.isEmpty) continue;
      final remote = _remoteBySegmentId[segment.id];
      final previousText = _syncedTextBySegmentId[segment.id];

      if (remote == null) {
        final eventId = _eventIdFor(segment.id);
        final intake = await _captureGateway.ingestSegment(
          _visitId,
          eventId: eventId,
          itemId: _itemIdFor(segment.id),
          transcript: text,
        );
        _remoteBySegmentId[segment.id] = intake;
        _syncedTextBySegmentId[segment.id] = text;

        await _ensureSessionReady();
        // Fire-and-forget: ne bloque pas _serial pour ne pas interrompre l'écoute.
        final capturedEventId = eventId;
        final capturedText = text;
        unawaited(
          _captureGateway
              .analyzeProgressiveSegment(
                _visitId,
                eventId: capturedEventId,
                transcript: capturedText,
              )
              .then((progressive) {
                if (!_disposed) _applyAiState(progressive, finalReview: false);
              })
              .catchError((Object error) {
                _publishSynchronizationFailure(error);
              }),
        );
        continue;
      }

      if (previousText != text) {
        final corrected = await _captureGateway.correctSegment(
          _visitId,
          remote.id,
          text,
        );
        _remoteBySegmentId[segment.id] = corrected;
        _syncedTextBySegmentId[segment.id] = text;
        requiresRebuild = true;
      }
    }

    if (requiresRebuild && segments.isNotEmpty) {
      final rebuilt = await _captureGateway.rebuild(
        _visitId,
        _initialDraft,
        locale: _locale,
      );
      _sessionReady = true;
      if (!_disposed) _applyAiState(rebuilt, finalReview: false);
    } else if (segments.isEmpty) {
      _clearAiPreview();
    }

    if (!_disposed) {
      _speechService.value = _speechService.value.copyWith(
        transcriptSyncStatus: TranscriptSyncStatus.synced,
        clearError: true,
      );
    }
  }

  void _applyAiState(ClinicalAiState state, {required bool finalReview}) {
    final current = _speechService.value;
    final explicitVitals = _parser.parse(current.transcript).vitals;
    final aiVitals = state.vitalsFrom(includePending: true);
    final note = state.noteFrom(includePending: true);
    final vitals = explicitVitals.mergePrefer(aiVitals);

    _speechService.value = current.copyWith(
      status: finalReview
          ? state.hasPendingProposals
                ? SpeechStatus.proposalReview
                : SpeechStatus.done
          : current.status,
      stage: finalReview
          ? ClinicalVoiceStage.review
          : ClinicalVoiceStage.capture,
      note: note,
      vitals: vitals,
      revisions: state.revisions,
      assistantMessage: state.assistantMessage,
      needsClarification: finalReview && state.needsClarification,
      transcriptSyncStatus: TranscriptSyncStatus.synced,
      clearError: true,
    );
  }

  void _clearAiPreview() {
    final current = _speechService.value;
    _speechService.value = current.copyWith(
      note: const ConsultationNote(),
      vitals: const PatientVitals(),
      revisions: const <ClinicalAiRevision>[],
      needsClarification: false,
      transcriptSyncStatus: TranscriptSyncStatus.synced,
      clearAssistant: true,
      clearError: true,
    );
  }

  void _publishSynchronizationStart() {
    if (_disposed) return;
    _speechService.value = _speechService.value.copyWith(
      transcriptSyncStatus: TranscriptSyncStatus.syncing,
      clearError: true,
    );
  }

  void _publishSynchronizationFailure(Object error) {
    if (_disposed) return;
    final current = _speechService.value;
    _speechService.value = current.copyWith(
      transcriptSyncStatus: TranscriptSyncStatus.failed,
      errorMessage: current.status == SpeechStatus.listening
          ? null
          : clinicalVoiceUserMessage(error, locale: _locale),
      clearError: current.status == SpeechStatus.listening,
    );
  }

  Future<bool> _enqueue(Future<void> Function() operation) {
    final completer = Completer<bool>();
    _serial = _serial.then((_) async {
      if (_disposed) {
        completer.complete(false);
        return;
      }
      try {
        await operation();
        completer.complete(true);
      } catch (error) {
        _sessionReady = false;
        _publishSynchronizationFailure(error);
        completer.complete(false);
      }
    });
    return completer.future;
  }

  String _segmentIdFor(ClinicalVoiceIntake intake) {
    final itemId = intake.itemId?.trim();
    return itemId == null || itemId.isEmpty ? 'server-${intake.id}' : itemId;
  }

  String _itemIdFor(String segmentId) => _boundedIdentifier(segmentId);

  String _eventIdFor(String segmentId) {
    return _boundedIdentifier('mobile-$segmentId');
  }

  String _boundedIdentifier(String value) {
    final safe = value.replaceAll(RegExp(r'[^A-Za-z0-9._:-]'), '-');
    return safe.length <= 200 ? safe : safe.substring(safe.length - 200);
  }

  String _fingerprint(Iterable<ClinicalTranscriptSegment> segments) {
    return segments.map((item) => '${item.id}:${item.text.trim()}').join('|');
  }
}
