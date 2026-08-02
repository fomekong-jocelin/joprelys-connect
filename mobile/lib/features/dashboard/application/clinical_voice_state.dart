import 'package:flutter/foundation.dart';

import '../data/clinical_voice_ai_api.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';

enum SpeechStatus {
  idle,
  listening,
  processing,
  transcriptReview,
  proposalReview,
  done,
  error,
}

enum ClinicalVoiceStage { capture, review }

enum TranscriptSyncStatus { idle, syncing, synced, failed }

@immutable
final class ClinicalTranscriptSegment {
  const ClinicalTranscriptSegment({
    required this.id,
    required this.offset,
    required this.text,
  });

  final String id;
  final Duration offset;
  final String text;

  ClinicalTranscriptSegment copyWith({String? text}) {
    return ClinicalTranscriptSegment(
      id: id,
      offset: offset,
      text: text ?? this.text,
    );
  }
}

String clinicalTranscriptFromSegments(
  Iterable<ClinicalTranscriptSegment> segments,
) {
  final texts = segments
      .map((segment) => segment.text.trim())
      .where((text) => text.isNotEmpty);

  return texts.fold<String>('', (result, text) {
    if (result.isEmpty) return text;
    if (RegExp(r'[.!?;:]$').hasMatch(result)) return '$result $text';
    return '$result. $text';
  }).trim();
}

@immutable
final class RealtimeSpeechState {
  const RealtimeSpeechState({
    required this.status,
    required this.stage,
    required this.transcript,
    required this.segments,
    required this.partialTranscript,
    required this.partialOffset,
    required this.vitals,
    required this.note,
    required this.revisions,
    this.soundLevel = 0.0,
    this.transcriptSyncStatus = TranscriptSyncStatus.idle,
    this.errorMessage,
    this.assistantMessage,
    this.needsClarification = false,
  });

  final SpeechStatus status;
  final ClinicalVoiceStage stage;
  final String transcript;
  final List<ClinicalTranscriptSegment> segments;
  final String partialTranscript;
  final Duration partialOffset;
  final PatientVitals vitals;
  final ConsultationNote note;
  final List<ClinicalAiRevision> revisions;
  final double soundLevel;
  final TranscriptSyncStatus transcriptSyncStatus;
  final String? errorMessage;
  final String? assistantMessage;
  final bool needsClarification;

  bool get hasPendingProposals => revisions.any(
    (revision) => revision.isPending && revision.hasPendingProposals,
  );

  bool get hasApplicableResult => !note.isEmpty || !vitals.isEmpty;

  bool get hasTranscript => transcript.trim().isNotEmpty;

  bool get isSynchronizingTranscript =>
      transcriptSyncStatus == TranscriptSyncStatus.syncing;

  bool get hasTranscriptSyncFailure =>
      transcriptSyncStatus == TranscriptSyncStatus.failed;

  bool get isTranscriptReadyForAnalysis =>
      stage == ClinicalVoiceStage.capture &&
      status == SpeechStatus.transcriptReview &&
      hasTranscript &&
      partialTranscript.trim().isEmpty &&
      transcriptSyncStatus == TranscriptSyncStatus.synced &&
      errorMessage == null;

  RealtimeSpeechState copyWith({
    SpeechStatus? status,
    ClinicalVoiceStage? stage,
    String? transcript,
    List<ClinicalTranscriptSegment>? segments,
    String? partialTranscript,
    Duration? partialOffset,
    PatientVitals? vitals,
    ConsultationNote? note,
    List<ClinicalAiRevision>? revisions,
    double? soundLevel,
    TranscriptSyncStatus? transcriptSyncStatus,
    String? errorMessage,
    String? assistantMessage,
    bool? needsClarification,
    bool clearError = false,
    bool clearAssistant = false,
    bool clearPartial = false,
  }) {
    return RealtimeSpeechState(
      status: status ?? this.status,
      stage: stage ?? this.stage,
      transcript: transcript ?? this.transcript,
      segments: segments ?? this.segments,
      partialTranscript: clearPartial
          ? ''
          : partialTranscript ?? this.partialTranscript,
      partialOffset: clearPartial
          ? Duration.zero
          : partialOffset ?? this.partialOffset,
      vitals: vitals ?? this.vitals,
      note: note ?? this.note,
      revisions: revisions ?? this.revisions,
      soundLevel: soundLevel ?? this.soundLevel,
      transcriptSyncStatus: transcriptSyncStatus ?? this.transcriptSyncStatus,
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      assistantMessage: clearAssistant
          ? null
          : assistantMessage ?? this.assistantMessage,
      needsClarification: needsClarification ?? this.needsClarification,
    );
  }
}
