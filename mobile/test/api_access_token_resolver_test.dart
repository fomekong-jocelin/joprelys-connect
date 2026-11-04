import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/api_access_token_resolver.dart';
import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/core/network/api_session.dart';
import 'package:joprelys_mobile/core/network/session_refresh_coordinator.dart';

void main() {
  final now = DateTime.utc(2026, 11, 4, 9);

  ApiAccessTokenResolver resolverFor(_StoreSessionAccess access) {
    return ApiAccessTokenResolver(
      sessionAccess: access,
      refreshCoordinator: SessionRefreshCoordinator(),
      clock: () => now,
    );
  }

  test('returns the stored token while it stays valid', () async {
    final access = _StoreSessionAccess(
      _snapshot('current', now.add(const Duration(minutes: 10))),
    );

    expect(await resolverFor(access).resolve(), 'current');
    expect(access.refreshCalls, 0);
  });

  test('reads the store on every call (token refreshed elsewhere)', () async {
    final access = _StoreSessionAccess(
      _snapshot('before-refresh', now.add(const Duration(minutes: 10))),
    );
    final resolver = resolverFor(access);

    expect(await resolver.resolve(), 'before-refresh');
    access.snapshot = _snapshot(
      'after-refresh',
      now.add(const Duration(minutes: 15)),
    );
    expect(await resolver.resolve(), 'after-refresh');
  });

  test('refreshes a token that is about to expire', () async {
    final access = _StoreSessionAccess(
      _snapshot('almost-expired', now.add(const Duration(seconds: 5))),
    )..refreshed = _snapshot('fresh', now.add(const Duration(minutes: 15)));

    expect(await resolverFor(access).resolve(), 'fresh');
    expect(access.refreshCalls, 1);
    expect(access.rejectedTokens, ['almost-expired']);
  });

  test('returns null without a session', () async {
    expect(await resolverFor(_StoreSessionAccess(null)).resolve(), isNull);
  });

  test('propagates refresh failures unchanged', () async {
    const failure = ApiException(
      kind: ApiFailureKind.timeout,
      code: 'REQUEST_TIMEOUT',
      message: 'REQUEST_TIMEOUT',
      retryable: true,
    );
    final access = _StoreSessionAccess(_snapshot('expired', now))
      ..refreshError = failure;

    await expectLater(resolverFor(access).resolve(), throwsA(same(failure)));
  });
}

ApiSessionSnapshot _snapshot(String token, DateTime expiresAt) {
  return ApiSessionSnapshot(
    kind: ApiSessionKind.professional,
    accessToken: token,
    accessTokenExpiresAt: expiresAt,
  );
}

final class _StoreSessionAccess implements ApiSessionAccess {
  _StoreSessionAccess(this.snapshot);

  ApiSessionSnapshot? snapshot;
  ApiSessionSnapshot? refreshed;
  Object? refreshError;
  int refreshCalls = 0;
  final List<String?> rejectedTokens = <String?>[];

  @override
  Future<ApiSessionSnapshot?> current() async => snapshot;

  @override
  Future<void> expire() async => snapshot = null;

  @override
  Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken}) async {
    refreshCalls += 1;
    rejectedTokens.add(rejectedAccessToken);
    final error = refreshError;
    if (error != null) throw error;
    snapshot = refreshed;
    return refreshed;
  }
}
