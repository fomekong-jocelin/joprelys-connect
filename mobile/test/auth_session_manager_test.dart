import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/core/security/local_session_data_purger.dart';
import 'package:joprelys_mobile/features/auth/application/auth_controller.dart';
import 'package:joprelys_mobile/features/auth/application/auth_providers.dart';
import 'package:joprelys_mobile/features/auth/application/auth_session_manager.dart';
import 'package:joprelys_mobile/features/auth/application/auth_session_scope.dart';
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

  group('session expiry', () {
    test('a refresh timeout keeps the session and signals nothing', () async {
      final stored = session('expired', now, expiresAt: now);
      final store = MemoryAuthSessionStore()..session = stored;
      final purger = RecordingPurger();
      final gateway = FakeAuthGateway()
        ..refreshError = const ApiException(
          kind: ApiFailureKind.timeout,
          code: 'REQUEST_TIMEOUT',
          message: 'REQUEST_TIMEOUT',
          retryable: true,
        );
      final manager = AuthSessionManager(
        store: store,
        authApi: gateway,
        cookieJar: CookieJar(),
        localDataPurgers: [purger],
        clock: () => now,
      );
      addTearDown(manager.dispose);
      var expirations = 0;
      manager.sessionExpirations.listen((_) => expirations += 1);

      await expectLater(
        manager.refresh(rejectedAccessToken: 'expired'),
        throwsA(isA<ApiException>()),
      );
      await Future<void>.delayed(Duration.zero);

      expect(store.session, same(stored));
      expect(purger.calls, 0);
      expect(expirations, 0);
    });

    test(
      'a refused refresh purges local data and signals expiry once',
      () async {
        final store = MemoryAuthSessionStore()
          ..session = session('expired', now, expiresAt: now);
        final purger = RecordingPurger();
        final gateway = FakeAuthGateway()
          ..refreshError = const ApiException(
            kind: ApiFailureKind.unauthenticated,
            code: 'UNAUTHORIZED',
            message: 'UNAUTHORIZED',
            statusCode: 401,
          );
        final manager = AuthSessionManager(
          store: store,
          authApi: gateway,
          cookieJar: CookieJar(),
          localDataPurgers: [purger],
          clock: () => now,
        );
        addTearDown(manager.dispose);
        var expirations = 0;
        manager.sessionExpirations.listen((_) => expirations += 1);

        await expectLater(
          manager.refresh(rejectedAccessToken: 'expired'),
          throwsA(isA<ApiException>()),
        );
        // The recovery interceptor then expires the (already cleared) session.
        await manager.expire();
        await Future<void>.delayed(Duration.zero);

        expect(store.session, isNull);
        expect(purger.calls, greaterThanOrEqualTo(1));
        expect(expirations, 1);
      },
    );

    test(
      'every purger runs and expiry is signalled even if one fails',
      () async {
        final store = MemoryAuthSessionStore()
          ..session = session('active', now);
        final purger = RecordingPurger();
        final manager = AuthSessionManager(
          store: store,
          authApi: FakeAuthGateway(),
          cookieJar: CookieJar(),
          localDataPurgers: [FailingPurger(), purger],
          clock: () => now,
        );
        addTearDown(manager.dispose);
        var expirations = 0;
        manager.sessionExpirations.listen((_) => expirations += 1);

        await expectLater(manager.expire(), throwsA(isA<StateError>()));
        await Future<void>.delayed(Duration.zero);

        expect(store.session, isNull);
        expect(purger.calls, 1);
        expect(expirations, 1);
      },
    );

    test('logout purges feature data with the session', () async {
      final store = MemoryAuthSessionStore()..session = session('active', now);
      final purger = RecordingPurger();
      final manager = AuthSessionManager(
        store: store,
        authApi: FakeAuthGateway(),
        cookieJar: CookieJar(),
        localDataPurgers: [purger],
        clock: () => now,
      );
      addTearDown(manager.dispose);

      await manager.logout();

      expect(store.session, isNull);
      expect(purger.calls, 1);
    });

    test(
      'AuthController leaves the session and resets its scope on expiry',
      () async {
        final store = MemoryAuthSessionStore()
          ..session = session('active', now);
        final manager = AuthSessionManager(
          store: store,
          authApi: FakeAuthGateway(),
          cookieJar: CookieJar(),
          clock: () => now,
        );
        addTearDown(manager.dispose);
        final container = ProviderContainer(
          overrides: [authSessionManagerProvider.overrideWithValue(manager)],
        );
        addTearDown(container.dispose);
        container.listen(authSessionScopeProvider, (_, _) {});

        final restored = await container.read(authControllerProvider.future);
        expect(restored.status, AuthStatus.authenticated);
        expect(container.read(authSessionScopeProvider), isNotNull);

        await manager.expire();
        await Future<void>.delayed(Duration.zero);

        final state = container.read(authControllerProvider).value;
        expect(state?.status, AuthStatus.unauthenticated);
        expect(state?.errorCode, AuthController.sessionExpiredCode);
        expect(container.read(authSessionScopeProvider), isNull);
      },
    );
  });
}

final class RecordingPurger implements LocalSessionDataPurger {
  int calls = 0;

  @override
  Future<void> purgeLocalSessionData() async {
    calls += 1;
  }
}

final class FailingPurger implements LocalSessionDataPurger {
  @override
  Future<void> purgeLocalSessionData() async {
    throw StateError('purge failed');
  }
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
