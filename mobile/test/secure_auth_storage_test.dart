import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/secure_cookie_storage.dart';
import 'package:joprelys_mobile/features/auth/data/secure_auth_session_store.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';

void main() {
  const secureStorage = FlutterSecureStorage();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues(<String, String>{});
  });

  test('secure cookie storage tracks, reads and deletes all cookie keys', () async {
    final storage = SecureCookieStorage(
      secureStorage,
      namespace: 'test.cookie',
    );
    await storage.init(true, false);

    await storage.write('first-cookie-key', 'first-value');
    await storage.write('second-cookie-key', 'second-value');

    expect(await storage.read('first-cookie-key'), 'first-value');
    expect(await storage.read('second-cookie-key'), 'second-value');

    await storage.deleteAll(const <String>[]);

    expect(await storage.read('first-cookie-key'), isNull);
    expect(await storage.read('second-cookie-key'), isNull);
    expect(await secureStorage.readAll(), isEmpty);
  });

  test('secure session store round-trips only the professional session', () async {
    final store = SecureAuthSessionStore(
      secureStorage,
      storageKey: 'test.professional-session',
    );
    final expected = ProfessionalSession(
      accessToken: 'access-token',
      expiresAt: DateTime.utc(2026, 7, 30, 12, 15),
      sessionExpiresAt: DateTime.utc(2026, 8, 6, 12),
      sessionId: 'session-id',
      email: 'doctor@example.test',
      name: 'Doctor Test',
      role: 'DOCTOR',
      biometricEnabled: true,
    );

    await store.write(expected);
    final restored = await store.read();

    expect(restored?.accessToken, expected.accessToken);
    expect(restored?.expiresAt, expected.expiresAt);
    expect(restored?.sessionExpiresAt, expected.sessionExpiresAt);
    expect(restored?.sessionId, expected.sessionId);
    expect(restored?.email, expected.email);
    expect(restored?.name, expected.name);
    expect(restored?.role, expected.role);
    expect(restored?.biometricEnabled, isTrue);
    expect(restored.toString(), isNot(contains(expected.accessToken)));
  });

  test('malformed secure session is cleared instead of partially restored', () async {
    const key = 'test.professional-session';
    await secureStorage.write(key: key, value: '{"accessToken":"partial"}');
    const store = SecureAuthSessionStore(secureStorage, storageKey: key);

    expect(await store.read(), isNull);
    expect(await secureStorage.read(key: key), isNull);
  });
}
