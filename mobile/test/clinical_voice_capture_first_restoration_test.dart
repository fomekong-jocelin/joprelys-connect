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
  });

  test('capture screen keeps edit, delete, clear and microphone actions', () {
    final source = File(
      'lib/features/dashboard/presentation/widgets/clinical_voice_assistant_sheet.dart',
    ).readAsStringSync();
    expect(source, contains('ClinicalTranscriptTimeline('));
    expect(source, contains('editable: !listening && !processing'));
    expect(source, contains('onSegmentChanged: onSegmentChanged'));
    expect(source, contains('onSegmentDeleted: onSegmentDeleted'));
    expect(source, contains('onPressed: onClearAll'));
    expect(source, contains('onToggleListening: onToggleListening'));
  });
}
