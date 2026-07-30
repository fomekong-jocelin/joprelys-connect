enum ApiSessionKind {
  professional,
  patient,
}

final class ApiSessionSnapshot {
  const ApiSessionSnapshot({
    required this.kind,
    this.accessToken,
    this.accessTokenExpiresAt,
  });

  final ApiSessionKind kind;
  final String? accessToken;
  final DateTime? accessTokenExpiresAt;

  bool get canRefresh => kind == ApiSessionKind.professional;

  bool hasUsableAccessToken(DateTime now) {
    final token = accessToken;
    if (token == null || token.trim().isEmpty) {
      return false;
    }
    final expiresAt = accessTokenExpiresAt;
    return expiresAt == null || expiresAt.isAfter(now);
  }
}

abstract interface class ApiSessionAccess {
  Future<ApiSessionSnapshot?> current();

  Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken});

  Future<void> expire();
}

final class AnonymousApiSessionAccess implements ApiSessionAccess {
  const AnonymousApiSessionAccess();

  @override
  Future<ApiSessionSnapshot?> current() async => null;

  @override
  Future<void> expire() async {}

  @override
  Future<ApiSessionSnapshot?> refresh({String? rejectedAccessToken}) async {
    return null;
  }
}
