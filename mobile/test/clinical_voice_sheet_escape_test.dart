import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('the clinical voice sheet always keeps an escape path', () {
    final source = File(
      'lib/features/dashboard/presentation/widgets/'
      'clinical_voice_progressive_assistant_sheet.dart',
    ).readAsStringSync();

    expect(source, contains('isDismissible: true'));
    expect(source, contains('enableDrag: true'));
    expect(source, contains('onPressed: onClose'));
    expect(source, isNot(contains('onPressed: processing ? null : onClose')));
    expect(source, isNot(contains('await _coordinator.synchronizeNow();\n'
        '    if (mounted) Navigator.of(context).pop();')));
  });
}
