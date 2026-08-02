import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('clinical voice provider updates are deferred outside lifecycle methods', () {
    final source = File(
      'lib/features/dashboard/presentation/widgets/clinical_voice_assistant_sheet.dart',
    ).readAsStringSync();

    final initStart = source.indexOf('void initState()');
    final lifecycleStart = source.indexOf('void didChangeAppLifecycleState');
    final disposeStart = source.indexOf('void dispose()');
    final toggleStart = source.indexOf('Future<void> _toggleListening');

    expect(initStart, greaterThanOrEqualTo(0));
    expect(lifecycleStart, greaterThan(initStart));
    expect(disposeStart, greaterThan(lifecycleStart));
    expect(toggleStart, greaterThan(disposeStart));

    final initBlock = source.substring(initStart, lifecycleStart);
    final disposeBlock = source.substring(disposeStart, toggleStart);

    expect(initBlock, contains('addPostFrameCallback'));
    expect(
      initBlock,
      isNot(
        contains(
          '.activate(AppForegroundActivity.clinicalVoiceAssistant);',
        ),
      ),
    );
    expect(disposeBlock, contains('addPostFrameCallback'));
    expect(
      disposeBlock,
      isNot(contains('ref.read(appForegroundActivityProvider.notifier)')),
    );
  });
}
