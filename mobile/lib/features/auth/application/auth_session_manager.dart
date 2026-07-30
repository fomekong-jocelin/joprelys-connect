import 'package:cookie_jar/cookie_jar.dart';

import '../../../core/network/api_exception.dart';
import '../../../core/network/api_session.dart';
import '../data/auth_api.dart';
import '../data/auth_session_store.dart';
import '../domain/professional_session.dart';

final class AuthSessionManager implements ApiSessionAccess {
  AuthSessionManager({
    required AuthSessionStore store,
    required AuthGateway authApi,
    required CookieJar cookieJar,
    DateTime Function()? clock,
  }) : _store = store,
       _authApi = authApi,
       _cookieJar = cookieJar,
       _clock = clock ?? DateTime.now;

  final AuthSessionStore _store;
  final AuthGateway _authApi;
  final CookieJar _cookieJar;
  final DateTime Function() _clock;

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

  Future<void> clearLocalSession() async {
    await Future.wait([_store.clear(), _cookieJar.deleteAll()]);
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
        await clearLocalSession();
      }
      rethrow;
    }
  }

  @override
  Future<void> expire() => clearLocalSession();

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
