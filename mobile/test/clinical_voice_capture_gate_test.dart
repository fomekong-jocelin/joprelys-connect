import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_speech_service.dart';
import 'package:joprelys_mobile/features/dashboard/domain/consultation_note.dart';
import 'package:joprelys_mobile/features/dashboard/domain/patient_vitals.dart';

RealtimeSpeechState captureState({
  required SpeechStatus status,
  TranscriptSyncStatus syncStatus = TranscriptSyncStatus.synced,
  String partialTranscript = '',
  String? errorMessage,
}) {
  return RealtimeSpeechState(
    status: status,
    stage: ClinicalVoiceStage.capture,
    transcript: 'Le patient consulte pour une fatigue persistante.',
    segments: const <ClinicalTranscriptSegment>[
      ClinicalTranscriptSegment(
        id: 'segment-1',
        offset: Duration.zero,
        text: 'Le patient consulte pour une fatigue persistante.',
      ),
    ],
    partialTranscript: partialTranscript,
    partialOffset: Duration.zero,
    vitals: const PatientVitals(),
    note: const ConsultationNote(),
    revisions: const <ClinicalAiRevision>[],
    transcriptSyncStatus: syncStatus,
    errorMessage: errorMessage,
  );
}

void main() {
  group('clinical synthesis gate', () {
    test('stays closed while dictation is listening', () {
      final state = captureState(status: SpeechStatus.listening);
      expect(state.isTranscriptReadyForAnalysis, isFalse);
    });

    test('stays closed while a live passage is not explicitly saved', () {
      final state = captureState(
        status: SpeechStatus.transcriptReview,
        partialTranscript: 'et depuis trois semaines',
      );
      expect(state.isTranscriptReadyForAnalysis, isFalse);
    });

    test('stays closed after a local save failure or capture error', () {
      final failedSave = captureState(
        status: SpeechStatus.transcriptReview,
        syncStatus: TranscriptSyncStatus.failed,
      );
      final interrupted = captureState(
        status: SpeechStatus.error,
        errorMessage: 'Dictation interrupted',
      );

      expect(failedSave.isTranscriptReadyForAnalysis, isFalse);
      expect(interrupted.isTranscriptReadyForAnalysis, isFalse);
    });

    test('opens only after explicit successful dictation save', () {
      final state = captureState(status: SpeechStatus.transcriptReview);
      expect(state.isTranscriptReadyForAnalysis, isTrue);
    });
  });

  group('Android speech recovery', () {
    test('restarts transient and common recognizer session failures', () {
      expect(
        shouldRestartClinicalSpeechRecognition(
          'error_temporary',
          permanent: false,
        ),
        isTrue,
      );
      for (final error in <String>[
        'error_busy',
        'error_client',
        'error_audio_error',
        'error_network_timeout',
        'error_server_disconnected',
        'error_speech_timeout',
      ]) {
        expect(
          shouldRestartClinicalSpeechRecognition(error, permanent: true),
          isTrue,
          reason: error,
        );
      }
    });

    test('does not loop on permission, language or throttling failures', () {
      for (final error in <String>[
        'error_permission',
        'error_language_not_supported',
        'error_language_unavailable',
        'error_too_many_requests',
      ]) {
        expect(
          shouldRestartClinicalSpeechRecognition(error, permanent: true),
          isFalse,
          reason: error,
        );
      }
    });
  });
}
