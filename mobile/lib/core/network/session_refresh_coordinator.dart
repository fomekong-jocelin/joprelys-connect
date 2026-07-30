import 'api_session.dart';

final class SessionRefreshCoordinator {
  Future<ApiSessionSnapshot?>? _inFlight;

  Future<ApiSessionSnapshot?> refresh(
    ApiSessionAccess sessionAccess, {
    String? rejectedAccessToken,
  }) {
    final current = _inFlight;
    if (current != null) {
      return current;
    }

    final future = sessionAccess.refresh(
      rejectedAccessToken: rejectedAccessToken,
    );
    _inFlight = future;
    return future.whenComplete(() {
      if (identical(_inFlight, future)) {
        _inFlight = null;
      }
    });
  }
}
