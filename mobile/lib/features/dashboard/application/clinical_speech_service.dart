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
  final double soundLevel; // Amplitude vocale réelle pour l'égaliseur d'ondes !
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

/// Pipeline de captation vocale continue non bloquant (Capture Pipeline).
/// Conforme à la logique Web Angular (RealtimeClinicalTurnCoordinator) :
/// Le flux de dictée ne s'arrête jamais après une phrase et ne requiert aucune
/// validation intermédiaire pour poursuivre l'écoute.
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
  bool _shouldKeepListening = false;

  final List<String> _accumulatedTurns = [];
  String _currentPartial = '';
  Timer? _reconnectLoopTimer;

  Future<void> initialize() async {
    if (_isInitialized) return;
    try {
      _speech = stt.SpeechToText();
      _isInitialized = await _speech!.initialize(
        onStatus: (status) {
          // Relance automatique immédiate du flux d'écoute si l'OS coupe après une pause
          if (status == 'done' || status == 'notListening') {
            if (_shouldKeepListening && value.status == SpeechStatus.listening) {
              _commitCurrentPartial();
              _restartListeningLoop();
            }
          }
        },
        onError: (errorNotification) {
          if (_shouldKeepListening && value.status == SpeechStatus.listening) {
            _restartListeningLoop();
          } else {
            value = value.copyWith(
              status: SpeechStatus.error,
              errorMessage: errorNotification.errorMsg,
            );
          }
        },
      );
    } catch (e) {
      _isInitialized = false;
    }
  }

  void _commitCurrentPartial() {
    final text = _currentPartial.trim();
    if (text.isNotEmpty) {
      if (_accumulatedTurns.isNotEmpty && text.startsWith(_accumulatedTurns.last)) {
        _accumulatedTurns.removeLast();
      }
      if (!_accumulatedTurns.contains(text)) {
        _accumulatedTurns.add(text);
      }
      _currentPartial = '';
    }
  }

  void _restartListeningLoop() {
    _reconnectLoopTimer?.cancel();
    _reconnectLoopTimer = Timer(const Duration(milliseconds: 150), () {
      if (_shouldKeepListening) {
        _listenInternal();
      }
    });
  }

  Future<void> startRealtimeListening() async {
    _shouldKeepListening = true;
    _accumulatedTurns.clear();
    _currentPartial = '';

    value = value.copyWith(
      status: SpeechStatus.listening,
      transcript: '',
      vitals: const PatientVitals(),
      note: const ConsultationNote(),
      soundLevel: 12.0,
    );

    await initialize();
    await _listenInternal();
  }

  Future<void> _listenInternal() async {
    if (!_shouldKeepListening) return;

    if (_isInitialized && _speech != null) {
      try {
        await _speech!.listen(
          onResult: (result) {
            _currentPartial = result.recognizedWords;
            if (result.finalResult) {
              _commitCurrentPartial();
            }
            _updateFullTranscript();
          },
          onSoundLevelChange: (level) {
            // Decibel dynamic sound level mapping
            final normalized = (level + 2.0).clamp(5.0, 60.0);
            value = value.copyWith(soundLevel: normalized);
          },
          listenFor: const Duration(hours: 1),
          pauseFor: const Duration(seconds: 10),
          partialResults: true,
          cancelOnError: false,
          listenMode: stt.ListenMode.dictation,
          localeId: 'fr_FR',
        );
      } catch (_) {
        // Safe fallback retry
        _restartListeningLoop();
      }
    }
  }

  void _updateFullTranscript() {
    final parts = List<String>.from(_accumulatedTurns);
    final cur = _currentPartial.trim();
    if (cur.isNotEmpty) {
      if (parts.isNotEmpty && cur.startsWith(parts.last)) {
        parts.removeLast();
      }
      if (!parts.contains(cur)) {
        parts.add(cur);
      }
    }
    final fullText = parts.join('. ');
    updateTranscript(fullText);
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
    _shouldKeepListening = false;
    _reconnectLoopTimer?.cancel();

    if (_isInitialized && _speech != null && _speech!.isListening) {
      await _speech!.stop();
    }

    _commitCurrentPartial();
    _updateFullTranscript();

    value = value.copyWith(
      status: SpeechStatus.done,
      soundLevel: 0.0,
    );
  }

  Future<void> cancelListening() async {
    _shouldKeepListening = false;
    _reconnectLoopTimer?.cancel();

    if (_isInitialized && _speech != null && _speech!.isListening) {
      await _speech!.cancel();
    }

    _accumulatedTurns.clear();
    _currentPartial = '';

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
    _shouldKeepListening = false;
    _reconnectLoopTimer?.cancel();
    if (_isInitialized && _speech != null) {
      _speech!.cancel();
    }
    super.dispose();
  }
}
