import 'api_session.dart';
import 'session_refresh_coordinator.dart';

/// Resolves the access token to use *now* for transports that cannot go
/// through the Dio recovery interceptor (WebSocket handshakes).
///
/// The token is read from the session store on every call, and refreshed
/// through the shared [SessionRefreshCoordinator] when it is missing, expired
/// or about to expire. Refresh failures are propagated unchanged.
final class ApiAccessTokenResolver {
  factory ApiAccessTokenResolver({
    required ApiSessionAccess sessionAccess,
    required SessionRefreshCoordinator refreshCoordinator,
    DateTime Function()? clock,
    Duration minimumValidity = const Duration(seconds: 30),
  }) {
    return ApiAccessTokenResolver._(
      sessionAccess,
      refreshCoordinator,
      clock ?? DateTime.now,
      minimumValidity,
    );
  }

  ApiAccessTokenResolver._(
    this._sessionAccess,
    this._refreshCoordinator,
    this._clock,
    this._minimumValidity,
  );

  final ApiSessionAccess _sessionAccess;
  final SessionRefreshCoordinator _refreshCoordinator;
  final DateTime Function() _clock;
  final Duration _minimumValidity;

  /// Returns a usable access token, or `null` when no session can provide one.
  Future<String?> resolve() async {
    final session = await _sessionAccess.current();
    if (session == null) return null;

    final now = _clock().toUtc();
    if (session.hasUsableAccessToken(now.add(_minimumValidity))) {
      return session.accessToken;
    }
    if (!session.canRefresh) {
      return session.hasUsableAccessToken(now) ? session.accessToken : null;
    }

    final refreshed = await _refreshCoordinator.refresh(
      _sessionAccess,
      rejectedAccessToken: session.accessToken,
    );
    if (refreshed == null || !refreshed.hasUsableAccessToken(_clock())) {
      return null;
    }
    return refreshed.accessToken;
  }
}
