import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;

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
    this.soundLevel = 0.0,
    this.errorMessage,
  });

  final SpeechStatus status;
  final String transcript;
  final PatientVitals vitals;
  final ConsultationNote note;
  final double soundLevel; // Niveau sonore en décibels pour faire vibrer les ondes !
  final String? errorMessage;

  RealtimeSpeechState copyWith({
    SpeechStatus? status,
    String? transcript,
    PatientVitals? vitals,
    ConsultationNote? note,
    double? soundLevel,
    String? errorMessage,
  }) {
    return RealtimeSpeechState(
      status: status ?? this.status,
      transcript: transcript ?? this.transcript,
      vitals: vitals ?? this.vitals,
      note: note ?? this.note,
      soundLevel: soundLevel ?? this.soundLevel,
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
  stt.SpeechToText? _speech;
  bool _isInitialized = false;

  Future<void> initialize() async {
    if (_isInitialized) return;
    try {
      _speech = stt.SpeechToText();
      _isInitialized = await _speech!.initialize(
        onStatus: (status) {
          if (status == 'done' || status == 'notListening') {
            if (value.status == SpeechStatus.listening) {
              value = value.copyWith(status: SpeechStatus.done);
            }
          }
        },
        onError: (errorNotification) {
          value = value.copyWith(
            status: SpeechStatus.error,
            errorMessage: errorNotification.errorMsg,
          );
        },
      );
    } catch (e) {
      _isInitialized = false;
    }
  }

  Future<void> startRealtimeListening() async {
    await initialize();

    value = value.copyWith(
      status: SpeechStatus.listening,
      transcript: '',
      vitals: const PatientVitals(),
      note: const ConsultationNote(),
      soundLevel: 10.0,
    );

    if (_isInitialized && _speech != null) {
      await _speech!.listen(
        onResult: (result) {
          final words = result.recognizedWords;
          updateTranscript(words);
        },
        onSoundLevelChange: (level) {
          // Transmission du niveau sonore réel au peintre d'ondes !
          final normalized = (level + 2.0).clamp(0.0, 50.0);
          value = value.copyWith(soundLevel: normalized);
        },
        cancelOnError: true,
        partialResults: true,
        localeId: 'fr_FR',
      );
    }
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

  Future<void> stopListening() async {
    if (_isInitialized && _speech != null && _speech!.isListening) {
      await _speech!.stop();
    }
    value = value.copyWith(
      status: SpeechStatus.done,
      soundLevel: 0.0,
    );
  }

  Future<void> cancelListening() async {
    if (_isInitialized && _speech != null && _speech!.isListening) {
      await _speech!.cancel();
    }
    value = const RealtimeSpeechState(
      status: SpeechStatus.idle,
      transcript: '',
      vitals: PatientVitals(),
      note: ConsultationNote(),
      soundLevel: 0.0,
    );
  }

  @override
  void dispose() {
    if (_isInitialized && _speech != null) {
      _speech!.cancel();
    }
    super.dispose();
  }
}
