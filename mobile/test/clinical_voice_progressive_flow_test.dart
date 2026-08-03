import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('durable intake is acknowledged before progressive AI analysis', () {
    final source = File(
      'lib/features/dashboard/application/clinical_voice_progressive_coordinator.dart',
    ).readAsStringSync();

    final ingest = source.indexOf('await _captureGateway.ingestSegment(');
    final analyze = source.indexOf(
      'await _captureGateway.analyzeProgressiveSegment(',
      ingest,
    );

    expect(ingest, greaterThanOrEqualTo(0));
    expect(analyze, greaterThan(ingest));
    expect(source, contains('Future<void> _serial = Future<void>.value()'));
  });

  test('AI synchronization never turns active listening into terminal error', () {
    final source = File(
      'lib/features/dashboard/application/clinical_voice_progressive_coordinator.dart',
    ).readAsStringSync();
    final start = source.indexOf('void _publishSynchronizationFailure');
    final end = source.indexOf('Future<bool> _enqueue', start);
    final failureHandling = source.substring(start, end);

    expect(
      failureHandling,
      contains('errorMessage: current.status == SpeechStatus.listening'),
    );
    expect(failureHandling, contains('? null'));
    expect(
      failureHandling,
      contains('clearError: current.status == SpeechStatus.listening'),
    );
  });

  test('progressive assistant starts only after local restoration', () {
    final orchestration = File(
      'lib/features/dashboard/presentation/widgets/clinical_voice_progressive_assistant_sheet.dart',
    ).readAsStringSync();
    final sections = File(
      'lib/features/dashboard/presentation/widgets/clinical_voice_progressive_assistant_sections.dart',
    ).readAsStringSync();

    final initialize = orchestration.indexOf('await _speechService.initialize()');
    final coordinator = orchestration.indexOf('await _coordinator.start()');
    expect(initialize, greaterThanOrEqualTo(0));
    expect(coordinator, greaterThan(initialize));
    expect(sections, contains('ClinicalVoiceProgressivePreview(state: state)'));
    expect(orchestration.split('\n').length, lessThan(500));
    expect(sections.split('\n').length, lessThan(500));
  });

  test(
    'consultation entry point uses the progressive assistant with clean SOAP',
    () {
      final source = File(
        'lib/features/dashboard/presentation/widgets/consultation_notes_sheet.dart',
      ).readAsStringSync();
      final start = source.indexOf('void _launchAssistant()');
      final end = source.indexOf('Future<void> _retryLoad()', start);
      final launch = source.substring(start, end);

      expect(launch, contains('ClinicalVoiceProgressiveAssistantSheet.show('));
      expect(launch, contains('initialDraft: const <String, String>{}'));
      expect(launch, contains('applyAcceptedDraft(result.note)'));
    },
  );

  test('vitals entry point uses the same progressive capture pipeline', () {
    final source = File(
      'lib/features/dashboard/presentation/widgets/patient_vitals_sheet.dart',
    ).readAsStringSync();
    final start = source.indexOf('void _launchAssistant()');
    final end = source.indexOf('int _applyAcceptedVitals', start);
    final launch = source.substring(start, end);

    expect(launch, contains('ClinicalVoiceProgressiveAssistantSheet.show('));
    expect(launch, contains("'vitals': jsonEncode(current.toJson())"));
    expect(launch, contains('_applyAcceptedVitals(result.vitals)'));
  });

  test(
    'voice working set is consumed only after consultation save response',
    () {
      final source = File(
        'lib/features/dashboard/data/consultation_api.dart',
      ).readAsStringSync();

      final saveResponse = source.indexOf(
        'final saved = ConsultationNote.fromJson',
      );
      final consume = source.indexOf('realtime-intake/consume', saveResponse);
      expect(saveResponse, greaterThanOrEqualTo(0));
      expect(consume, greaterThan(saveResponse));
    },
  );

  test('voice working set is consumed only after vitals save response', () {
    final source = File(
      'lib/features/dashboard/data/vitals_api.dart',
    ).readAsStringSync();

    final saveResponse = source.indexOf('final saved = PatientVitals.fromJson');
    final consume = source.indexOf('realtime-intake/consume', saveResponse);
    expect(saveResponse, greaterThanOrEqualTo(0));
    expect(consume, greaterThan(saveResponse));
  });

  test(
    'final review rebuilds from durable transcript instead of stale session',
    () {
      final source = File(
        'lib/features/dashboard/application/clinical_voice_progressive_coordinator.dart',
      ).readAsStringSync();
      final start = source.indexOf('Future<void> finishAndReview()');
      final end = source.indexOf('Future<bool> clearAll()', start);
      final finalization = source.substring(start, end);

      expect(finalization, contains('await synchronizeNow()'));
      expect(finalization, contains('await _captureGateway.rebuild('));
      expect(finalization, isNot(contains('_aiGateway.getSession')));
    },
  );
}
