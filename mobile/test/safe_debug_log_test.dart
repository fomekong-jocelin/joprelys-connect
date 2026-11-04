import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/security/safe_debug_log.dart';

void main() {
  test('redacts query tokens from a WebSocket failure message', () {
    const message =
        "WebSocketException: Connection to 'https://api.test/api/voice/stream"
        "?token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.sig#' was not upgraded";

    final redacted = redactSensitiveText(message);

    expect(redacted, isNot(contains('eyJ')));
    expect(redacted, contains('token=<redacted>'));
    expect(redacted, contains('/api/voice/stream'));
  });

  test('redacts access_token, ticket and bearer credentials', () {
    final redacted = redactSensitiveText(
      'a?access_token=abc&ticket=def&x=1 Authorization: Bearer ghi.jkl',
    );

    expect(redacted, isNot(contains('abc')));
    expect(redacted, isNot(contains('def')));
    expect(redacted, isNot(contains('ghi.jkl')));
    expect(redacted, contains('x=1'));
  });

  test('never logs the source of a FormatException', () {
    const error = FormatException('Unexpected character', 'Patient Fixture');

    expect(describeErrorForLog(error), 'FormatException');
  });
}
