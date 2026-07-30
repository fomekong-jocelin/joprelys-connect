import 'dart:async';
import 'package:flutter/foundation.dart';

import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';
import 'clinical_dictation_parser.dart';

enum SpeechStatus { idle, listening, processing, done, error }

@immutable
final class RealtimeSpeechState {
  const RealtimeSpeechState({
    required this.status,
    required this.transcript,
    required this.vitals,
    required this.note,
    this.errorMessage,
  });

  final SpeechStatus status;
  final String transcript;
  final PatientVitals vitals;
  final ConsultationNote note;
  final String? errorMessage;

  RealtimeSpeechState copyWith({
    SpeechStatus? status,
    String? transcript,
    PatientVitals? vitals,
    ConsultationNote? note,
    String? errorMessage,
  }) {
    return RealtimeSpeechState(
      status: status ?? this.status,
      transcript: transcript ?? this.transcript,
      vitals: vitals ?? this.vitals,
      note: note ?? this.note,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

class ClinicalSpeechService extends ValueNotifier<RealtimeSpeechState> {
  ClinicalSpeechService()
      : super(const RealtimeSpeechState(
          status: SpeechStatus.idle,
          transcript: '',
          vitals: PatientVitals(),
          note: ConsultationNote(),
        ));

  final _parser = const ClinicalDictationParser();
  Timer? _simulatedVoiceTimer;

  void startRealtimeListening() {
    _simulatedVoiceTimer?.cancel();
    value = value.copyWith(
      status: SpeechStatus.listening,
      transcript: '',
      vitals: const PatientVitals(),
      note: const ConsultationNote(),
    );
  }

  void updateTranscript(String text) {
    final parsed = _parser.parse(text);
    value = value.copyWith(
      transcript: text,
      vitals: parsed.vitals,
      note: parsed.note,
      status: SpeechStatus.listening,
    );
  }

  void stopListening() {
    _simulatedVoiceTimer?.cancel();
    value = value.copyWith(status: SpeechStatus.done);
  }

  void cancelListening() {
    _simulatedVoiceTimer?.cancel();
    value = const RealtimeSpeechState(
      status: SpeechStatus.idle,
      transcript: '',
      vitals: PatientVitals(),
      note: ConsultationNote(),
    );
  }

  @override
  void dispose() {
    _simulatedVoiceTimer?.cancel();
    super.dispose();
  }
}
