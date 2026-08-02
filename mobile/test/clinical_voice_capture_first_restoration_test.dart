import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('an existing AI session reopens on editable dictation', () {
    final source = File(
      'lib/features/dashboard/application/clinical_speech_service.dart',
    ).readAsStringSync();
    final start = source.indexOf('void _restoreExistingSession');
    final end = source.indexOf('void _replaceSegmentsWithTranscript', start);
    expect(start, greaterThanOrEqualTo(0));
    expect(end, greaterThan(start));

    final restoration = source.substring(start, end);
    expect(restoration, isNot(contains('_applyAiState(existing)')));
    expect(restoration, contains('SpeechStatus.transcriptReview'));
    expect(restoration, contains('ClinicalVoiceStage.capture'));
    expect(restoration, contains("transcriptStatus.toUpperCase() == 'NONE'"));
    expect(restoration, contains("transcript: ''"));
  });

  test('capture screen keeps edit, delete, clear and microphone actions', () {
    final source = File(
      'lib/features/dashboard/presentation/widgets/clinical_voice_assistant_sheet.dart',
    ).readAsStringSync();
    expect(source, contains('ClinicalTranscriptTimeline('));
    expect(source, contains('!state.isSynchronizingTranscript'));
    expect(source, contains('onSegmentChanged: onSegmentChanged'));
    expect(source, contains('onSegmentDeleted: onSegmentDeleted'));
    expect(source, contains('onPressed: disabled ? null : onClearAll'));
    expect(source, contains('onToggleListening: onToggleListening'));
    expect(source, contains('class _CaptureActionBar'));
    expect(source, contains('state.hasTranscriptSyncFailure'));
    expect(source, contains('onRetrySave'));
  });

  test('capture never sends one network request per recognized passage', () {
    final source = File(
      'lib/features/dashboard/application/clinical_speech_service.dart',
    ).readAsStringSync();

    expect(source, isNot(contains('_gateway.savePendingTranscript')));
    expect(source, contains('ClinicalTranscriptDraftGateway'));
    expect(source, contains('_draftStore.write'));
    expect(source, contains('_draftDebounce'));
    expect(source, contains('return _ensureTranscriptPersisted();'));
  });

  test(
    'one live passage survives recognizer restarts before being committed',
    () {
      final source = File(
        'lib/features/dashboard/application/clinical_speech_service.dart',
      ).readAsStringSync();

      expect(source, isNot(contains('_stabilityTimer')));
      expect(source, isNot(contains('_stableHypothesisDelay')));
      expect(source, contains('_currentPartialFinalized = true'));
      expect(source, contains('currentFinalized: _currentPartialFinalized'));
      expect(source, contains('pauseFor: const Duration(seconds: 4)'));
      expect(source, contains("status == 'done' || status == 'notListening'"));
    },
  );
}
