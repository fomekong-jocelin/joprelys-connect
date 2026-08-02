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
  return segments
      .map((segment) => segment.text.trim())
      .where((text) => text.isNotEmpty)
      .join('. ')
      .trim();
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
  final String? errorMessage;
  final String? assistantMessage;
  final bool needsClarification;

  bool get hasPendingProposals => revisions.any(
    (revision) => revision.isPending && revision.hasPendingProposals,
  );

  bool get hasApplicableResult => !note.isEmpty || !vitals.isEmpty;

  bool get hasTranscript => transcript.trim().isNotEmpty;

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
      errorMessage: clearError ? null : errorMessage ?? this.errorMessage,
      assistantMessage: clearAssistant
          ? null
          : assistantMessage ?? this.assistantMessage,
      needsClarification: needsClarification ?? this.needsClarification,
    );
  }
}
