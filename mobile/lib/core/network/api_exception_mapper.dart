import 'package:dio/dio.dart';

import 'api_exception.dart';
import 'api_headers.dart';

final class ApiExceptionMapper {
  const ApiExceptionMapper();

  ApiException map(DioException exception) {
    final networkFailure = _mapTransportFailure(exception);
    if (networkFailure != null) {
      return networkFailure;
    }

    final response = exception.response;
    final statusCode = response?.statusCode;
    final payload = _asMap(response?.data);
    final nestedError = _asMap(payload?['error']);
    final detail = _string(payload?['detail']);
    final nestedMessage = _string(nestedError?['message']);
    final title = _string(payload?['title']);
    final code = _resolveCode(
      nestedCode: _string(nestedError?['code']),
      detail: detail,
      statusCode: statusCode,
    );
    final traceId =
        _string(nestedError?['trace_id']) ??
        response?.headers.value(ApiHeaders.traceId) ??
        _string(exception.requestOptions.headers[ApiHeaders.traceId]);

    return ApiException(
      kind: _kindForStatus(statusCode),
      code: code,
      message: nestedMessage ?? detail ?? title ?? code,
      statusCode: statusCode,
      traceId: traceId,
      action: _string(nestedError?['action']),
      requiredScope: _string(nestedError?['required_scope']),
      retryable: _isRetryableStatus(statusCode),
    );
  }

  ApiException? _mapTransportFailure(DioException exception) {
    return switch (exception.type) {
      DioExceptionType.cancel => const ApiException(
        kind: ApiFailureKind.cancelled,
        code: 'REQUEST_CANCELLED',
        message: 'REQUEST_CANCELLED',
      ),
      DioExceptionType.connectionTimeout ||
      DioExceptionType.sendTimeout ||
      DioExceptionType.receiveTimeout => ApiException(
        kind: ApiFailureKind.timeout,
        code: 'REQUEST_TIMEOUT',
        message: 'REQUEST_TIMEOUT',
        traceId: _requestTraceId(exception),
        retryable: true,
      ),
      DioExceptionType.connectionError => ApiException(
        kind: ApiFailureKind.noConnection,
        code: 'NETWORK_UNAVAILABLE',
        message: 'NETWORK_UNAVAILABLE',
        traceId: _requestTraceId(exception),
        retryable: true,
      ),
      DioExceptionType.badCertificate => ApiException(
        kind: ApiFailureKind.noConnection,
        code: 'TLS_CERTIFICATE_REJECTED',
        message: 'TLS_CERTIFICATE_REJECTED',
        traceId: _requestTraceId(exception),
      ),
      DioExceptionType.badResponse || DioExceptionType.unknown => null,
    };
  }

  String? _requestTraceId(DioException exception) {
    return _string(exception.requestOptions.headers[ApiHeaders.traceId]);
  }

  String _resolveCode({
    required String? nestedCode,
    required String? detail,
    required int? statusCode,
  }) {
    if (nestedCode != null) {
      return nestedCode;
    }
    if (detail != null && RegExp(r'^[A-Z][A-Z0-9_]{2,}$').hasMatch(detail)) {
      return detail;
    }
    return switch (statusCode) {
      400 => 'BAD_REQUEST',
      401 => 'UNAUTHORIZED',
      403 => 'ACCESS_DENIED',
      404 => 'NOT_FOUND',
      408 => 'REQUEST_TIMEOUT',
      409 => 'CONFLICT',
      422 => 'VALIDATION_ERROR',
      429 => 'RATE_LIMITED',
      500 => 'INTERNAL_ERROR',
      502 => 'BAD_GATEWAY',
      503 => 'SERVICE_UNAVAILABLE',
      504 => 'GATEWAY_TIMEOUT',
      _ => 'API_ERROR',
    };
  }

  ApiFailureKind _kindForStatus(int? statusCode) {
    return switch (statusCode) {
      400 || 422 => ApiFailureKind.validation,
      401 => ApiFailureKind.unauthenticated,
      403 => ApiFailureKind.forbidden,
      404 => ApiFailureKind.notFound,
      408 => ApiFailureKind.timeout,
      409 => ApiFailureKind.conflict,
      429 => ApiFailureKind.rateLimited,
      >= 500 => ApiFailureKind.server,
      _ => ApiFailureKind.unknown,
    };
  }

  bool _isRetryableStatus(int? statusCode) {
    return statusCode == 408 ||
        statusCode == 429 ||
        statusCode == 502 ||
        statusCode == 503 ||
        statusCode == 504;
  }

  Map<String, dynamic>? _asMap(Object? value) {
    if (value is Map<String, dynamic>) {
      return value;
    }
    if (value is Map) {
      return value.map((key, item) => MapEntry(key.toString(), item));
    }
    return null;
  }

  String? _string(Object? value) {
    if (value is! String) {
      return null;
    }
    final trimmed = value.trim();
    return trimmed.isEmpty ? null : trimmed;
  }
}
