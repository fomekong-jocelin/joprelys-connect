import 'package:flutter/foundation.dart';

final RegExp _sensitiveQueryParameter = RegExp(
  r"""\b((?:access_?)?token|ticket)=[^&\s#"']+""",
  caseSensitive: false,
);
final RegExp _bearerCredential = RegExp(
  r'\bBearer\s+[A-Za-z0-9._~+/=-]+',
  caseSensitive: false,
);

/// Removes credentials (query tokens, bearer headers) from a diagnostic text.
String redactSensitiveText(String input) {
  return input
      .replaceAllMapped(
        _sensitiveQueryParameter,
        (match) => '${match.group(1)}=<redacted>',
      )
      .replaceAll(_bearerCredential, 'Bearer <redacted>');
}

/// Describes an error for developer logs without leaking secrets or clinical
/// content.
String describeErrorForLog(Object error) {
  // FormatException.toString() embeds the parsed source, which may contain
  // dictated clinical text: only its type is safe to log.
  if (error is FormatException) return 'FormatException';
  return redactSensitiveText(error.toString());
}

/// Debug-only diagnostic log. Never emitted in profile or release builds, and
/// always redacted: callers must not pass URIs, tokens or patient data in
/// [event].
void safeDebugLog(String event, {Object? error}) {
  if (!kDebugMode) return;
  debugPrint(error == null ? event : '$event (${describeErrorForLog(error)})');
}
