import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_speech_service.dart';

void main() {
  group('clinicalVoiceUserMessage', () {
    test('hides technical response parsing details from French users', () {
      final message = clinicalVoiceUserMessage(
        const FormatException('Invalid AI session response'),
        locale: 'fr',
      );

      expect(message, contains('temporairement illisible'));
      expect(message, isNot(contains('FormatException')));
      expect(message, isNot(contains('Invalid AI session response')));
    });

    test('maps connection failures to an actionable English message', () {
      final message = clinicalVoiceUserMessage(
        const ApiException(
          kind: ApiFailureKind.noConnection,
          code: 'NETWORK_UNAVAILABLE',
          message: 'SocketException: Failed host lookup',
        ),
        locale: 'en',
      );

      expect(message, 'No connection. Check your network and try again.');
      expect(message, isNot(contains('SocketException')));
    });

    test('maps microphone permission errors without exposing plugin codes', () {
      final message = clinicalVoiceUserMessage(
        StateError('error_permission'),
        locale: 'fr',
      );

      expect(message, 'Autorisez l’accès au microphone pour démarrer la dictée.');
      expect(message, isNot(contains('error_permission')));
    });

    test('returns a safe fallback for unknown technical errors', () {
      final message = clinicalVoiceUserMessage(
        StateError('unexpected_native_failure_42'),
        locale: 'fr',
      );

      expect(message, 'Impossible de poursuivre la dictée pour le moment. Réessayez.');
      expect(message, isNot(contains('unexpected_native_failure_42')));
    });
  });
}
