import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/features/auth/application/auth_session_manager.dart';
import 'package:joprelys_mobile/features/auth/data/auth_api.dart';
import 'package:joprelys_mobile/features/auth/data/auth_session_store.dart';
import 'package:joprelys_mobile/features/auth/domain/professional_session.dart';

void main() {
  final now = DateTime.utc(2026, 7, 30, 12);
  final cookieUri = Uri.parse('https://api.test/api/auth/refresh');

  test(
    'login clears stale state then stores an authenticated session',
    () async {
      final store = MemoryAuthSessionStore()..session = session('stale', now);
      final cookies = CookieJar();
      await cookies.saveFromResponse(cookieUri, [
        Cookie('refresh_token', 'stale'),
      ]);
      final gateway = FakeAuthGateway()
        ..loginResult = AuthExchangeResult.authenticated(session('fresh', now));
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: cookies,
        clock: () => now,
      );

      final result = await manager.login(
        email: 'doctor@example.test',
        password: 'not-logged',
      );

      expect(result.session?.accessToken, 'fresh');
      expect(store.session?.accessToken, 'fresh');
      expect(await cookies.loadForRequest(cookieUri), isEmpty);
      expect(gateway.loginCalls, 1);
    },
  );

  test('OTP challenge never persists an incomplete session', () async {
    final store = MemoryAuthSessionStore();
    final gateway = FakeAuthGateway()
      ..loginResult = AuthExchangeResult.otpRequired('doctor@example.test');
    final manager = AuthSessionManager(
      store: store,
      authApi: gateway,
      cookieJar: CookieJar(),
      clock: () => now,
    );

    final result = await manager.login(
      email: 'doctor@example.test',
      password: 'not-logged',
    );

    expect(result.requiresOtp, isTrue);
    expect(store.session, isNull);
  });

  test('restore returns a usable session without a refresh call', () async {
    final stored = session('usable', now);
    final store = MemoryAuthSessionStore()..session = stored;
    final gateway = FakeAuthGateway();
    final manager = AuthSessionManager(
      store: store,
      authApi: gateway,
      cookieJar: CookieJar(),
      clock: () => now,
    );

    final restored = await manager.restore();

    expect(restored, same(stored));
    expect(gateway.refreshCalls, 0);
  });

  test(
    'restore refreshes an expired access token and preserves biometric lock',
    () async {
      final store = MemoryAuthSessionStore()
        ..session = session(
          'expired',
          now,
          expiresAt: now.subtract(const Duration(minutes: 1)),
          biometricEnabled: true,
        );
      final gateway = FakeAuthGateway()
        ..refreshResult = session('fresh', now, biometricEnabled: false);
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: CookieJar(),
        clock: () => now,
      );

      final restored = await manager.restore();

      expect(restored?.accessToken, 'fresh');
      expect(restored?.biometricEnabled, isTrue);
      expect(store.session?.biometricEnabled, isTrue);
      expect(gateway.refreshCalls, 1);
    },
  );

  test(
    'authentication refusal clears the session and refresh cookie',
    () async {
      final store = MemoryAuthSessionStore()
        ..session = session(
          'expired',
          now,
          expiresAt: now.subtract(const Duration(minutes: 1)),
        );
      final cookies = CookieJar();
      await cookies.saveFromResponse(cookieUri, [
        Cookie('refresh_token', 'secret'),
      ]);
      final gateway = FakeAuthGateway()
        ..refreshError = const ApiException(
          kind: ApiFailureKind.unauthenticated,
          code: 'UNAUTHORIZED',
          message: 'UNAUTHORIZED',
        );
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: cookies,
        clock: () => now,
      );

      expect(await manager.restore(), isNull);
      expect(store.session, isNull);
      expect(await cookies.loadForRequest(cookieUri), isEmpty);
    },
  );

  test(
    'network failure preserves local state and exposes recovery state',
    () async {
      final stored = session(
        'expired',
        now,
        expiresAt: now.subtract(const Duration(minutes: 1)),
      );
      final store = MemoryAuthSessionStore()..session = stored;
      final gateway = FakeAuthGateway()
        ..refreshError = const ApiException(
          kind: ApiFailureKind.noConnection,
          code: 'NETWORK_UNAVAILABLE',
          message: 'NETWORK_UNAVAILABLE',
          retryable: true,
        );
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: CookieJar(),
        clock: () => now,
      );

      await expectLater(
        manager.restore(),
        throwsA(
          isA<SessionRecoveryUnavailable>().having(
            (error) => error.session,
            'session',
            same(stored),
          ),
        ),
      );
      expect(store.session, same(stored));
    },
  );

  test(
    'logout clears local session and cookies even when API logout fails',
    () async {
      final store = MemoryAuthSessionStore()..session = session('active', now);
      final cookies = CookieJar();
      await cookies.saveFromResponse(cookieUri, [
        Cookie('refresh_token', 'secret'),
      ]);
      final gateway = FakeAuthGateway()..logoutError = StateError('offline');
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: cookies,
        clock: () => now,
      );

      await manager.logout();
      expect(store.session, isNull);
      expect(await cookies.loadForRequest(cookieUri), isEmpty);
    },
  );

  test(
    'concurrent recovery reuses a newer usable token without refresh',
    () async {
      final store = MemoryAuthSessionStore()
        ..session = session('new-token', now);
      final gateway = FakeAuthGateway();
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: CookieJar(),
        clock: () => now,
      );

      final snapshot = await manager.refresh(rejectedAccessToken: 'old-token');

      expect(snapshot?.accessToken, 'new-token');
      expect(gateway.refreshCalls, 0);
    },
  );
}

ProfessionalSession session(
  String accessToken,
  DateTime now, {
  DateTime? expiresAt,
  bool biometricEnabled = false,
}) {
  return ProfessionalSession(
    accessToken: accessToken,
    expiresAt: expiresAt ?? now.add(const Duration(minutes: 15)),
    sessionExpiresAt: now.add(const Duration(days: 7)),
    sessionId: 'session-id',
    email: 'doctor@example.test',
    name: 'Doctor Test',
    role: 'DOCTOR',
    biometricEnabled: biometricEnabled,
  );
}

final class MemoryAuthSessionStore implements AuthSessionStore {
  ProfessionalSession? session;

  @override
  Future<void> clear() async {
    session = null;
  }

  @override
  Future<ProfessionalSession?> read() async => session;

  @override
  Future<void> write(ProfessionalSession session) async {
    this.session = session;
  }
}

final class FakeAuthGateway implements AuthGateway {
  AuthExchangeResult? loginResult;
  AuthExchangeResult? otpResult;
  ProfessionalSession? refreshResult;
  Object? refreshError;
  Object? logoutError;
  int loginCalls = 0;
  int refreshCalls = 0;

  @override
  Future<AuthExchangeResult> login({
    required String email,
    required String password,
  }) async {
    loginCalls += 1;
    return loginResult ?? AuthExchangeResult.otpRequired(email);
  }

  @override
  Future<void> logout() async {
    final error = logoutError;
    if (error != null) {
      throw error;
    }
  }

  @override
  Future<ProfessionalSession> refresh() async {
    refreshCalls += 1;
    final error = refreshError;
    if (error != null) {
      throw error;
    }
    return refreshResult!;
  }

  @override
  Future<AuthExchangeResult> verifyOtp({
    required String email,
    required String otpCode,
  }) async {
    return otpResult ?? AuthExchangeResult.otpRequired(email);
  }
}
