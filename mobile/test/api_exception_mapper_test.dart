import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/core/network/api_headers.dart';

void main() {
  const mapper = ApiExceptionMapper();

  test('maps the canonical backend error envelope', () {
    final request = RequestOptions(
      path: '/api/patients',
      headers: {ApiHeaders.traceId: 'trc_mobile_request'},
    );
    final response = Response<dynamic>(
      requestOptions: request,
      statusCode: 403,
      data: {
        'error': {
          'code': 'ACCESS_DENIED',
          'message': 'Action interdite',
          'trace_id': 'trc_backend',
          'action': 'patients.read',
          'required_scope': 'PATIENT_READ',
        },
        'detail': 'Action interdite',
      },
    );

    final mapped = mapper.map(
      DioException.badResponse(
        statusCode: 403,
        requestOptions: request,
        response: response,
      ),
    );

    expect(mapped.kind, ApiFailureKind.forbidden);
    expect(mapped.code, 'ACCESS_DENIED');
    expect(mapped.message, 'Action interdite');
    expect(mapped.traceId, 'trc_backend');
    expect(mapped.action, 'patients.read');
    expect(mapped.requiredScope, 'PATIENT_READ');
    expect(mapped.retryable, isFalse);
  });

  test('maps RFC 7807 auth failures and keeps response trace header', () {
    final request = RequestOptions(path: '/api/auth/refresh');
    final response = Response<dynamic>(
      requestOptions: request,
      statusCode: 401,
      headers: Headers.fromMap({ApiHeaders.traceId: ['trc_response']}),
      data: {
        'title': 'Invalid authentication session',
        'detail': 'AUTH_SESSION_INVALID',
        'status': 401,
      },
    );

    final mapped = mapper.map(
      DioException.badResponse(
        statusCode: 401,
        requestOptions: request,
        response: response,
      ),
    );

    expect(mapped.kind, ApiFailureKind.unauthenticated);
    expect(mapped.code, 'AUTH_SESSION_INVALID');
    expect(mapped.traceId, 'trc_response');
  });

  test('maps timeout and connection failures without exposing payloads', () {
    final request = RequestOptions(
      path: '/api/visits',
      headers: {ApiHeaders.traceId: 'trc_timeout'},
    );

    final timeout = mapper.map(
      DioException(
        requestOptions: request,
        type: DioExceptionType.receiveTimeout,
      ),
    );
    final offline = mapper.map(
      DioException(
        requestOptions: request,
        type: DioExceptionType.connectionError,
      ),
    );

    expect(timeout.kind, ApiFailureKind.timeout);
    expect(timeout.code, 'REQUEST_TIMEOUT');
    expect(timeout.traceId, 'trc_timeout');
    expect(timeout.retryable, isTrue);
    expect(offline.kind, ApiFailureKind.noConnection);
    expect(offline.code, 'NETWORK_UNAVAILABLE');
    expect(offline.retryable, isTrue);
  });
}
