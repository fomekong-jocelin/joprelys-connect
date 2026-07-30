enum ApiFailureKind {
  unauthenticated,
  forbidden,
  validation,
  notFound,
  conflict,
  rateLimited,
  server,
  timeout,
  noConnection,
  cancelled,
  malformedResponse,
  unknown,
}

final class ApiException implements Exception {
  const ApiException({
    required this.kind,
    required this.code,
    required this.message,
    this.statusCode,
    this.traceId,
    this.action,
    this.requiredScope,
    this.retryable = false,
  });

  final ApiFailureKind kind;
  final String code;
  final String message;
  final int? statusCode;
  final String? traceId;
  final String? action;
  final String? requiredScope;
  final bool retryable;

  @override
  String toString() {
    return 'ApiException(kind: $kind, code: $code, '
        'statusCode: $statusCode, traceId: $traceId)';
  }
}
