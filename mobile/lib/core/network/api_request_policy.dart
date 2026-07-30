enum ApiAuthenticationMode { none, optional, required }

enum ApiRetryMode { never, safeMethod, idempotencyKey }

final class ApiRequestPolicy {
  const ApiRequestPolicy({
    required this.authenticationMode,
    required this.retryMode,
    this.recoverProfessionalSession = true,
    this.idempotencyKey,
  }) : assert(
         retryMode != ApiRetryMode.idempotencyKey || idempotencyKey != null,
         'Une clé d’idempotence est requise pour ce mode de retry.',
       );

  const ApiRequestPolicy.publicRequest({
    this.retryMode = ApiRetryMode.never,
    this.idempotencyKey,
  }) : authenticationMode = ApiAuthenticationMode.none,
       recoverProfessionalSession = false,
       assert(
         retryMode != ApiRetryMode.idempotencyKey || idempotencyKey != null,
       );

  const ApiRequestPolicy.protectedRead()
    : authenticationMode = ApiAuthenticationMode.required,
      retryMode = ApiRetryMode.safeMethod,
      recoverProfessionalSession = true,
      idempotencyKey = null;

  const ApiRequestPolicy.protectedWrite({String? idempotencyKey})
    : authenticationMode = ApiAuthenticationMode.required,
      retryMode = idempotencyKey == null
          ? ApiRetryMode.never
          : ApiRetryMode.idempotencyKey,
      recoverProfessionalSession = true,
      idempotencyKey = idempotencyKey;

  final ApiAuthenticationMode authenticationMode;
  final ApiRetryMode retryMode;
  final bool recoverProfessionalSession;
  final String? idempotencyKey;

  bool allowsTransientRetry(String method) {
    return switch (retryMode) {
      ApiRetryMode.never => false,
      ApiRetryMode.safeMethod => _isSafeMethod(method),
      ApiRetryMode.idempotencyKey => idempotencyKey?.trim().isNotEmpty == true,
    };
  }

  static bool _isSafeMethod(String method) {
    return switch (method.toUpperCase()) {
      'GET' || 'HEAD' || 'OPTIONS' => true,
      _ => false,
    };
  }
}

abstract final class ApiRequestExtraKeys {
  static const policy = 'joprelys.api.policy';
  static const authRetryCount = 'joprelys.api.authRetryCount';
  static const transientRetryCount = 'joprelys.api.transientRetryCount';
}
