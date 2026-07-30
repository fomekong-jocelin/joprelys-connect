import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:joprelys_mobile/core/network/api_client.dart';
import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/core/network/api_exception_mapper.dart';
import 'package:joprelys_mobile/core/network/api_request_policy.dart';
import 'package:joprelys_mobile/features/auth/data/auth_api.dart';

import 'support/queue_http_client_adapter.dart';

void main() {
  test(
    'login sends the existing backend contract and maps an OTP challenge',
    () async {
      final adapter = QueueHttpClientAdapter((options, callIndex) {
        return jsonResponse(200, {
          'accessToken': '',
          'tokenType': 'Bearer',
          'expiresAt': null,
          'email': 'doctor@example.test',
          'name': '',
          'role': '',
          'requiresOtp': true,
        });
      });
      final api = buildAuthApi(adapter);

      final result = await api.login(
        email: '  doctor@example.test ',
        password: 'password-not-logged',
      );

      expect(result.requiresOtp, isTrue);
      expect(result.email, 'doctor@example.test');
      final request = adapter.requests.single;
      expect(request.path, '/api/auth/login');
      expect(request.data, {
        'email': 'doctor@example.test',
        'password': 'password-not-logged',
      });
      final policy =
          request.extra[ApiRequestExtraKeys.policy] as ApiRequestPolicy;
      expect(policy.authenticationMode, ApiAuthenticationMode.none);
      expect(policy.recoverProfessionalSession, isFalse);
    },
  );

  test(
    'OTP verification maps the complete professional session response',
    () async {
      final adapter = QueueHttpClientAdapter((options, callIndex) {
        return jsonResponse(200, {
          'accessToken': 'access-token',
          'tokenType': 'Bearer',
          'expiresAt': '2026-07-30T12:15:00Z',
          'sessionExpiresAt': '2026-08-06T12:00:00Z',
          'sessionId': 'session-id',
          'email': 'doctor@example.test',
          'name': 'Doctor Test',
          'role': 'DOCTOR',
          'requiresOtp': false,
        });
      });
      final api = buildAuthApi(adapter);

      final result = await api.verifyOtp(
        email: 'doctor@example.test',
        otpCode: '123456',
      );

      final session = result.session!;
      expect(session.accessToken, 'access-token');
      expect(session.expiresAt, DateTime.utc(2026, 7, 30, 12, 15));
      expect(session.sessionExpiresAt, DateTime.utc(2026, 8, 6, 12));
      expect(session.sessionId, 'session-id');
      expect(session.role, 'DOCTOR');
      expect(adapter.requests.single.data, {
        'email': 'doctor@example.test',
        'otpCode': '123456',
      });
    },
  );

  test(
    'refresh rejects a malformed response instead of inventing a session',
    () async {
      final adapter = QueueHttpClientAdapter((options, callIndex) {
        return jsonResponse(200, {
          'accessToken': 'access-token',
          'tokenType': 'Bearer',
          'expiresAt': null,
          'email': 'doctor@example.test',
          'name': 'Doctor Test',
          'role': 'DOCTOR',
        });
      });
      final api = buildAuthApi(adapter);

      await expectLater(
        api.refresh(),
        throwsA(
          isA<ApiException>()
              .having(
                (error) => error.kind,
                'kind',
                ApiFailureKind.malformedResponse,
              )
              .having((error) => error.code, 'code', 'AUTH_EXPIRESAT_MISSING'),
        ),
      );
    },
  );

  test('logout disables automatic refresh and retry', () async {
    final adapter = QueueHttpClientAdapter((options, callIndex) {
      return ResponseBody.fromString('', 204);
    });
    final api = buildAuthApi(adapter);

    await api.logout();

    final request = adapter.requests.single;
    expect(request.path, '/api/auth/logout');
    final policy =
        request.extra[ApiRequestExtraKeys.policy] as ApiRequestPolicy;
    expect(policy.authenticationMode, ApiAuthenticationMode.required);
    expect(policy.retryMode, ApiRetryMode.never);
    expect(policy.recoverProfessionalSession, isFalse);
  });
}

AuthApi buildAuthApi(QueueHttpClientAdapter adapter) {
  final dio = Dio(BaseOptions(baseUrl: 'https://api.test'))
    ..httpClientAdapter = adapter;
  return AuthApi(
    ApiClient(dio: dio, exceptionMapper: const ApiExceptionMapper()),
  );
}

ResponseBody jsonResponse(int statusCode, Map<String, dynamic> payload) {
  return ResponseBody.fromString(
    jsonEncode(payload),
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}
