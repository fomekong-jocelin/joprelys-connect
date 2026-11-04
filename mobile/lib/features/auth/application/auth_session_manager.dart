import 'dart:async';

import 'package:cookie_jar/cookie_jar.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/api_session.dart';
import '../../../core/security/local_session_data_purger.dart';
import '../data/auth_api.dart';
import '../data/auth_session_store.dart';
import '../domain/professional_session.dart';

final class AuthSessionManager implements ApiSessionAccess {
  factory AuthSessionManager({
    required AuthSessionStore store,
    required AuthGateway authApi,
    required CookieJar cookieJar,
    Iterable<LocalSessionDataPurger> localDataPurgers =
        const <LocalSessionDataPurger>[],
    DateTime Function()? clock,
  }) {
    return AuthSessionManager._(
      store,
      authApi,
      cookieJar,
      List<LocalSessionDataPurger>.unmodifiable(localDataPurgers),
      clock ?? DateTime.now,
    );
  }

  AuthSessionManager._(
    this._store,
    this._authApi,
    this._cookieJar,
    this._localDataPurgers,
    this._clock,
  );

  final AuthSessionStore _store;
  final AuthGateway _authApi;
  final CookieJar _cookieJar;
  final List<LocalSessionDataPurger> _localDataPurgers;
  final DateTime Function() _clock;
  final StreamController<void> _expirations =
      StreamController<void>.broadcast();

  /// Emits once each time a stored session is invalidated by the server
  /// (refused refresh) or expired by the network layer. Network failures and
  /// timeouts never emit: the session stays available for a later retry.
  Stream<void> get sessionExpirations => _expirations.stream;

  Future<AuthExchangeResult> login({
    required String email,
    required String password,
  }) async {
    await clearLocalSession();
    final result = await _authApi.login(email: email, password: password);
    final session = result.session;
    if (session != null) {
      await _store.write(session);
    }
    return result;
  }

  Future<AuthExchangeResult> verifyOtp({
    required String email,
    required String otpCode,
  }) async {
    final result = await _authApi.verifyOtp(email: email, otpCode: otpCode);
    final session = result.session;
    if (session != null) {
      await _store.write(session);
    }
    return result;
  }

  Future<ProfessionalSession?> restore() async {
    final stored = await _store.read();
    if (stored == null) {
      return null;
    }

    final now = _clock().toUtc();
    final absoluteExpiry = stored.sessionExpiresAt;
    if (absoluteExpiry != null && !absoluteExpiry.isAfter(now)) {
      await clearLocalSession();
      return null;
    }
    if (stored.hasUsableAccessToken(now)) {
      return stored;
    }

    try {
      return await _refreshProfessionalSession(stored);
    } on ApiException catch (error) {
      if (error.kind == ApiFailureKind.unauthenticated ||
          error.kind == ApiFailureKind.forbidden) {
        await clearLocalSession();
        return null;
      }
      throw SessionRecoveryUnavailable(stored, error);
    } catch (error) {
      throw SessionRecoveryUnavailable(stored, error);
    }
  }

  Future<void> logout() async {
    try {
      final stored = await _store.read();
      if (stored != null) {
        await _authApi.logout();
      }
    } catch (_) {
      // Local session and cookies remain authoritative for mobile sign-out.
    } finally {
      await clearLocalSession();
    }
  }

  Future<ProfessionalSession?> setBiometricEnabled(bool enabled) async {
    final stored = await _store.read();
    if (stored == null) {
      return null;
    }
    final updated = stored.copyWith(biometricEnabled: enabled);
    await _store.write(updated);
    return updated;
  }

  /// Clears the session, its cookies and every feature-owned local data
  /// (clinical drafts, downloaded results). All purges run even if one fails;
  /// the first failure is then rethrown.
  Future<void> clearLocalSession() async {
    await Future.wait([
      _store.clear(),
      _cookieJar.deleteAll(),
      for (final purger in _localDataPurgers) purger.purgeLocalSessionData(),
    ]);
  }

  @override
  Future<ApiSessionSnapshot?> current() async {
    return (await _store.read())?.toApiSnapshot();
  }

  @override
  Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken}) async {
    final stored = await _store.read();
    if (stored == null) {
      return null;
    }

    final now = _clock().toUtc();
    if (stored.accessToken != rejectedAccessToken &&
        stored.hasUsableAccessToken(now)) {
      return stored.toApiSnapshot();
    }

    try {
      final refreshed = await _refreshProfessionalSession(stored);
      return refreshed.toApiSnapshot();
    } on ApiException catch (error) {
      if (error.kind == ApiFailureKind.unauthenticated ||
          error.kind == ApiFailureKind.forbidden) {
        await _expireStoredSession();
      }
      rethrow;
    }
  }

  @override
  Future<void> expire() => _expireStoredSession();

  Future<void> dispose() => _expirations.close();

  Future<void> _expireStoredSession() async {
    final hadSession = await _store.read() != null;
    try {
      await clearLocalSession();
    } finally {
      // The UI must leave the expired session even if a local purge failed;
      // the failure itself still propagates to the caller.
      if (hadSession && !_expirations.isClosed) {
        _expirations.add(null);
      }
    }
  }

  Future<ProfessionalSession> _refreshProfessionalSession(
    ProfessionalSession previous,
  ) async {
    final refreshed = await _authApi.refresh();
    final persisted = refreshed.copyWith(
      biometricEnabled: previous.biometricEnabled,
    );
    await _store.write(persisted);
    return persisted;
  }
}

final class StoredApiSessionAccess implements ApiSessionAccess {
  const StoredApiSessionAccess(this._store);

  final AuthSessionStore _store;

  @override
  Future<ApiSessionSnapshot?> current() async {
    return (await _store.read())?.toApiSnapshot();
  }

  @override
  Future<void> expire() => _store.clear();

  @override
  Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken}) async {
    return null;
  }
}

final class SessionRecoveryUnavailable implements Exception {
  const SessionRecoveryUnavailable(this.session, this.cause);

  final ProfessionalSession session;
  final Object cause;

  @override
  String toString() => 'SessionRecoveryUnavailable(cause: $cause)';
}
